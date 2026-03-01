# API Documentation Specification

## Purpose

This specification defines the requirements for exposing a self-hosted OpenAPI / Swagger UI documentation layer on the Electoral Votapp REST API. It covers endpoint availability, security scheme visibility, controller grouping, per-endpoint documentation quality, and the security boundary between the public docs paths and the protected `/api/**` routes.

---

## Requirements

### REQ-1: OpenAPI Endpoint Availability

The system MUST expose an OpenAPI 3.x JSON document at `GET /v3/api-docs`.

The endpoint SHALL return a valid OpenAPI 3.x JSON object with HTTP status 200.

The document MUST list all 5 controller groups and their operations.

The endpoint SHALL be reachable without any HTTP Basic credentials.

#### Scenario: OpenAPI spec loads without authentication

- GIVEN the application is running
- WHEN a client sends `GET /v3/api-docs` with no `Authorization` header
- THEN the response status SHALL be `200 OK`
- AND the `Content-Type` header SHALL contain `application/json`
- AND the response body SHALL be a valid OpenAPI 3.x JSON object (containing `openapi`, `info`, and `paths` top-level keys)

#### Scenario: OpenAPI spec is rejected when credentials are required but docs are whitelisted

- GIVEN the application is running
- WHEN a client sends `GET /v3/api-docs` with an invalid `Authorization: Basic <bad-credentials>` header
- THEN the response status SHALL still be `200 OK` (the docs path is permit-all; bad credentials are ignored, not rejected)

#### Scenario: OpenAPI spec includes all registered routes

- GIVEN the application is running and all 5 controllers are loaded
- WHEN a client fetches `GET /v3/api-docs`
- THEN the `paths` object in the response body SHALL contain at minimum:
  - `POST /api/v1/votes`
  - `POST /api/v1/auth/login`
  - at least one path registered by `EleccionController`
  - at least one path registered by `CandidatesController`
  - at least one path registered by `OrganizationController`

---

### REQ-2: Swagger UI Availability

The system MUST expose an interactive Swagger UI at `GET /swagger-ui.html`.

The UI SHALL render without requiring HTTP Basic authentication.

The system SHOULD also respond at `/swagger-ui/index.html` (the canonical redirect target used internally by SpringDoc).

#### Scenario: Swagger UI loads without authentication

- GIVEN the application is running
- WHEN a client sends `GET /swagger-ui.html` with no `Authorization` header
- THEN the response status SHALL be `200 OK` or a redirect (3xx) to the Swagger UI index page
- AND following any redirect SHALL ultimately return a page containing the Swagger UI HTML

#### Scenario: Swagger UI index is directly accessible

- GIVEN the application is running
- WHEN a client sends `GET /swagger-ui/index.html` with no `Authorization` header
- THEN the response status SHALL be `200 OK`
- AND the response body SHALL contain HTML that initializes the Swagger UI

#### Scenario: Swagger UI is accessible with wrong credentials

- GIVEN the application is running
- WHEN a client sends `GET /swagger-ui.html` with an invalid `Authorization: Basic <bad-credentials>` header
- THEN the response status SHALL be `200 OK` or a redirect to the UI page (docs path is permit-all; invalid credentials are not rejected for this path)

---

### REQ-3: Security Scheme Documentation

The system MUST declare an HTTP Basic security scheme named `basicAuth` in the OpenAPI document.

The security scheme SHALL be visible in the Swagger UI "Authorize" dialog.

The scheme type MUST be `http` with scheme `basic`.

All protected endpoints (under `/api/**`) SHOULD reference the `basicAuth` security requirement so that the Swagger UI "lock" icon is shown on those operations.

#### Scenario: basicAuth security scheme present in OpenAPI document

- GIVEN the application is running
- WHEN a client fetches `GET /v3/api-docs`
- THEN the response body SHALL contain a `components.securitySchemes.basicAuth` object
- AND that object SHALL have `type: http` and `scheme: basic`

#### Scenario: Authorize dialog shows Basic Auth option in Swagger UI

- GIVEN the application is running and the Swagger UI is loaded in a browser
- WHEN the user clicks the "Authorize" button in the Swagger UI
- THEN a dialog SHALL appear listing a `basicAuth` entry of type `http (Basic)`

---

### REQ-4: Controller Grouping

The system MUST expose all 5 REST controllers as distinct named tag groups in the OpenAPI document and Swagger UI.

Each controller SHALL be annotated with `@Tag(name=..., description=...)` to produce a human-readable group label.

The 5 required tag names are:
- `Votes` (from `VoteController`)
- `Authentication` (from `AuthController`)
- `Elections` (from `EleccionController`)
- `Candidates` (from `CandidatesController`)
- `Organizations` (from `OrganizationController`)

The exact tag name strings are informational; the requirement is that each controller produces exactly one distinct tag visible in the UI.

#### Scenario: All 5 controller groups appear in OpenAPI tags

- GIVEN the application is running
- WHEN a client fetches `GET /v3/api-docs`
- THEN the response body SHALL contain a `tags` array with at least 5 entries
- AND each of the 5 controllers SHALL contribute at least one distinct tag name to that array

#### Scenario: Tag groups are visible in Swagger UI

- GIVEN the Swagger UI is loaded
- WHEN the user views the main operations list
- THEN at least 5 collapsed operation groups SHALL be visible, each labeled with the corresponding controller's tag name

---

### REQ-5: Endpoint Documentation

Every handler method on each of the 5 controllers MUST be annotated with `@Operation` providing a non-empty `summary` and `description`.

Every handler method MUST declare at least the following `@ApiResponse` entries where applicable:
- `200` — successful response
- `400` — bad request (for endpoints that accept a request body)
- `401` — unauthorized (for endpoints that enforce HTTP Basic)
- `404` — not found (for endpoints that look up resources by identifier)

The OpenAPI `paths` object SHALL include the response codes declared via `@ApiResponse` for each operation.

#### Scenario: Vote submission endpoint is fully documented

- GIVEN the application is running
- WHEN a client fetches `GET /v3/api-docs`
- THEN the `paths["/api/v1/votes"]["post"]` object SHALL contain a non-empty `summary`
- AND SHALL contain a non-empty `description`
- AND SHALL contain response entries for at least `200`, `400`, and `401`

#### Scenario: Login endpoint is fully documented

- GIVEN the application is running
- WHEN a client fetches `GET /v3/api-docs`
- THEN the `paths["/api/v1/auth/login"]["post"]` object SHALL contain a non-empty `summary`
- AND SHALL contain response entries for at least `200` and `401`

#### Scenario: Missing request body still returns 400 (regression check)

- GIVEN the application is running and `POST /api/v1/votes` requires a JSON body
- WHEN a client sends `POST /api/v1/votes` with a valid `Authorization` header but an empty body
- THEN the response status SHALL be `400 Bad Request`
- AND the documented `400` response SHALL match this observed behavior

#### Scenario: All EleccionController endpoints have summaries

- GIVEN the application is running
- WHEN a client fetches `GET /v3/api-docs`
- THEN every operation listed under `EleccionController`'s tag SHALL have a non-empty `summary` field

---

### REQ-6: Documentation Paths Are Publicly Accessible (No Auth Required)

The system MUST whitelist the following paths in `SecurityConfig` so that they do NOT require HTTP Basic authentication:

- `/v3/api-docs/**`
- `/swagger-ui/**`
- `/swagger-ui.html`

The security configuration SHALL grant `permitAll()` to requests matching these patterns.

The security configuration MUST NOT remove or weaken the `authenticated()` rule applied to `/api/**`.

#### Scenario: OpenAPI JSON is accessible without credentials

- GIVEN the application is running
- WHEN a client sends `GET /v3/api-docs` with no `Authorization` header
- THEN the response status SHALL be `200 OK` (not `401 Unauthorized`)

#### Scenario: Swagger UI is accessible without credentials

- GIVEN the application is running
- WHEN a client sends `GET /swagger-ui.html` with no `Authorization` header
- THEN the response status SHALL NOT be `401 Unauthorized`

#### Scenario: SpringDoc sub-resources are accessible without credentials

- GIVEN the application is running
- WHEN a client requests any path matching `/swagger-ui/**` (e.g. `/swagger-ui/swagger-ui.css`) with no `Authorization` header
- THEN the response status SHALL NOT be `401 Unauthorized`

---

### REQ-7: Existing API Endpoints Still Require HTTP Basic (No Security Regression)

All endpoints under `/api/**` MUST continue to enforce HTTP Basic authentication after the documentation layer is added.

An unauthenticated request to any `/api/**` endpoint SHALL return `401 Unauthorized`.

Adding SpringDoc MUST NOT alter the authentication or authorization behavior of any existing business endpoint.

#### Scenario: Unauthenticated vote submission is rejected

- GIVEN the application is running
- WHEN a client sends `POST /api/v1/votes` with no `Authorization` header
- THEN the response status SHALL be `401 Unauthorized`

#### Scenario: Unauthenticated login call is rejected

- GIVEN the application is running
- WHEN a client sends `POST /api/v1/auth/login` with no `Authorization` header
- THEN the response status SHALL be `401 Unauthorized`

#### Scenario: Authenticated vote submission still succeeds

- GIVEN the application is running and valid HTTP Basic credentials exist
- WHEN a client sends `POST /api/v1/votes` with a valid `Authorization: Basic <credentials>` header and a well-formed body
- THEN the response status SHALL be `200 OK` or `201 Created`

#### Scenario: Duplicate vote token still returns 409 after adding docs (edge case)

- GIVEN the application is running and a vote with token `TOKEN-DUPLICATE` has already been submitted
- WHEN a client sends `POST /api/v1/votes` with valid credentials and the same token `TOKEN-DUPLICATE`
- THEN the response status SHALL be `409 Conflict`
- AND this behavior SHALL be unchanged from pre-documentation behavior

#### Scenario: Unauthenticated EleccionController request is rejected

- GIVEN the application is running
- WHEN a client sends a request to any `/api/**` path handled by `EleccionController` with no `Authorization` header
- THEN the response status SHALL be `401 Unauthorized`
