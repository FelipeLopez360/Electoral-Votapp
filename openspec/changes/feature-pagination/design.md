# Design: 8-Item Pagination and Admin Search

## Technical Approach

Add paginated reads to the two admin list endpoints (`GET /api/v1/elections`, `GET /api/v1/funcionarios`) and election name/code search, reusing the existing pure-Java `PageResult<T>` abstraction already proven by the census flow. Both list endpoints currently call output ports directly from the controller (no use case in the read path), so the change is a thin vertical slice per context: Controller → RepositoryPort → RepositoryAdapter → JpaRepository. `PageResult` is promoted to a shared package so both contexts depend on one type without a cross-context cycle.

## Architecture Decisions

| Decision | Choice | Alternatives rejected | Rationale |
|----------|--------|-----------------------|-----------|
| Pagination type | Reuse `PageResult<T>` (move to shared pkg) | Spring `Page` in ports; new DTO per context | `Page` leaks Spring into domain (AGENTS.md law); `PageResult` already exists and is battle-tested by census. |
| Shared package name | `co.com.votapp.ws.common.domain.model.PageResult` | `shared.domain.model` (proposal text) | `co.com.votapp.ws.common` already exists (`NotFoundException`); avoids inventing a parallel `shared` root. |
| Read path location | Keep reads in Controller→Port (no use case) | Introduce list use cases | Existing convention: reads carry no business rule and call ports directly. Adding use cases would be unjustified scope. |
| Default/clamp policy | `size` default 8, clamp 1..100; `page` default 0, floor 0 | Trust client values | Prevents `size=0`/negative `PageRequest` exceptions and unbounded reads. |
| Election search | New `@Query` with `LOWER(...) LIKE` on `codigo`/`nombre`, null/blank = match all | Spring `Specification`; derived method | Mirrors the existing `FuncionarioJpaRepository.search` JPQL pattern for consistency. |
| Ordering | `createdAt DESC` for elections; existing order for funcionarios | Sort param from client | Matches current `findAllByOrderByCreatedAtDesc`; no UI requirement for custom sort. |

## Data Flow

    GET /elections?page&size&search        GET /funcionarios?page&size&search
              │                                        │
        ElectionController                      FuncionarioController
              │ (clamp page/size)                      │ (clamp page/size)
        ElectionRepositoryPort.findAll(           FuncionarioRepositoryPort.findAll(
          page,size,search) : PageResult            page,size,search) : PageResult
              │                                        │
        ElectionRepositoryAdapter               FuncionarioRepositoryAdapter
          PageRequest → JpaRepo.search()          PageRequest → JpaRepo.search()
          Page<Entity> → PageResult.map(toDomain) Page<Entity> → PageResult.map(toDomain)
              │                                        │
              └──── PageResult<...Response> via .map(Response::from) ────┘

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `common/domain/model/PageResult.java` | Create (move) | Relocate from `electoral.domain.model`; identical record + `map`/`empty`. |
| `electoral/domain/model/PageResult.java` | Delete | Replaced by shared type. |
| census files (8) referencing `electoral...PageResult` | Modify | Update import to `common.domain.model.PageResult`. No logic change. |
| `electoral/domain/port/out/ElectionRepositoryPort.java` | Modify | Add `PageResult<Election> findAll(int page, int size, String search)`. |
| `electoral/.../persistence/EleccionJpaRepository.java` | Modify | Add `Page<EleccionEntity> search(String search, Pageable)` JPQL. |
| `electoral/.../persistence/ElectionRepositoryAdapter.java` | Modify | Implement paginated `findAll`; map `Page`→`PageResult`. |
| `electoral/.../web/ElectionController.java` | Modify | `listElections` returns `PageResult<ElectionResponse>`; add `page`/`size`/`search` params + clamp. |
| `auth/domain/port/out/FuncionarioRepositoryPort.java` | Modify | Add `PageResult<Funcionario> findAll(int page, int size, String search)`. |
| `auth/.../persistence/FuncionarioJpaRepository.java` | Modify | Overload `search(String, Pageable)` returning `Page`. |
| `auth/.../persistence/FuncionarioRepositoryAdapter.java` | Modify | Implement paginated `findAll`; map `Page`→`PageResult`. |
| `auth/.../web/FuncionarioController.java` | Modify | `list` returns `PageResult<FuncionarioResponse>`; add `page`/`size` params + clamp. |

Existing `List<Funcionario> findAll(String)` is called ONLY by `FuncionarioController` (verified: no other production caller). Replace its signature with the paginated one and update `FuncionarioControllerTest`/`FuncionarioRepositoryAdapterIT` accordingly.

## Interfaces / Contracts

Port additions (pure Java — no Spring import):

```java
PageResult<Election> findAll(int page, int size, String search);     // ElectionRepositoryPort
PageResult<Funcionario> findAll(int page, int size, String search);  // FuncionarioRepositoryPort
```

JSON response shape (both endpoints), unchanged from census `PageResult`:

```json
{ "content": [ /* items */ ], "page": 0, "size": 8, "totalElements": 42, "totalPages": 6 }
```

Election search JPQL (mirrors funcionario pattern):

```java
@Query("""
    SELECT e FROM EleccionEntity e
    WHERE :search IS NULL OR :search = ''
       OR LOWER(e.codigo) LIKE LOWER(CONCAT('%', :search, '%'))
       OR LOWER(e.nombre) LIKE LOWER(CONCAT('%', :search, '%'))
    """)
Page<EleccionEntity> search(@Param("search") String search, Pageable pageable);
```

## Testing Strategy

Strict TDD (RED → GREEN → TRIANGULATE → REFACTOR). No production code before a failing test.

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit (`*Test`) | Adapter maps `Page`→`PageResult`; controller clamps `size`/`page` and defaults to 8 | JUnit + Mockito, mock JpaRepository / port. Mirror `CensoRepositoryAdapterTest`. |
| Unit (`*Test`) | Election search query selects correct field; blank search returns all | Mockito on adapter; verify `PageRequest` + search arg passed. |
| Integration (`*IT`) | Real pageable + search over Postgres for both repos | Testcontainers `@SpringBootTest`. Mirror `CensoRepositoryAdapterIT`. |
| Regression | Census pagination untouched after `PageResult` move | Run existing `Censo*` suites green. |

## Migration / Rollout

No DB migration. Breaking API shape (array → page object) on both endpoints. Backend must deploy in lockstep with the companion frontend PR that consumes `content` and renders the election search bar. Rollback = revert the backend commit, coordinated with frontend revert if both deployed.

## Open Questions

- [ ] Confirm shared package `common.domain.model` vs proposal's `shared.domain.model` with the team (design recommends `common`, reusing the existing root). Resolve before sdd-apply since it dictates the import path for all moved files.
