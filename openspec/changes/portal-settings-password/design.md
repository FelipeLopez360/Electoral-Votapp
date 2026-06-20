# Design: Portal Settings & Password

## Technical Approach

Extend the existing `auth` bounded context to let an authenticated Funcionario change
their password, edit profile (email/telefono), and explicitly invalidate their session.
Login is augmented to surface `debeCambiarPassword`. Backend follows the **exact**
portal conventions already in the codebase: thin controllers with **inline `record` DTOs**,
domain use cases wired manually in `DomainConfig`, `documentoIdentidad` as the use-case key,
and `funcionarioId` resolved from the `PortalAuthFilter` request attribute. No app-service
layer is added for these flows — they are single-step (matching `PortalAuthController`,
not the multi-step `CastVoteAppService`).

> **Repo note:** No `frontend/` exists in this repository. Frontend changes are
> documented for the separate SPA repo and are **out of scope for this backend change's
> verification**. This design ships and tests the Spring Boot side only.

## Architecture Decisions

| # | Decision | Choice | Rejected alt. | Rationale |
|---|----------|--------|---------------|-----------|
| 1 | DTO location | Inline `record`s inside each controller | Separate `application/dto/*.java` | Matches existing `PortalAuthController`/`PortalVotingController` convention. Prompt's separate-DTO suggestion contradicts the actual codebase. |
| 2 | App-service layer | None — controllers call domain ports directly | `PasswordChangeAppService`/`ProfileAppService` | Single-step flows. `CastVoteAppService` exists only for multi-step atomicity; these aren't atomic-multi-step. Adding `@Transactional` services here would be ceremony. |
| 3 | Use-case key | `documentoIdentidad` (String) | `funcionarioId` (Integer) | Reuses all existing `FuncionarioRepositoryPort` methods keyed by documento. Controller resolves `funcionarioId → documentoIdentidad` once via `findById`. |
| 4 | Endpoint placement | `GET/PUT /me`, `PUT /password`, `POST /logout` added to a new `PortalSettingsController` | Overload `PortalAuthController` | Keeps login controller single-responsibility; all routes still under `/api/v1/portal/**` so the Order(1) Bearer chain covers them with zero `SecurityConfig` change. |
| 5 | Password persistence | New `updatePasswordHash(doc, hash)` port + `@Modifying` JPQL | Load entity, mutate, `save()` | `save()` re-maps the whole domain object and never touches hash by design. A targeted `UPDATE ... SET password_hash` mirrors the existing lockout-method pattern and avoids round-tripping the credential. |
| 6 | New-password strength | Validated **inside** `ChangePasswordUseCase` (domain) | Bean Validation on DTO | Strength is a business rule; keeps it testable without Spring and consistent across callers. Throws `WeakPasswordException extends DomainException`. |
| 7 | Logout key | Session token (String) | funcionarioId | `PortalSessionPort.removeSession(token)` already exists and is token-scoped (current-session-only, per proposal out-of-scope for global logout). Controller reads the raw Bearer token. |
| 8 | confirmNewPassword | Validated at controller level — simple equality check | Domain use case | `confirmNewPassword` is a client-side typo prevention, not business logic. Controller returns 400 before delegating to the use case if `newPassword != confirmNewPassword`. |
| 9 | Same-password rejection | Validated inside `ChangePasswordUseCase` — throws `WeakPasswordException` | Controller-level | Using the same password is a policy violation, not a client mistake. Handled as domain logic alongside the other strength rules. Maps to 409 via existing `DomainException` handler. |

## Data Flow

**Login (with debeCambiarPassword):**
```
LoginPage → POST /portal/login → PortalAuthController
   → authenticatePort.authenticate(doc, pass) → Funcionario
   → sessionPort.createSession(id) → token
   ← LoginResponse(token, id, nombre, debeCambiarPassword)   ← NEW field
```

**Change password:**
```
PUT /portal/password (Bearer) → PortalAuthFilter sets funcionarioId attr
   → PortalSettingsController.resolveDocumento(funcionarioId) via repo.findById
   → changePasswordPort.change(doc, current, new)
        ├─ findPasswordHashByDocumentoIdentidad → encoder.matches(current, hash)? else InvalidCredentials
        ├─ validate strength(new) else WeakPassword
        ├─ encoder.encode(new) → repo.updatePasswordHash(doc, newHash)
        └─ repo.save(funcionario with debeCambiarPassword=false)   ← clears first-login flag
   ← 204
```

**Update profile:**
```
PUT /portal/me (Bearer) → resolve funcionario
   → updateProfilePort.update(doc, email, telefono)
        → load Funcionario → rebuild with new email/telefono only → repo.save()
   ← 200 ProfileResponse
```

**Logout:**
```
POST /portal/logout (Bearer) → controller reads raw token from Authorization header
   → logoutPort.logout(token) → sessionPort.removeSession(token)
   ← 204
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `auth/domain/port/in/ChangePasswordUseCase.java` | Create | `void change(String doc, String currentPassword, String newPassword)` |
| `auth/domain/port/in/UpdateProfileUseCase.java` | Create | `Funcionario update(String doc, String email, String telefono)` |
| `auth/domain/port/in/LogoutUseCase.java` | Create | `void logout(String sessionToken)` |
| `auth/domain/usecase/ChangePasswordUseCaseImpl.java` | Create | Verify current, validate strength, encode, persist, clear flag |
| `auth/domain/usecase/UpdateProfileUseCaseImpl.java` | Create | Partial update of email/telefono via existing `save()` |
| `auth/domain/usecase/LogoutUseCaseImpl.java` | Create | Delegates to `PortalSessionPort.removeSession` |
| `auth/domain/exception/WeakPasswordException.java` | Create | `extends DomainException` (→409) |
| `auth/domain/port/out/FuncionarioRepositoryPort.java` | Modify | Add `void updatePasswordHash(String doc, String newHash)` |
| `auth/infrastructure/.../persistence/FuncionarioJpaRepository.java` | Modify | Add `@Modifying @Query("UPDATE ... SET passwordHash ...")` |
| `auth/infrastructure/.../persistence/FuncionarioRepositoryAdapter.java` | Modify | Implement `updatePasswordHash` |
| `auth/infrastructure/.../web/portal/PortalSettingsController.java` | Create | `GET/PUT /me`, `PUT /password`, `POST /logout` + inline DTOs |
| `auth/infrastructure/.../web/portal/PortalAuthController.java` | Modify | Add `debeCambiarPassword` to `LoginResponse` |
| `config/DomainConfig.java` | Modify | Wire 3 new use-case beans |

## Interfaces / Contracts

```java
// domain/port/in
public interface ChangePasswordUseCase {
    void change(String documentoIdentidad, String currentPassword, String newPassword);
}
public interface UpdateProfileUseCase {
    Funcionario update(String documentoIdentidad, String email, String telefono);
}
public interface LogoutUseCase { void logout(String sessionToken); }

// domain/port/out — added to FuncionarioRepositoryPort
void updatePasswordHash(String documentoIdentidad, String newHash);
```

REST contracts (camelCase, inline records):
- `PUT  /api/v1/portal/password` `{currentPassword,newPassword}` → `204` / `401` (wrong current) / `409` (weak)
- `GET  /api/v1/portal/me` → `{nombres,apellidos,documentoIdentidad,email,telefono,numeroEmpleado}`
- `PUT  /api/v1/portal/me` `{email,telefono}` → `200 ProfileResponse`
- `POST /api/v1/portal/logout` → `204`
- `LoginResponse` gains `boolean debeCambiarPassword`

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit | `ChangePasswordUseCaseImpl`: wrong current → `InvalidCredentialsException`; weak new → `WeakPasswordException`; happy path encodes + `updatePasswordHash` + clears `debeCambiarPassword` | JUnit5 + Mockito, mock `FuncionarioRepositoryPort` + `PasswordEncoderPort` |
| Unit | `UpdateProfileUseCaseImpl`: only email/telefono change, other fields preserved; not-found → `NotFoundException` | Mockito |
| Unit | `LogoutUseCaseImpl`: calls `removeSession(token)` exactly once | Mockito verify |
| WebMvc | `PortalSettingsController` happy + error status mapping; `LoginResponse` includes flag | `@WebMvcTest` slice, mock ports (mirror `PortalAuthControllerWebMvcTest`) |
| IT | `FuncionarioRepositoryAdapterIT`: `updatePasswordHash` persists new hash, leaves other columns intact | Testcontainers PostgreSQL |

## Migration / Rollout

No migration required — `password_hash` and `debe_cambiar_password` columns already exist
(V1 schema). Rollout = code only; revert by reverting the PR. Redis sessions unaffected.

## Open Questions

- [ ] Password strength policy threshold (min length / char classes) — assume **min 8 chars** for MVP unless spec dictates otherwise.
- [ ] Should changing password also invalidate the current session (force re-login)? Default: **no** (keep session; only clear `debeCambiarPassword`).
