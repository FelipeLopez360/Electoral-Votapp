# Proposal: Implement Keycloak Identity Infrastructure

## Intent

The application currently has no centralized identity provider. Authentication is handled
via HTTP Basic auth directly in Spring Boot, which is tightly coupled, hard to extend, and
does not support token-based federation or custom user attribute claims.

This change introduces Keycloak as an identity infrastructure layer so that future OAuth2
/ OIDC integration (Phase 2) has a running, pre-configured identity server to connect to.
The goal is purely infrastructure: stand up Keycloak in Docker Compose, define the
`votapp-realm` with a public client and custom protocol mappers, seed a test user, and
provide developer documentation for verifying JWT issuance manually.

No Spring Boot code is modified in this change. HTTP Basic auth remains active.

---

## Scope

### In Scope
- `docker-compose.yml` — add `keycloak-db` (postgres, dedicated) and `keycloak` services
- `docs/keycloak/import/votapp-realm.json` — Keycloak realm export file containing:
  - Realm: `votapp-realm`
  - Client: `votapp-backend` (OpenID Connect, public, redirect URIs: *, web origins: *)
  - Protocol Mappers for custom JWT claims:
    - `documento_identidad` → `custom_documento_id` (access token: true)
    - `numero_empleado` → `custom_numero_empleado` (access token: true)
    - `departamento_codigo` → `custom_depto_cod` (access token: true)
  - Test user: `funcionario_test` (password: `1234`, temporary: false)
    - Attributes: documento_identidad=12345678, numero_empleado=EMP001, departamento_codigo=DEP_BOG
- `docs/keycloak/TEST_GUIDE.md` — curl commands for obtaining and inspecting a JWT

### Out of Scope
- `pom.xml` — no oauth2-resource-server dependency added yet
- `SecurityConfig.java` — no replacement of httpBasic() with JWT validation
- `application.yaml` — no oauth2 resource server config
- `AuthController.java` — no changes to login endpoint
- Any modifications to existing test files (VoteControllerE2ETest, etc.)
- Keycloak production hardening (TLS, realm brute-force, email config)

---

## Approach

### Docker Compose layout
Add two new services to the existing `docker-compose.yml`:

1. **`keycloak-db`**: Postgres 15 image, dedicated volume (`keycloak-db-data`), env vars
   `POSTGRES_DB=keycloak`, `POSTGRES_USER=keycloak`, `POSTGRES_PASSWORD=keycloak`.
   No host-port exposure (internal only).

2. **`keycloak`**: `quay.io/keycloak/keycloak:24.0`, depends on `keycloak-db`,
   command: `start-dev --import-realm`, env vars `KC_DB=postgres`,
   `KC_DB_URL=jdbc:postgresql://keycloak-db:5432/keycloak`, `KC_DB_USERNAME/PASSWORD`,
   `KEYCLOAK_ADMIN=admin`, `KEYCLOAK_ADMIN_PASSWORD=admin`.
   Host port: `8180:8080` (avoids collision with Spring Boot on 8080).
   Volume: `./docs/keycloak/import:/opt/keycloak/data/import:ro`.

### Realm JSON strategy
Use Keycloak's import-on-startup feature (`--import-realm`). The realm JSON will be a
minimal but complete export structure (Keycloak 24 schema), containing all client and
mapper definitions and the test user credential. This avoids manual UI configuration
and makes setup fully reproducible.

### Test Guide
`TEST_GUIDE.md` provides step-by-step verification:
1. `curl` POST to `/realms/votapp-realm/protocol/openid-connect/token` with password grant.
2. Decode the JWT payload with `base64` / `jq` to inspect `custom_documento_id`,
   `custom_numero_empleado`, `custom_depto_cod` claims.

---

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `docker-compose.yml` | Modified | Add keycloak-db and keycloak services + volume declaration |
| `docs/keycloak/import/votapp-realm.json` | New | Keycloak realm config with client, mappers, test user |
| `docs/keycloak/TEST_GUIDE.md` | New | Developer guide for JWT verification |

---

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| `--import-realm` silently skips if realm already exists | Low | Document in TEST_GUIDE: `docker compose down -v` to re-import |
| Keycloak 24 schema differs from older realm JSONs | Med | Generate JSON against Keycloak 24 spec; avoid deprecated fields |
| Port 8180 conflict on dev machines | Low | Document alternate port override in TEST_GUIDE |
| Test user password marked temporary causes login flow issues | Low | Set `temporary: false` in credentials block of realm JSON |
| `docs/keycloak/import` volume path wrong on Windows | Low | Docker Desktop handles forward-slash paths correctly |

---

## Rollback Plan

This change adds only new files and new Docker Compose services.
To roll back:
1. Remove the two new services (`keycloak`, `keycloak-db`) from `docker-compose.yml`.
2. Remove the `keycloak-db-data` volume declaration.
3. Delete `docs/keycloak/` directory.
4. Run `docker compose down -v` to remove any created containers and volumes.

No existing services, source code, or test files are touched; rollback is zero-risk.

---

## Dependencies

- Docker Compose (already in use for postgres + redis services)
- Internet access to pull `quay.io/keycloak/keycloak:24.0` and `postgres:15`
- Existing `docker-compose.yml` must remain valid YAML after edits

---

## Success Criteria

- [ ] `docker compose up keycloak` starts without error; admin UI accessible at http://localhost:8180
- [ ] `votapp-realm` realm is auto-imported on first startup
- [ ] `votapp-backend` client is visible in Keycloak admin console
- [ ] Token request for `funcionario_test` returns a valid JWT (HTTP 200)
- [ ] Decoded JWT access token contains claims: `custom_documento_id`, `custom_numero_empleado`, `custom_depto_cod` with correct values
- [ ] `docker compose up` for existing services (postgres, redis) still works without modification
- [ ] `docs/keycloak/TEST_GUIDE.md` curl commands produce expected output without modification
