# Specifications: Portal Settings & Password

## 1. Portal Authentication (MODIFIED)

### Requirement: Return Password Change Flag
The system MUST include `debeCambiarPassword` in the login response DTO.

#### Scenario: User logs in successfully
- GIVEN a user with valid credentials
- WHEN the user submits a login request to `/api/v1/portal/login`
- THEN the system returns a 200 OK response
- AND the response includes `sessionToken`, `funcionarioId`, `nombre`, and `debeCambiarPassword`

### Requirement: Non-blocking Login
The system SHOULD NOT block the user from authenticating at the API level if `debeCambiarPassword=true`.

#### Scenario: User with debeCambiarPassword=true logs in
- GIVEN a user whose `debe_cambiar_password` flag is set to true in the database
- WHEN the user submits a login request to `/api/v1/portal/login`
- THEN the system returns a 200 OK response with `debeCambiarPassword=true`
- AND the backend creates a valid session in Redis

## 2. Portal Password Management (ADDED)

### Requirement: Change Password Endpoint
The system MUST provide a `PUT /api/v1/portal/password` endpoint requiring a valid session token.

#### Scenario: Successful password change
- GIVEN a logged-in user
- WHEN the user submits a valid `currentPassword`, `newPassword`, and matching `confirmNewPassword`
- AND `newPassword` meets strength rules (min 8 chars, 1 uppercase, 1 lowercase, 1 number)
- AND `newPassword` is different from `currentPassword`
- THEN the system updates the password hash in the database
- AND sets `debe_cambiar_password` to `false`
- AND returns a 200 OK response

#### Scenario: Incorrect current password
- GIVEN a logged-in user
- WHEN the user submits an incorrect `currentPassword`
- THEN the system returns a 4xx error (e.g., 401 Unauthorized or 400 Bad Request) with an appropriate error message
- AND the password is not changed

#### Scenario: Passwords do not match
- GIVEN a logged-in user
- WHEN the user submits a `newPassword` that does not match `confirmNewPassword`
- THEN the system returns a 400 Bad Request with a validation error

#### Scenario: Password strength fails
- GIVEN a logged-in user
- WHEN the user submits a `newPassword` that lacks an uppercase letter, lowercase letter, number, or is under 8 characters
- THEN the system returns a 400 Bad Request detailing the password strength policy

#### Scenario: New password same as old
- GIVEN a logged-in user
- WHEN the user submits a `newPassword` identical to `currentPassword`
- THEN the system returns a 400 Bad Request indicating the password must be new

## 3. Portal Settings (ADDED)

### Requirement: Get Profile Endpoint
The system MUST provide a `GET /api/v1/portal/me` endpoint to retrieve user profile data.

#### Scenario: Fetch user profile
- GIVEN a logged-in user
- WHEN the user requests `/api/v1/portal/me`
- THEN the system returns a 200 OK response
- AND the response contains `nombres`, `apellidos`, `documentoIdentidad`, `email`, `telefono`, and `numeroEmpleado`

### Requirement: Update Profile Endpoint
The system MUST provide a `PUT /api/v1/portal/me` endpoint to update contact details.

#### Scenario: Update email and phone
- GIVEN a logged-in user
- WHEN the user submits an update request with valid `email` and `telefono`
- THEN the system updates these fields in the database
- AND returns a 200 OK response

#### Scenario: Attempt to update restricted fields
- GIVEN a logged-in user
- WHEN the user submits an update request attempting to modify `nombres`, `apellidos`, `documentoIdentidad`, or `numeroEmpleado`
- THEN the system MUST ignore those fields (or return a 400 Bad Request)
- AND the restricted fields remain unchanged in the database

## 4. Portal Session Management (ADDED)

### Requirement: Logout Endpoint
The system MUST provide a `POST /api/v1/portal/logout` endpoint to invalidate server-side sessions.

#### Scenario: User logs out
- GIVEN a logged-in user with an active session in Redis
- WHEN the user requests `/api/v1/portal/logout`
- THEN the system removes the session token from Redis
- AND returns a 200 OK (or 204 No Content) response

#### Scenario: Logout without valid session
- GIVEN an unauthenticated user or an invalid token
- WHEN a request is made to `/api/v1/portal/logout`
- THEN the system returns a 401 Unauthorized (or ignores it and returns 200/204)

## 5. UI/UX Requirements (ADDED)

### Requirement: First-Login Enforcement
The frontend MUST enforce password changes on first login based on the API response.

#### Scenario: First login redirect
- GIVEN a user logs in successfully
- WHEN the API returns `debeCambiarPassword=true`
- THEN the frontend redirects the user to `/portal/configuracion?firstLogin=true`

#### Scenario: First login settings view
- GIVEN a user is on `/portal/configuracion?firstLogin=true`
- WHEN the page loads
- THEN the system shows ONLY the password change form
- AND displays a notice that a password change is required

#### Scenario: First login success redirect
- GIVEN a user is on the first login settings view
- WHEN the user successfully changes their password
- THEN the frontend redirects the user to the dashboard

### Requirement: Settings Page
The frontend MUST provide a configuration interface for authenticated users.

#### Scenario: Standard settings navigation
- GIVEN a logged-in user with `debeCambiarPassword=false`
- WHEN the user navigates to `/portal/configuracion`
- THEN the page displays two sections: "Cambiar contraseña" and "Datos personales"
- AND the "Datos personales" section allows editing `email` and `telefono`
- AND `nombres`, `apellidos`, `documentoIdentidad`, and `numeroEmpleado` are rendered read-only

### Requirement: Frontend Logout
The frontend MUST handle logging out and cleaning local state.

#### Scenario: Initiating logout
- GIVEN a logged-in user
- WHEN the user clicks the "Logout" button in the navigation header
- THEN the frontend calls `POST /api/v1/portal/logout`
- AND clears the local session (localStorage/sessionStorage/Context) regardless of API success or network error
- AND redirects the user to the login screen