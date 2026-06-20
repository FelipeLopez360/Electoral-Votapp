# Proposal: 8-Item Pagination and Admin Search

## Intent
Prevent infinite scrolling in admin lists by introducing paginated endpoints (defaulting to 8 items per page) and adding a search bar for elections by name/code to match the existing funcionario search.

## Scope

### In Scope
- Extract the existing pure-Java pagination abstraction (`PageResult`) into a shared domain package to avoid cross-context cyclic dependencies.
- Update `GET /api/v1/elections` to support `page`, `size` (defaulting to 8 items), and `search` (matching codigo or nombre).
- Update `GET /api/v1/funcionarios` to support `page` and `size` parameters.
- Update corresponding use cases, domain ports, and JPA repositories (including `EleccionJpaRepository` and `FuncionarioJpaRepository`).

### Out of Scope
- Frontend SPA modifications. These must be performed and deployed via a companion PR in the separate frontend repository, as this workspace is backend-only.
- Pagination for voter-facing endpoints outside the admin panel.

## Capabilities

### New Capabilities
- None

### Modified Capabilities
- `admin-elections-read`: Convert raw list response to paginated response and introduce search capabilities.
- `admin-funcionarios-read`: Convert raw list response to paginated response.

## Approach

1. **Shared Pagination**: Move `PageResult` from the electoral context to a common domain package (e.g., `co.com.votapp.ws.shared.domain.model.PageResult`) to enable reuse across bounded contexts.
2. **Backend API Changes**:
    - Modify `ElectionController` and `FuncionarioController` to return `PageResult<T>` instead of `List<T>`.
    - Update input ports to receive `page`, `size`, and `search` arguments.
3. **Persistence Layer**:
    - Update `EleccionJpaRepository` to use a pageable query that searches `codigo` or `nombre` with `createdAt DESC` ordering.
    - Update `FuncionarioJpaRepository` to pass `Pageable` into its existing search query.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `co.com.votapp.ws.electoral.domain.model.PageResult` | Moved | Extract to shared package for reuse. |
| `co.com.votapp.ws.electoral.infrastructure.adapter.in.web.ElectionController` | Modified | Breaking API shape; adds pagination/search parameters. |
| `co.com.votapp.ws.electoral.infrastructure.adapter.out.persistence.EleccionJpaRepository` | Modified | Adds pageable query for name/code. |
| `co.com.votapp.ws.auth.infrastructure.adapter.in.web.FuncionarioController` | Modified | Breaking API shape; adds pagination parameters. |
| `co.com.votapp.ws.auth.infrastructure.adapter.out.persistence.FuncionarioJpaRepository` | Modified | Upgrades search to return Spring `Page`. |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| API/Frontend Breakage | High | Coordinate backend release with companion frontend PR. The API response structure changes from a raw array to a page object. |
| Census Flow Breakage | Low | Rely on existing `CensoControllerTest` suite to catch import extraction issues during `PageResult` refactor. |

## Rollback Plan
Revert the backend repository commit. Requires atomic coordinated revert with the frontend repository if deployed simultaneously.

## Dependencies
- **Frontend Repository**: Companion PR required to consume paginated `content` structure, control the 8-item display logic, and render the election search bar.

## Success Criteria
- [ ] `GET /api/v1/elections` returns paginated results and filters by `nombre` or `codigo`.
- [ ] `GET /api/v1/funcionarios` returns paginated results and applies search filters correctly.
- [ ] Both contexts use a single shared pure-Java pagination abstraction (`PageResult`).
- [ ] Existing census pagination tests continue to pass.