# Spec: infrastructure/docs
## Change: implement-keycloak-infra
## Domain: TEST_GUIDE.md — developer documentation for token retrieval and JWT verification

---

## Scope

This spec covers the contents and completeness of `docs/keycloak/TEST_GUIDE.md`.
The guide MUST enable a developer with Docker and curl to verify the Keycloak setup
end-to-end without reading any other document.

---

## Requirements

### REQ-DOCS-1: TEST_GUIDE.md MUST exist at docs/keycloak/TEST_GUIDE.md

The file MUST be created at the path `docs/keycloak/TEST_GUIDE.md`.

#### Scenario: DOCS-1a — file exists

- GIVEN the implement-keycloak-infra change has been applied
- WHEN `ls docs/keycloak/TEST_GUIDE.md` is executed
- THEN the file exists and is non-empty

---

### REQ-DOCS-2: Guide MUST include a Prerequisites section

The guide MUST list: Docker Compose, `curl`, and a JWT decode utility (`jq` or browser decoder).

#### Scenario: DOCS-2a — developer can identify required tools

- GIVEN a developer reads TEST_GUIDE.md
- WHEN they reach the Prerequisites section
- THEN they can identify all tools needed before running any command

---

### REQ-DOCS-3: Guide MUST include a Step 1 — Start services

The guide MUST show the exact command to start the Keycloak services and advise
waiting for the admin console at http://localhost:8180.

---

### REQ-DOCS-4: Guide MUST include a Step 2 — Obtain access token

The guide MUST include a complete, copy-pasteable `curl` command that:
- POSTs to `http://localhost:8180/realms/votapp-realm/protocol/openid-connect/token`
- Uses `grant_type=password`, `client_id=votapp-backend`
- Uses `username=funcionario_test` and `password=1234`
- Formats output with `| jq .` or `| python3 -m json.tool`

#### Scenario: DOCS-4a — curl command produces a valid token response

- GIVEN Keycloak is running and the realm is imported
- WHEN the exact curl command from TEST_GUIDE.md is copy-pasted and executed
- THEN HTTP 200 is returned with `access_token` in the JSON response

#### Scenario: DOCS-4b — guide shows where to find the access_token

- GIVEN the token response is received
- WHEN the developer reads the guide
- THEN they know to extract the `access_token` field for use in subsequent steps

---

### REQ-DOCS-5: Guide MUST include a Step 3 — Decode JWT and verify custom claims

The guide MUST include commands to decode the JWT payload and display the custom claims.
It MUST provide both macOS (`base64 -D`) and Linux (`base64 -d`) variants.
It MUST explicitly list the expected claim names and values.

#### Scenario: DOCS-5a — developer can see all three custom claims

- GIVEN a valid access_token has been obtained
- WHEN the decode command from TEST_GUIDE.md is executed
- THEN the decoded payload shows `custom_documento_id`, `custom_numero_empleado`, `custom_depto_cod`

#### Scenario: DOCS-5b — guide specifies expected values for the test user

- GIVEN the developer reads the guide
- THEN they can verify the claim values against expected:
  - `custom_documento_id` = `"12345678"`
  - `custom_numero_empleado` = `"EMP001"`
  - `custom_depto_cod` = `"DEP_BOG"`

---

### REQ-DOCS-6: All commands MUST be in fenced code blocks

Every shell command in the guide MUST be wrapped in a fenced code block (``` or ~~~)
to enable syntax highlighting and clear copy-paste boundaries.

#### Scenario: DOCS-6a — commands are clearly delimited

- GIVEN a developer reads the guide in any Markdown renderer
- WHEN they locate a command
- THEN the command is visually distinct from prose and can be selected/copied cleanly

---

### REQ-DOCS-7: Guide MUST include a Troubleshooting section

The guide MUST address at minimum:
- Connection refused on port 8180 (Keycloak still starting)
- HTTP 401 on token request (wrong credentials or realm not imported)

#### Scenario: DOCS-7a — connection refused troubleshooting

- GIVEN Keycloak is still starting
- WHEN a developer reads the Troubleshooting section
- THEN they find a command to check Keycloak logs: `docker compose logs keycloak`

#### Scenario: DOCS-7b — 401 unauthorized troubleshooting

- GIVEN the token request returns HTTP 401
- WHEN a developer reads the Troubleshooting section
- THEN they find instructions to verify realm import and re-import via `docker compose down -v`
