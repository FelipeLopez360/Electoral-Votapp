# Tasks: Portal Settings & Password

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~600 backend, ~250 frontend |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 (Backend Domain), PR 2 (Backend Web/Adapters), PR 3 (Frontend) |
| Delivery strategy | ask-on-risk |
| Chain strategy | stacked-to-main |

Decision needed before apply: Yes
Chained PRs recommended: Yes
Chain strategy: stacked-to-main
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Backend Domain & Ports | PR 1 (Backend) | Base: main. Use cases, exceptions, ports, and unit tests. |
| 2 | Backend Infrastructure | PR 2 (Backend) | Base: PR 1 branch. Controllers, adapters, and integration tests. |
| 3 | Frontend Integration | PR 1 (Frontend)| Separate repo. Settings page, auth context, API client methods. |

## Phase 1: Backend Domain & Core (Spring Boot Repo)

- [x] 1.1 Update `LoginResponse` to add `debeCambiarPassword`. Modify `co.com.votapp.ws.auth.domain.port.in.AuthenticateFuncionarioUseCase` output model (and its implementation) to return `debeCambiarPassword` alongside the token.
- [x] 1.2 Create `co.com.votapp.ws.auth.domain.exception.WeakPasswordException` (extends `DomainException`).
- [x] 1.3 Modify `co.com.votapp.ws.auth.domain.port.out.FuncionarioRepositoryPort` to add `void updatePasswordHash(String documentoIdentidad, String newHash)`.
- [x] 1.4 Create `co.com.votapp.ws.auth.domain.port.in.ChangePasswordUseCase`, `UpdateProfileUseCase`, and `LogoutUseCase`.
- [x] 1.5 Test: Create `ChangePasswordUseCaseImplTest` (JUnit5/Mockito) verifying weak password rejection, bad current password, and success flow.
- [x] 1.6 Implement `co.com.votapp.ws.auth.domain.usecase.ChangePasswordUseCaseImpl` checking current password, validating strength (min 8 chars, 1 upper, 1 lower, 1 num), encoding, updating hash, and setting `debeCambiarPassword=false`.
- [x] 1.7 Test: Create `UpdateProfileUseCaseImplTest`.
- [x] 1.8 Implement `co.com.votapp.ws.auth.domain.usecase.UpdateProfileUseCaseImpl` to update only `email` and `telefono`, preserving other fields.
- [x] 1.9 Test: Create `LogoutUseCaseImplTest`.
- [x] 1.10 Implement `co.com.votapp.ws.auth.domain.usecase.LogoutUseCaseImpl` to call `PortalSessionPort.removeSession(token)`.
- [x] 1.11 Modify `co.com.votapp.ws.config.DomainConfig` to register the 3 new use-case beans.

## Phase 2: Backend Infrastructure & Adapters (Spring Boot Repo)

- [x] 2.1 Modify `co.com.votapp.ws.auth.infrastructure.adapter.out.persistence.FuncionarioJpaRepository` adding `@Modifying @Query("UPDATE FuncionarioEntity f SET f.passwordHash = :hash, f.debeCambiarPassword = false WHERE f.documentoIdentidad = :doc")`.
- [x] 2.2 Modify `co.com.votapp.ws.auth.infrastructure.adapter.out.persistence.FuncionarioRepositoryAdapter` to implement `updatePasswordHash`.
- [x] 2.3 Test: Update `FuncionarioRepositoryAdapterIT` (Testcontainers) to verify `updatePasswordHash` persists the hash and clears the flag without touching other fields.
- [x] 2.4 Modify `co.com.votapp.ws.auth.infrastructure.adapter.in.web.portal.PortalAuthController` to populate `debeCambiarPassword` in the response DTO.
- [x] 2.5 Test: Create `PortalSettingsControllerWebMvcTest` mocking the new use cases and the `PortalAuthFilter`.
- [x] 2.6 Create `co.com.votapp.ws.auth.infrastructure.adapter.in.web.portal.PortalSettingsController` with inline records for its DTOs (`PasswordChangeRequest`, `ProfileUpdateRequest`, `ProfileResponse`).
- [x] 2.7 Implement `PUT /api/v1/portal/password` mapped to `ChangePasswordUseCase`. Resolve `documentoIdentidad` via `FuncionarioRepositoryPort.findById`.
- [x] 2.8 Implement `GET /api/v1/portal/me` and `PUT /api/v1/portal/me` mapped to `UpdateProfileUseCase`.
- [x] 2.9 Implement `POST /api/v1/portal/logout` mapped to `LogoutUseCase` (read raw token from Authorization header).

## Phase 3: Frontend Integration (React Repo)

- [ ] 3.1 Update `src/api/portalClient.ts` adding methods for `changePassword`, `getProfile`, `updateProfile`, and `logout`.
- [ ] 3.2 Update `src/contexts/PortalAuthContext.tsx` to read and store `debeCambiarPassword` on login and implement `logout` triggering the API.
- [ ] 3.3 Modify `src/pages/portal/LoginPage.tsx` to redirect to `/portal/configuracion?firstLogin=true` if `debeCambiarPassword` is true.
- [ ] 3.4 Create `src/pages/portal/SettingsPage.tsx` with UI sections for "Cambiar contraseña" and "Datos personales".
- [ ] 3.5 Implement Password Change form enforcing client-side validation (matching passwords). Redirect to dashboard on success if `firstLogin=true`.
- [ ] 3.6 Implement Profile Update form allowing edits to email and phone, rendering other personal data as read-only.
- [ ] 3.7 Modify `src/layouts/PortalLayout.tsx` to add "Configuración" link to the navigation menu.
- [ ] 3.8 Modify `src/App.tsx` router to include the new `/portal/configuracion` route.
