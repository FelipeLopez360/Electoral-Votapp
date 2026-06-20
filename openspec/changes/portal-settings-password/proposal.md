# Proposal: Portal Settings & Password

## Intent

Enable Funcionario users to update their password and basic profile information from the voting portal, enforcing a password change on first login and properly invalidating server-side sessions on logout. This addresses basic security and user management needs missing in the MVP.

## Scope

### In Scope
- Enforce `debeCambiarPassword` check during login and redirect accordingly.
- Create a Settings page (`/portal/configuracion`) with Password Change and Profile Update tabs.
- Backend endpoint to update password (`PUT /api/v1/portal/password`) requiring current password verification.
- Backend endpoint to fetch profile (`GET /api/v1/portal/me`).
- Backend endpoint to update profile (`PUT /api/v1/portal/me`) restricted to email and phone.
- Backend logout endpoint (`POST /api/v1/portal/logout`) to invalidate the current Redis session.

### Out of Scope
- Admin panel changes.
- Email verification, 2FA, or other security layers.
- Avatar/photo upload.
- Notifications or activity log.
- Global logout (invalidating all sessions for a user, only current session is affected).

## Capabilities

### New Capabilities
- `portal-settings`: Managing personal profile data (email, phone) from the portal.
- `portal-password-management`: Changing password and enforcing first-login password updates.
- `portal-session-management`: Explicit backend session invalidation (logout).

### Modified Capabilities
- `portal-authentication`: Returning `debeCambiarPassword` flag during login.

## Approach

1. **Backend first**:
   - Update `AuthenticateFuncionarioUseCase` to return `debeCambiarPassword` alongside the session token.
   - Implement `ChangePasswordUseCase` using `PasswordEncoderPort` to verify current and hash new password, adding `updatePasswordHash` to persistence port.
   - Implement `UpdateProfileUseCase` to update email and phone.
   - Implement `LogoutUseCase` to delete the session token from Redis.
   - Create the respective REST controllers under `infrastructure/adapter/in/web/`.
2. **Frontend second**:
   - Update `PortalAuthContext` to handle `debeCambiarPassword` redirecting to the settings page.
   - Build `/portal/configuracion` page with Password and Profile sections.
   - Wire backend API calls via `portalClient`.
   - Update `Logout` action to trigger the `POST /logout` endpoint.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `co.com.votapp.ws.voting.domain` | Modified | Update auth responses, add new use cases for settings and logout. |
| `co.com.votapp.ws.voting.infrastructure.adapter.in.web` | Modified/New | Update login controller, add controllers for me, password, and logout. |
| `co.com.votapp.ws.voting.infrastructure.adapter.out.persistence` | Modified | Add `updatePasswordHash` logic and profile update updates to `Funcionario` entities. |
| `frontend/src/contexts/PortalAuthContext` | Modified | Login flow redirect logic and logout API integration. |
| `frontend/src/pages/portal/` | New | Settings page layout and components. |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Incomplete session invalidation | Low | Ensure `PortalSessionPort.delete(token)` properly removes the token from Redis. |
| Bypassing `debeCambiarPassword` | Medium | In future iterations, block voting endpoints if this flag is true, but for MVP just redirect on login. |

## Rollback Plan

Revert the specific PRs containing these changes. Since the database schema (adding new columns) is not required (columns already exist), rollback only entails reverting application code and frontend changes. The Redis sessions will naturally expire or remain unaffected.

## Dependencies

- None (Relies on existing `Funcionario` domain and Redis setup).

## Success Criteria

- [ ] A Funcionario logging in with `debeCambiarPassword=true` is automatically redirected to the Settings page to change it.
- [ ] Users can successfully change their password after providing the current one.
- [ ] Users can view and update their email and phone number.
- [ ] Clicking logout makes a backend call that invalidates the session token in Redis.
