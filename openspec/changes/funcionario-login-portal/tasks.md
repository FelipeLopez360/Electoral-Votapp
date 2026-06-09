# Tasks: Funcionario Login Portal

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 600 - 800 lines |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 (Domain) → PR 2 (Adapters) → PR 3 (Web) → PR 4 (Frontend) |
| Delivery strategy | single-pr-default / ask-always |
| Chain strategy | stacked-to-main |

Decision needed before apply: Yes
Chained PRs recommended: Yes
Chain strategy: stacked-to-main
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Schema, Ports, Domain Use Cases | PR 1 | Base branch: main; Test domain logic |
| 2 | Adapters & Config | PR 2 | Base branch: PR 1; Test JPA/Redis integrations |
| 3 | Web Controllers & Security | PR 3 | Base branch: PR 2; Integration tests for endpoints |
| 4 | Frontend Portal SPA | PR 4 | Base branch: main (Frontend repo); E2E tests |

## Phase 1: Foundation & Domain (Backend PR 1)

- [x] 1.1 Update Seed Data
  - Desc: Update default passwords to use valid BCrypt hashes.
  - Files: `src/main/resources/db/migration/V1__Initial_schema.sql`
  - Deps: None
  - Effort: S
  - AC: Seed data loads without errors. Backend. ✅ DONE

- [x] 1.2 Update Ports
  - Desc: Add matches() to PasswordEncoderPort, lockout/hash lookups to FuncionarioRepositoryPort, findIssued to VotingTokenRepositoryPort.
  - Files: `auth/domain/port/out/*`, `voting/domain/port/out/*`
  - Deps: 1.1
  - Effort: S
  - AC: Ports defined without Spring dependencies. Backend. ✅ DONE

- [x] 1.3 Create Domain Exceptions
  - Desc: Add InvalidCredentials, AccountLocked, AccountInactive exceptions.
  - Files: `auth/domain/exception/*`
  - Deps: None
  - Effort: S
  - AC: Exceptions extend domain base exceptions. Backend. ✅ DONE

- [x] 1.4 Update AuthenticateFuncionarioUseCase
  - Desc: Implement BCrypt verification, active state check, and lockout counters.
  - Files: `auth/domain/usecase/AuthenticateFuncionarioUseCase.java`
  - Deps: 1.2, 1.3
  - Effort: M
  - AC: Fails securely on bad password or locked account. Backend. ✅ DONE

- [x] 1.5 Create CastVoteByTokenIdUseCase
  - Desc: Create input port and impl reusing atomic flow by `tokenId`.
  - Files: `voting/domain/port/in/CastVoteByTokenIdPort.java`, `voting/domain/usecase/CastVoteByTokenIdUseCaseImpl.java`
  - Deps: 1.2
  - Effort: M
  - AC: Executes vote by ID without rawToken. Backend. ✅ DONE

## Phase 2: Adapters & Config (Backend PR 2)

- [ ] 2.1 Implement BCrypt & JPA Adapters
  - Desc: Implement `matches` in BCrypt adapter and new queries/updates in JPA adapters.
  - Files: `auth/infrastructure/adapter/out/security/*`, `.../persistence/*`
  - Deps: 1.2, 1.4
  - Effort: M
  - AC: JPA tests pass for lockout logic. Backend.

- [ ] 2.2 Implement Redis Session Adapter
  - Desc: Implement PortalSessionPort storing opaque UUIDs mapped to Funcionario ID.
  - Files: `auth/infrastructure/adapter/out/redis/RedisPortalSessionAdapter.java`
  - Deps: 1.2
  - Effort: M
  - AC: Issues and resolves sessions correctly. Backend.

- [ ] 2.3 Transactional App Service
  - Desc: Wrap vote use cases with Spring `@Transactional` to keep domain pure.
  - Files: `voting/application/service/CastVoteAppService.java`
  - Deps: 1.5
  - Effort: S
  - AC: Rollbacks occur on failure. Backend.

- [ ] 2.4 DomainConfig Wiring
  - Desc: Wire new adapters and updated use cases.
  - Files: `config/DomainConfig.java`
  - Deps: 2.1, 2.2, 2.3
  - Effort: S
  - AC: Context loads cleanly. Backend.

## Phase 3: Web & Security (Backend PR 3) — ✅ COMPLETE

- [x] 3.1 PortalAuthFilter & SecurityConfig
  - Desc: Create token filter and dual SecurityFilterChain (Basic vs Portal).
  - Files: `common/config/SecurityConfig.java`, `auth/.../in/web/PortalAuthFilter.java`
  - Deps: 2.2
  - Effort: L
  - AC: `/api/v1/portal/**` protected by session token. Backend. ✅ DONE

- [x] 3.2 Portal Controllers
  - Desc: Create PortalAuthController (login) and PortalVotingController (dashboard, vote).
  - Files: `auth/.../web/portal/PortalAuthController.java`, `voting/.../web/portal/PortalVotingController.java`
  - Deps: 3.1
  - Effort: M
  - AC: Endpoints adhere to data contracts. Backend. ✅ DONE

- [x] 3.3 Fix ElectionController
  - Desc: Fix ignored ballot path parameter mapping.
  - Files: `electoral/.../in/web/ElectionController.java`
  - Deps: None
  - Effort: S
  - AC: Parameter is read correctly. Backend. ✅ DONE

## Phase 4: Frontend Portal (Frontend PR 4)

- [ ] 4.1 Auth Context & API
  - Desc: Setup PortalAuthContext (bearer token).
  - Files: `src/context/PortalAuthContext.tsx`, `src/api/portalClient.ts`
  - Deps: 3.2
  - Effort: M
  - AC: Token sent in Authorization header. Frontend.

- [ ] 4.2 Login Page
  - Desc: Implement login form with error handling.
  - Files: `src/pages/portal/LoginPage.tsx`
  - Deps: 4.1
  - Effort: M
  - AC: Displays appropriate lockout/credential errors. Frontend.

- [ ] 4.3 Dashboard & Vote Pages
  - Desc: Show assigned elections and cast vote view.
  - Files: `src/pages/portal/DashboardPage.tsx`, `src/pages/portal/VotePage.tsx`
  - Deps: 4.1
  - Effort: L
  - AC: Never shows raw token; updates `hasVoted` state. Frontend.
