# Design: implement-keycloak-infra

## Technical Approach

Add Keycloak 24.0 as a containerized identity provider to the Electoral-Votapp development
environment. Two new services (`keycloak-db` + `keycloak`) are added to `docker-compose.yml`.
A pre-built realm JSON is version-controlled and auto-imported on first startup via the
`--import-realm` flag. A developer test guide verifies the full token issuance flow with
custom JWT claims. No Spring Boot application code is modified in this phase.

---

## Architecture Decisions

### AD-1: Separate keycloak-db (not reusing votapp-postgres)

**Choice**: Dedicated `keycloak-db` PostgreSQL service, not shared with `votapp-postgres`.
**Alternatives considered**: Reusing `votapp-postgres` with a separate DB name / second schema.
**Rationale**:
- Keycloak creates 80+ internal tables (REALM, REALM_ATTRIBUTE, KEYCLOAK_ROLE, USER_ENTITY, etc.)
  that would pollute the `sistema_votaciones_institucional` schema.
- Separate service allows independent lifecycle: wipe Keycloak data (`docker compose down -v keycloak-db`)
  without touching app data.
- `keycloak-db` is NOT exposed to the host (no `ports:` mapping), reducing attack surface.
- Matches standard Keycloak documentation and production patterns.

### AD-2: Port 8180:8080 for Keycloak

**Choice**: Map container port 8080 to host port 8180.
**Alternatives considered**: 9090, 8090, changing Spring Boot port.
**Rationale**:
- Spring Boot backend runs natively (not containerized) on host port 8080 by default.
- 8180 is the conventional Keycloak alternate port — widely documented and familiar to devs.
- No conflict with postgres (5432) or redis (6379).

### AD-3: start-dev mode (not production mode)

**Choice**: `command: start-dev --import-realm` with `KC_HTTP_ENABLED=true`.
**Alternatives considered**: `start --optimized` (production mode).
**Rationale**:
- Production mode requires TLS certificates and HTTPS — unnecessary overhead for dev.
- `start-dev` enables admin console at `/` root path, disables TLS requirement.
- Fast startup; no build step required.
- Clearly scoped as dev-only; future production phase will use hardened config.

### AD-4: Realm import via --import-realm flag + volume mount

**Choice**: Version-controlled JSON at `docs/keycloak/import/votapp-realm.json`, mounted as
`:ro` at `/opt/keycloak/data/import`, Keycloak started with `--import-realm`.
**Alternatives considered**: Manual UI configuration; post-startup Admin REST API script.
**Rationale**:
- Declarative, reproducible, and version-controlled — any developer can `git clone` + `docker compose up`.
- `:ro` mount prevents the container from modifying the source file.
- Keycloak 24 natively supports `--import-realm`; no custom entrypoint script needed.
- If realm already exists in DB, import is skipped gracefully (idempotent).

### AD-5: oidc-usermodel-attribute-mapper for custom JWT claims

**Choice**: Protocol mapper type `oidc-usermodel-attribute-mapper` for all three custom claims.
**Alternatives considered**: `oidc-hardcoded-claim-mapper`, `oidc-script-based-protocol-mapper`.
**Rationale**:
- `oidc-usermodel-attribute-mapper` is the standard Keycloak mapper for per-user attributes.
- It reads from `user.attributes` at token issuance time — no scripting or custom SPI needed.
- Supports `jsonType.label: String` for simple string claims.
- Maps directly to the three `Funcionario` domain fields needed by the backend.

### AD-6: temporary: false for test user credentials

**Choice**: Set `"temporary": false` on the `funcionario_test` password credential.
**Alternatives considered**: `temporary: true` (Keycloak default for admin-created users).
**Rationale**:
- `temporary: true` forces a password change on first login, which breaks ROPC (curl) flows.
- For a seed/test user intended for automated verification, `temporary: false` is required.
- Clearly documented in TEST_GUIDE.md as a dev-only user — not a security concern.

---

## Data Flow

```
Developer
   │
   │  POST /realms/votapp-realm/protocol/openid-connect/token
   │  grant_type=password&client_id=votapp-backend
   │  &username=funcionario_test&password=1234
   ▼
Keycloak 24.0 (container: votapp-keycloak, port 8180)
   ├── Looks up user in keycloak-db (PostgreSQL)
   ├── Validates password (bcrypt, stored in keycloak-db)
   ├── Runs protocol mappers for votapp-backend client:
   │     documento_identidad  → custom_documento_id   = "12345678"
   │     numero_empleado      → custom_numero_empleado = "EMP001"
   │     departamento_codigo  → custom_depto_cod       = "DEP_BOG"
   ├── Signs JWT (RS256, keys in keycloak-db)
   └── Returns { access_token, refresh_token, expires_in, ... }
   │
   ▼
Developer decodes JWT payload (base64 segment 2):
{
  "iss":                    "http://localhost:8180/realms/votapp-realm",
  "sub":                    "<uuid>",
  "preferred_username":     "funcionario_test",
  "custom_documento_id":    "12345678",
  "custom_numero_empleado": "EMP001",
  "custom_depto_cod":       "DEP_BOG"
}
```

---

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `docker-compose.yml` | Modify | Add `keycloak-db` service, `keycloak` service, `keycloak-db-data` volume |
| `docs/keycloak/import/votapp-realm.json` | Create | Keycloak 24 realm export: realm, client, 3 mappers, 1 test user |
| `docs/keycloak/TEST_GUIDE.md` | Create | Step-by-step developer guide: start → get token → decode JWT |

---

## Docker Compose Additions

### keycloak-db service

```yaml
keycloak-db:
  image: postgres:15
  container_name: votapp-keycloak-db
  environment:
    POSTGRES_DB: keycloak
    POSTGRES_USER: keycloak
    POSTGRES_PASSWORD: keycloak
  volumes:
    - keycloak-db-data:/var/lib/postgresql/data
  networks:
    - votapp-network
  healthcheck:
    test: ["CMD-SHELL", "pg_isready -U keycloak -d keycloak"]
    interval: 5s
    timeout: 5s
    retries: 5
```

### keycloak service

```yaml
keycloak:
  image: quay.io/keycloak/keycloak:24.0
  container_name: votapp-keycloak
  command: start-dev --import-realm
  environment:
    KEYCLOAK_ADMIN: admin
    KEYCLOAK_ADMIN_PASSWORD: admin
    KC_DB: postgres
    KC_DB_URL: jdbc:postgresql://keycloak-db:5432/keycloak
    KC_DB_USERNAME: keycloak
    KC_DB_PASSWORD: keycloak
  ports:
    - "8180:8080"
  volumes:
    - ./docs/keycloak/import:/opt/keycloak/data/import:ro
  depends_on:
    keycloak-db:
      condition: service_started
  networks:
    - votapp-network
```

### Top-level volumes addition

```yaml
volumes:
  postgres_data:      # existing
  redis_data:         # existing
  keycloak-db-data:   # new
```

---

## Realm JSON Key Fields (votapp-realm.json)

```json
{
  "realm": "votapp-realm",
  "enabled": true,
  "sslRequired": "none",
  "clients": [{
    "clientId": "votapp-backend",
    "protocol": "openid-connect",
    "publicClient": true,
    "directAccessGrantsEnabled": true,
    "redirectUris": ["*"],
    "webOrigins": ["*"],
    "protocolMappers": [
      {
        "name": "documento_identidad",
        "protocol": "openid-connect",
        "protocolMapper": "oidc-usermodel-attribute-mapper",
        "config": {
          "user.attribute": "documento_identidad",
          "claim.name": "custom_documento_id",
          "access.token.claim": "true",
          "jsonType.label": "String"
        }
      }
      // ... (+ 2 more mappers for numero_empleado and departamento_codigo)
    ]
  }],
  "users": [{
    "username": "funcionario_test",
    "enabled": true,
    "attributes": {
      "documento_identidad": ["12345678"],
      "numero_empleado":     ["EMP001"],
      "departamento_codigo": ["DEP_BOG"]
    },
    "credentials": [{
      "type": "password",
      "value": "1234",
      "temporary": false
    }]
  }]
}
```

> **Note**: User attributes MUST be arrays (`["value"]`) — this is the Keycloak 24 export format.

---

## Testing Strategy

This is a pure infrastructure change with no JUnit-testable components.
Verification is entirely manual, as documented in `docs/keycloak/TEST_GUIDE.md`.

| Step | What to Test | How |
|------|-------------|-----|
| 1 | Docker Compose YAML is valid | `docker compose config --quiet` |
| 2 | keycloak-db starts healthy | `docker compose up -d keycloak-db` + healthcheck |
| 3 | Realm is auto-imported | Keycloak logs: `"votapp-realm imported"` |
| 4 | Admin console accessible | Browser: `http://localhost:8180/admin` |
| 5 | Token issuance works | curl ROPC request → HTTP 200 + access_token |
| 6 | Custom claims in JWT | Decode JWT payload, verify 3 custom claim names + values |
| 7 | Existing services unaffected | `docker compose up -d postgres redis` → no errors |

---

## Migration / Rollout

No data migration required. This change is purely additive.

To remove: delete the two new services from `docker-compose.yml`, delete `docs/keycloak/`,
run `docker compose down -v` to purge volumes.

---

## Open Questions

- None. All decisions are resolved. The change is ready for implementation.
