# Tasks: implement-keycloak-infra

## Phase 0: Preparación y contexto

- [ ] 0.1 Revisar el estado limpio del repo (`git status`, `git diff`) antes de tocar `docker-compose.yml` para asegurar que no arrastramos ruido.
- [ ] 0.2 Leer la propuesta (`proposal.md`) para validar alcance y riesgos antes de implementar cualquier cambio técnico.

## Phase 1: Infraestructura Docker Compose

Objetivo: añadir Keycloak y su Postgres dedicado sin romper los servicios existentes.

- [ ] 1.1 Añadir servicio `keycloak-db` (Postgres 15) justo después de `redis`.
  - `container_name`: `votapp-keycloak-db`
  - `POSTGRES_USER/PASSWORD/DB`: `keycloak`
  - Volumen exclusivo `keycloak-db-data:/var/lib/postgresql/data`
  - Red `votapp-network`, sin puertos host, healthcheck `pg_isready`
- [ ] 1.2 Añadir servicio `keycloak` con imagen `quay.io/keycloak/keycloak:24.0`.
  - Comando `start-dev --import-realm` para importar `votapp-realm`
  - Variables: `KEYCLOAK_ADMIN=admin`, `KEYCLOAK_ADMIN_PASSWORD=admin`, `KC_DB=postgres`,
    `KC_DB_URL=jdbc:postgresql://keycloak-db:5432/keycloak`, credenciales del DB
  - Puertos `8180:8080`, volumen `./docs/keycloak/import:/opt/keycloak/data/import:ro`
  - Depende de `keycloak-db` (`service_started`) y se une a `votapp-network`
- [ ] 1.3 Declarar volumen `keycloak-db-data` al lado de `postgres_data` y `redis_data`.
- [ ] 1.4 Ejecutar `docker compose config --quiet` y confirmar que `postgres` y `redis` siguen intactos (REQ-DOCKER-7).

## Phase 2: Realm JSON reproducible

- [ ] 2.1 Crear carpeta `docs/keycloak/import/` si no existe.
- [ ] 2.2 Añadir `votapp-realm.json` con metadata clave (`realm`, `enabled`, `sslRequired`, etc.).
- [ ] 2.3 Declarar cliente `votapp-backend` público con `redirectUris/webOrigins=*` y `directAccessGrantsEnabled`.
- [ ] 2.4 Crear tres mapeos de protocolo para `custom_documento_id`, `custom_numero_empleado` y `custom_depto_cod` usando `oidc-usermodel-attribute-mapper` y activando `access.token.claim`.
- [ ] 2.5 Añadir el usuario `funcionario_test` (`password=1234`, `temporary=false`) con atributos en formato array y habilitado.
- [ ] 2.6 Validar `votapp-realm.json` con `python3 -m json.tool`.

## Phase 3: Documentación operativa

- [ ] 3.1 Crear `docs/keycloak/TEST_GUIDE.md` y listar prerrequisitos (Docker Compose, `curl`, `jq` o Python).
- [ ] 3.2 Documentar Step 1: levantar `keycloak-db` + `keycloak`, seguir logs y confirmar admin UI en `http://localhost:8180/admin`.
- [ ] 3.3 Documentar Step 2: solicitar token con `curl` apuntando al endpoint `/protocol/openid-connect/token` y resaltar el `access_token` en la salida.
- [ ] 3.4 Documentar Step 3: decodificar JWT (macOS y Linux) y verificar los claims esperados.
- [ ] 3.5 Añadir sección de troubleshooting para errores comunes (puerto 8180, 401, reimportar realm).

## Phase 4: Verificación y estabilidad

- [ ] 4.1 Repetir `docker compose config --quiet` para confirmar YAML válido y servicios configurados.
- [ ] 4.2 Levantar `keycloak-db` y `keycloak`, esperar por log “Keycloak 24.0 ... started” y consultar `http://localhost:8180/realms/votapp-realm`.
- [ ] 4.3 Solicitar token de `funcionario_test` y verificar HTTP 200 + `access_token`.
- [ ] 4.4 Decodificar el JWT y confirmar `custom_documento_id`, `custom_numero_empleado`, `custom_depto_cod` con los valores definidos.
- [ ] 4.5 Volver a levantar `postgres` y `redis` para confirmar que los servicios existentes todavía arrancan.

## Phase 5: Seguimiento y registros

- [ ] 5.1 Marcar tareas completadas en este archivo a medida que se avancen las fases.
- [ ] 5.2 Registrar en Notion (Base de conocimientos) el resumen técnico, archivos afectados y el estado del cambio tras cada hito crítico.
