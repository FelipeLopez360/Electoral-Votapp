# Spec: infrastructure/keycloak
## Change: implement-keycloak-infra
## Domain: Realm, client, protocol mappers, test user

---

## Scope

This spec covers the contents of `docs/keycloak/import/votapp-realm.json` — the Keycloak 24
realm export file imported on container startup. It does NOT cover Spring Boot application code.

---

## Requirements

### REQ-KC-1: Realm MUST be named votapp-realm and enabled

The JSON MUST declare a realm with `"realm": "votapp-realm"` and `"enabled": true`.
The realm MUST set `"sslRequired": "none"` to support HTTP in `start-dev` mode.

#### Scenario: KC-1a — realm is available after import

- GIVEN `votapp-realm.json` contains `"realm": "votapp-realm"` and `"enabled": true`
- WHEN Keycloak starts with `--import-realm`
- THEN `GET http://localhost:8180/realms/votapp-realm` returns HTTP 200
- AND the response body contains `"realm": "votapp-realm"`

#### Scenario: KC-1b — realm OpenID configuration is published

- GIVEN the realm is imported and enabled
- WHEN `GET http://localhost:8180/realms/votapp-realm/.well-known/openid-configuration`
- THEN HTTP 200 is returned
- AND the response contains `"token_endpoint"` and `"jwks_uri"` fields

---

### REQ-KC-2: Client votapp-backend MUST be declared as OpenID Connect

A client with `"clientId": "votapp-backend"` MUST exist in the `clients` array.
Its `"protocol"` MUST be `"openid-connect"`. Its `"publicClient"` MUST be `true`.
Its `"directAccessGrantsEnabled"` MUST be `true` (enables Resource Owner Password Credentials flow).

#### Scenario: KC-2a — token can be obtained without client secret

- GIVEN client `votapp-backend` has `publicClient: true` and `directAccessGrantsEnabled: true`
- WHEN a POST is made to the token endpoint with `client_id=votapp-backend` (no `client_secret`)
- THEN a valid JWT access token is returned (HTTP 200)

---

### REQ-KC-3: Client redirect URIs and web origins MUST be open for development

The `votapp-backend` client MUST set `"redirectUris": ["*"]` and `"webOrigins": ["*"]`.

#### Scenario: KC-3a — no redirect URI error in dev

- GIVEN redirectUris is ["*"] and webOrigins is ["*"]
- WHEN a token request is made from any localhost origin
- THEN no redirect URI mismatch error is returned

---

### REQ-KC-4: Protocol mapper for documento_identidad MUST be declared

The `votapp-backend` client MUST declare a protocol mapper that:
- Maps user attribute `documento_identidad` to JWT claim `custom_documento_id`
- Uses mapper type `oidc-usermodel-attribute-mapper`
- Sets `"access.token.claim": "true"`

#### Scenario: KC-4a — custom_documento_id appears in access token

- GIVEN user `funcionario_test` has attribute `documento_identidad=12345678`
- AND the mapper is correctly configured
- WHEN a token is obtained for `funcionario_test`
- THEN the decoded JWT access token contains `"custom_documento_id": "12345678"`

---

### REQ-KC-5: Protocol mapper for numero_empleado MUST be declared

The `votapp-backend` client MUST declare a protocol mapper that:
- Maps user attribute `numero_empleado` to JWT claim `custom_numero_empleado`
- Uses mapper type `oidc-usermodel-attribute-mapper`
- Sets `"access.token.claim": "true"`

#### Scenario: KC-5a — custom_numero_empleado appears in access token

- GIVEN user `funcionario_test` has attribute `numero_empleado=EMP001`
- WHEN a token is obtained for `funcionario_test`
- THEN the decoded JWT access token contains `"custom_numero_empleado": "EMP001"`

---

### REQ-KC-6: Protocol mapper for departamento_codigo MUST be declared

The `votapp-backend` client MUST declare a protocol mapper that:
- Maps user attribute `departamento_codigo` to JWT claim `custom_depto_cod`
- Uses mapper type `oidc-usermodel-attribute-mapper`
- Sets `"access.token.claim": "true"`

#### Scenario: KC-6a — custom_depto_cod appears in access token

- GIVEN user `funcionario_test` has attribute `departamento_codigo=DEP_BOG`
- WHEN a token is obtained for `funcionario_test`
- THEN the decoded JWT access token contains `"custom_depto_cod": "DEP_BOG"`

---

### REQ-KC-7: Test user funcionario_test MUST be pre-created

The `users` array in the realm JSON MUST include a user with `"username": "funcionario_test"`.
The user's credentials MUST include a password `"1234"` with `"temporary": false`
so no forced password-change flow is triggered on first login.

#### Scenario: KC-7a — token obtained with funcionario_test / 1234

- GIVEN user `funcionario_test` exists with password `1234` (temporary: false)
- WHEN POST to token endpoint with `username=funcionario_test&password=1234`
- THEN HTTP 200 is returned with a valid access_token

#### Scenario: KC-7b — login is not blocked by temporary password prompt

- GIVEN credentials are `temporary: false`
- WHEN a ROPC token request is made
- THEN no `"Account is not fully set up"` error is returned

---

### REQ-KC-8: Test user MUST have the three required attributes

The `funcionario_test` user's `attributes` map MUST contain:
- `documento_identidad`: `["12345678"]`
- `numero_empleado`: `["EMP001"]`
- `departamento_codigo`: `["DEP_BOG"]`

(Keycloak stores user attributes as string arrays in the realm export format.)

#### Scenario: KC-8a — all three custom claims present with correct values

- GIVEN funcionario_test has all three attributes
- WHEN token is obtained and JWT decoded
- THEN access token contains:
  - `"custom_documento_id": "12345678"`
  - `"custom_numero_empleado": "EMP001"`
  - `"custom_depto_cod": "DEP_BOG"`

---

### REQ-KC-9: Realm JSON MUST be valid Keycloak 24 format

The JSON file MUST be valid JSON (parseable without error) and MUST use Keycloak 24
export schema. Deprecated fields from older Keycloak versions (e.g. `accessType`) MUST NOT
be used. The file MUST NOT contain null bytes or BOM characters.

#### Scenario: KC-9a — JSON validates and Keycloak imports without error

- GIVEN `votapp-realm.json` is syntactically valid JSON
- WHEN Keycloak 24 starts with `--import-realm`
- THEN no import error appears in Keycloak logs
- AND the realm, client, and user are all visible in the admin console
