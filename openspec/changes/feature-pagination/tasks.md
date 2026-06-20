# Tasks: 8-Item Pagination and Admin Search

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~350 lines |
| 400-line budget risk | Medium |
| Chained PRs recommended | No |
| Suggested split | Single PR |
| Delivery strategy | auto-forecast |
| Chain strategy | pending |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: pending
400-line budget risk: Medium

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Extract `PageResult` to common | PR 1 | Safe refactor without behavior change |
| 2 | Funcionario pagination | PR 1 | Adds pagination to `GET /funcionarios` |
| 3 | Election pagination & search | PR 1 | Adds pagination + search to `GET /elections` |

## Phase 1: Shared Domain Extraction

- [x] 1.1 Move `co.com.votapp.ws.electoral.domain.model.PageResult` to `co.com.votapp.ws.common.domain.model.PageResult`.
- [x] 1.2 Update imports in all classes and tests that referenced the electoral `PageResult` (e.g., `Censo*` files).
- [x] 1.3 Run `mvn test` and `mvn verify -Dgroups="integration"` to ensure existing census behavior is unchanged.

## Phase 2: Auth Context (Funcionarios) Pagination

- [x] 2.1 Update `FuncionarioRepositoryPort.java` `findAll` to `PageResult<Funcionario> findAll(int page, int size, String search)`.
- [x] 2.2 Update `FuncionarioJpaRepository.java` to change its `findAll` query to `Page<FuncionarioEntity> search(@Param("search") String search, Pageable pageable)`.
- [x] 2.3 Update `FuncionarioRepositoryAdapter.java` to implement the new port signature, map Spring `Page` to `PageResult`, and update `FuncionarioRepositoryAdapterTest` / `FuncionarioRepositoryAdapterIT`.
- [x] 2.4 Update `FuncionarioController.java` `list` endpoint to accept `page` (default 0), `size` (default 8), and `search`. Clamp size (1..100) and page (floor 0).
- [x] 2.5 Update `FuncionarioController.java` to return `PageResult<FuncionarioResponse>` instead of `List`.
- [x] 2.6 Update `FuncionarioControllerTest.java` to verify the new pagination and default parameters.

## Phase 3: Electoral Context (Elections) Pagination & Search

- [x] 3.1 Update `ElectionRepositoryPort.java` adding `PageResult<Election> findAll(int page, int size, String search)`.
- [x] 3.2 Add a `@Query` method `search(@Param("search") String search, Pageable pageable)` to `EleccionJpaRepository.java` filtering by `codigo` or `nombre`.
- [x] 3.3 Update `ElectionRepositoryAdapter.java` to implement the port method, using the search query with `createdAt DESC` ordering, and mapping the result to `PageResult`.
- [x] 3.4 Update `ElectionRepositoryAdapterTest` and `ElectionRepositoryAdapterIT` to verify pagination, search filters, and sorting.
- [x] 3.5 Update `ElectionController.java` `listElections` endpoint to accept clamped `page`, `size`, and `search` parameters, returning `PageResult<ElectionResponse>`.
- [x] 3.6 Update `ElectionControllerTest.java` to verify pagination, defaults, and the search argument pass-through.
