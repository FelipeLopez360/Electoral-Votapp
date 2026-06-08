# Spec: infrastructure/docker
## Change: implement-keycloak-infra
## Domain: Docker Compose service configuration

---

## Scope

This spec covers the required modifications to `docker-compose.yml` to introduce two new
services: `keycloak-db` (dedicated PostgreSQL 15) and `keycloak` (Keycloak 24.0 identity server).
No existing services in docker-compose.yml MUST be modified.

---

## Requirements

### REQ-DOCKER-1: keycloak-db service MUST be declared

The `docker-compose.yml` MUST declare a service named `keycloak-db` using the `postgres:15`
image. The service MUST NOT expose any host ports (internal-only). It MUST define the
following environment variables:
- `POSTGRES_DB=keycloak`
- `POSTGRES_USER=keycloak`
- `POSTGRES_PASSWORD=keycloak`

It MUST mount a named volume `keycloak-db-data` at `/var/lib/postgresql/data`.

#### Scenario: DOCKER-1a — keycloak-db starts and is reachable from keycloak

- GIVEN docker-compose.yml declares keycloak-db with postgres:15
- WHEN `docker compose up -d keycloak-db` is executed
- THEN the container starts healthy (pg_isready returns 0)
- AND the keycloak service can connect to it via `keycloak-db:5432`

#### Scenario: DOCKER-1b — keycloak-db data persists across restarts

- GIVEN keycloak-db has been started and used
- WHEN `docker compose restart keycloak-db` is executed
- THEN the `keycloak-db-data` volume retains all Keycloak internal tables

---

### REQ-DOCKER-2: keycloak service MUST be declared

The `docker-compose.yml` MUST declare a service named `keycloak` using the image
`quay.io/keycloak/keycloak:24.0`. The service MUST map host port `8180` to container
port `8080`. It MUST pass the command arguments `start-dev --import-realm`.

#### Scenario: DOCKER-2a — keycloak admin console is accessible

- GIVEN the keycloak service is declared with port `8180:8080`
- WHEN `docker compose up -d keycloak` is executed and Keycloak finishes starting
- THEN `curl http://localhost:8180/realms/votapp-realm` returns HTTP 200
- AND `http://localhost:8180/admin` is accessible in a browser

---

### REQ-DOCKER-3: keycloak environment variables MUST be set

The `keycloak` service MUST define at minimum the following environment variables:
- `KEYCLOAK_ADMIN=admin`
- `KEYCLOAK_ADMIN_PASSWORD=admin`
- `KC_DB=postgres`
- `KC_DB_URL=jdbc:postgresql://keycloak-db:5432/keycloak`
- `KC_DB_USERNAME=keycloak`
- `KC_DB_PASSWORD=keycloak`

#### Scenario: DOCKER-3a — Keycloak connects to its database

- GIVEN all KC_DB_* env vars are correctly set
- WHEN keycloak starts
- THEN Keycloak logs show successful DB connection (no "Failed to connect" errors)
- AND the admin console is reachable at http://localhost:8180

---

### REQ-DOCKER-4: keycloak MUST depend on keycloak-db

The `keycloak` service MUST declare `depends_on: keycloak-db` so Compose starts
the database container before Keycloak.

---

### REQ-DOCKER-5: import volume MUST be mounted read-only

The `keycloak` service MUST mount `./docs/keycloak/import` at
`/opt/keycloak/data/import` with the `:ro` flag.

#### Scenario: DOCKER-5a — realm JSON is auto-imported on first startup

- GIVEN `votapp-realm.json` exists in `./docs/keycloak/import/`
- AND the volume is mounted at `/opt/keycloak/data/import:ro`
- WHEN keycloak starts with `--import-realm`
- THEN Keycloak logs show `"Realm votapp-realm imported"`
- AND `GET /realms/votapp-realm` returns HTTP 200

#### Scenario: DOCKER-5b — existing realm is not re-imported on restart

- GIVEN votapp-realm already exists in keycloak-db from a previous start
- WHEN `docker compose restart keycloak` is executed
- THEN Keycloak logs show realm import was skipped (no duplicate realm error)
- AND the realm remains functional

---

### REQ-DOCKER-6: named volume MUST be declared at top level

The `docker-compose.yml` top-level `volumes:` block MUST include `keycloak-db-data`
alongside the existing `postgres_data` and `redis_data` volumes.

---

### REQ-DOCKER-7: existing services MUST NOT be modified

The existing `postgres` and `redis` services in `docker-compose.yml` MUST remain
byte-for-byte identical to their state before this change.

#### Scenario: DOCKER-7a — existing services unaffected

- GIVEN docker-compose.yml has been modified to add keycloak services
- WHEN `docker compose up -d postgres redis` is executed
- THEN both services start successfully with their original configuration
- AND the votapp application can still connect to postgres:5432 and redis:6379
