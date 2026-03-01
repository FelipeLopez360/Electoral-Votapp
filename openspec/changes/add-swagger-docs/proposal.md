# Proposal: Add Swagger / OpenAPI Documentation

## Intent

The Electoral Votapp REST API is currently undocumented. Developers and integrators have no interactive reference to discover endpoints, required parameters, or expected responses. This change adds SpringDoc OpenAPI support to expose a self-hosted Swagger UI at `/swagger-ui.html`, making the API explorable without reading source code. It also documents the HTTP Basic authentication scheme that already protects `/api/**` routes.

## Scope

### In Scope
- Add `org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.1` to `pom.xml`
- Enable SpringDoc endpoints via `application.yaml` (disabled by default in v3.x)
- Create `OpenApiConfig.java` — `@OpenAPIDefinition` with API title/description/version and `@SecurityScheme` for HTTP Basic
- Update `SecurityConfig.java` to whitelist `/swagger-ui/**`, `/swagger-ui.html`, and `/v3/api-docs/**` from HTTP Basic enforcement
- Annotate all 5 controllers with `@Tag` (controller-level grouping) and `@Operation`/`@ApiResponse` on each endpoint:
  - `VoteController`
  - `AuthController`
  - `EleccionController`
  - `CandidatesController`
  - `OrganizationController`
- Swagger UI accessible at `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON spec available at `http://localhost:8080/v3/api-docs`

### Out of Scope
- JWT / token-based auth migration (HTTP Basic remains unchanged)
- OpenAPI code generation (client SDKs, server stubs)
- Automated test coverage for documentation endpoints
- Versioning strategy for the API (v1, v2, etc.)
- Publishing docs to an external portal

## Approach

Standard SpringDoc integration for Spring Boot 4.x / Spring MVC:

1. **Dependency** — Add `springdoc-openapi-starter-webmvc-ui:3.0.1` to `pom.xml`. This is the correct artifact for `spring-boot-starter-webmvc` (not the WebFlux variant).
2. **YAML config** — Enable SpringDoc in `application.yaml` because v3.x disables `api-docs` and `swagger-ui` by default:
   ```yaml
   springdoc:
     api-docs:
       enabled: true
     swagger-ui:
       enabled: true
       path: /swagger-ui.html
   ```
3. **OpenApiConfig bean** — A new `@Configuration` class annotated with `@OpenAPIDefinition` sets the global API info (title, description, version, contact). A `@SecurityScheme` annotation declares the `basicAuth` HTTP Basic scheme so endpoints can reference it.
4. **SecurityConfig update** — Add the SpringDoc paths to the permit-all list so the docs UI is publicly accessible without credentials.
5. **Controller annotations** — Each controller gets `@Tag(name=..., description=...)`. Each handler method gets `@Operation(summary=..., description=...)` and relevant `@ApiResponse` entries (200, 400, 401, 404 where applicable).

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `pom.xml` | Modified | Add `springdoc-openapi-starter-webmvc-ui:3.0.1` dependency |
| `src/main/resources/application.yaml` | Modified | Enable SpringDoc api-docs and swagger-ui |
| `src/main/java/co/com/votapp/ws/common/config/SecurityConfig.java` | Modified | Whitelist `/swagger-ui/**`, `/swagger-ui.html`, `/v3/api-docs/**` |
| `src/main/java/co/com/votapp/ws/common/config/OpenApiConfig.java` | New | `@OpenAPIDefinition` + `@SecurityScheme(basicAuth)` |
| `VoteController` | Modified | Add `@Tag`, `@Operation`, `@ApiResponse` |
| `AuthController` | Modified | Add `@Tag`, `@Operation`, `@ApiResponse` |
| `EleccionController` | Modified | Add `@Tag`, `@Operation`, `@ApiResponse` |
| `CandidatesController` | Modified | Add `@Tag`, `@Operation`, `@ApiResponse` |
| `OrganizationController` | Modified | Add `@Tag`, `@Operation`, `@ApiResponse` |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| SpringDoc path conflicts with existing endpoints | Low | Paths `/v3/api-docs` and `/swagger-ui` are reserved by SpringDoc; verify no custom routes overlap |
| Security regression — docs UI exposed in production | Low | Whitelist is explicit; production profile can override `springdoc.swagger-ui.enabled=false` |
| SpringDoc incompatibility with Spring Boot 4.0.1 | Low | `springdoc-openapi-starter-webmvc-ui:3.0.1` targets Spring Boot 3.x/4.x; verify on first build |
| Annotation verbosity increases controller size | Low | Annotations are additive; no logic changes, existing behavior unaffected |

## Rollback Plan

1. Remove the `springdoc-openapi-starter-webmvc-ui` dependency block from `pom.xml`
2. Revert the `springdoc:` block in `application.yaml`
3. Remove `OpenApiConfig.java`
4. Revert `SecurityConfig.java` to its previous permit-all list
5. Remove `@Tag`, `@Operation`, and `@ApiResponse` annotations from controllers (optional — harmless if left, but clean to remove)

No database migrations are involved. Rollback is fully reversible via Git revert.

## Dependencies

- `org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.1` (new Maven dependency)
- Spring Boot 4.0.1 already present — no version upgrade needed
- `spring-boot-starter-webmvc` already present — correct companion for the chosen SpringDoc artifact

## Success Criteria

- [ ] Application starts without errors after adding the SpringDoc dependency
- [ ] `GET /v3/api-docs` returns a valid OpenAPI JSON document listing all 5 controller groups
- [ ] `GET /swagger-ui.html` renders the Swagger UI without requiring HTTP Basic credentials
- [ ] All 5 controllers appear as named groups (`@Tag`) in the Swagger UI
- [ ] Each endpoint displays its summary, description, and documented response codes
- [ ] The `basicAuth` security scheme is visible in the Swagger UI "Authorize" dialog
- [ ] Existing authenticated endpoints (`/api/**`) continue to require HTTP Basic — no security regression
