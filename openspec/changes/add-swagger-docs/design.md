# Design: Add Swagger / OpenAPI Documentation

## Technical Approach

Integrate `springdoc-openapi-starter-webmvc-ui:3.0.1` into the existing Spring Boot 4.0.1 / Spring MVC application to expose a self-hosted Swagger UI at `/swagger-ui.html` and an OpenAPI JSON spec at `/v3/api-docs`. SpringDoc auto-configures itself by introspecting `@RestController` beans at startup; the implementation adds a central `OpenApiConfig` configuration class, updates `SecurityConfig` to whitelist the docs paths, enables the endpoints via `application.yaml`, and decorates all five controllers with standard OpenAPI annotations (`@Tag`, `@Operation`, `@ApiResponse`).

No new infrastructure is required. No database changes. No logic changes to existing endpoints.

## Architecture Decisions

### Decision: springdoc-openapi-starter-webmvc-ui v3.x over v2.x

**Choice**: `org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.1`

**Alternatives considered**:
- `springdoc-openapi-ui:1.x` / `2.x` — the legacy artifact line
- `springdoc-openapi-starter-webflux-ui:3.0.1` — the WebFlux variant

**Rationale**:
- Spring Boot 4.x is built on Jakarta EE 10 (`jakarta.*` namespace). SpringDoc v3.x is the first release line that fully targets Jakarta EE 10; v2.x still uses `javax.*` and is incompatible with Spring Boot 4.
- The project uses `spring-boot-starter-webmvc` (not WebFlux), so the `webmvc-ui` artifact is the correct companion — it bundles `springdoc-openapi-starter-webmvc` plus the embedded Swagger UI static resources.
- Version `3.0.1` is the stable GA release aligned with the Spring Boot 4 / Spring Framework 7 baseline.

---

### Decision: OpenApiConfig placement in `common/config/`

**Choice**: New class `co.com.votapp.ws.common.config.OpenApiConfig`

**Alternatives considered**:
- Inline `@OpenAPIDefinition` on the main `@SpringBootApplication` class
- A dedicated `swagger/` sub-package

**Rationale**:
- The project already has a `common/config/` package containing `SecurityConfig.java`. Following this existing pattern keeps infrastructure configuration co-located and predictable.
- Annotating the main application class with `@OpenAPIDefinition` is a common but noisy pattern; a dedicated `@Configuration` class is cleaner and easier to remove during rollback.

---

### Decision: SecurityConfig whitelist strategy — explicit matchers before `anyRequest().permitAll()`

**Choice**: Add explicit `requestMatchers` for SpringDoc paths before the catch-all `.anyRequest().permitAll()`

**Alternatives considered**:
- Relying solely on `.anyRequest().permitAll()` to cover the docs paths (no explicit matchers)
- Moving docs paths behind authentication

**Rationale**:
- The existing `SecurityConfig` already uses `.anyRequest().permitAll()`, which technically covers the docs paths without changes. However, explicit matchers for `/swagger-ui/**`, `/swagger-ui.html`, and `/v3/api-docs/**` make the intent unambiguous to future maintainers and prevent accidental breakage if the catch-all is ever tightened.
- Keeping docs publicly accessible is intentional for developer experience. Production environments can override via `springdoc.swagger-ui.enabled=false` in a profile without touching `SecurityConfig`.

---

### Decision: Annotation placement — `@Tag` on class, `@Operation` + `@ApiResponse` on methods

**Choice**: Class-level `@Tag`, method-level `@Operation` and `@ApiResponse`

**Alternatives considered**:
- `@Tag` on each method individually (repetitive, verbose)
- No method-level annotations (relies on SpringDoc reflection defaults)

**Rationale**:
- `@Tag` on the class groups all methods under a single named section in Swagger UI — the standard and least repetitive approach.
- `@Operation` on each method provides a human-readable `summary` and `description`, which SpringDoc cannot infer from method names alone.
- `@ApiResponse` documents non-2xx responses (409 Conflict, 401 Unauthorized) that are not otherwise visible in the OpenAPI spec without explicit annotation.

## Data Flow

```
HTTP Client
    │
    ▼
GET /swagger-ui.html
    │
    ▼
SpringDoc StaticResource Handler
    │  (serves embedded Swagger UI HTML/JS/CSS)
    ▼
Browser loads Swagger UI
    │
    ▼
Swagger UI fetches GET /v3/api-docs
    │
    ▼
SpringDoc OpenApiWebMvcResource
    │  (introspects @RestController beans via Spring context)
    │  (merges @OpenAPIDefinition from OpenApiConfig)
    │  (resolves @Tag, @Operation, @ApiResponse from controllers)
    ▼
Returns OpenAPI 3.x JSON document
    │
    ▼
Swagger UI renders interactive documentation
```

Authentication flow (existing, unchanged):

```
HTTP Client
    │
    ▼
POST /api/v1/** (any protected endpoint)
    │
    ▼
SecurityFilterChain
    │  requestMatchers("/api/**") → authenticated()
    │  HTTP Basic credentials validated
    ▼
Controller → Service → Response
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `pom.xml` | Modify | Add `springdoc-openapi-starter-webmvc-ui:3.0.1` dependency |
| `src/main/resources/application.yaml` | Modify | Add `springdoc:` block to enable api-docs and swagger-ui |
| `src/main/java/co/com/votapp/ws/common/config/OpenApiConfig.java` | Create | `@OpenAPIDefinition` (title, description, version) + `@SecurityScheme` (basicAuth HTTP Basic) |
| `src/main/java/co/com/votapp/ws/common/config/SecurityConfig.java` | Modify | Add explicit permit matchers for `/swagger-ui/**`, `/swagger-ui.html`, `/v3/api-docs/**` |
| `src/main/java/co/com/votapp/ws/.../VoteController.java` | Modify | Add `@Tag`, `@Operation`, `@ApiResponse` |
| `src/main/java/co/com/votapp/ws/.../AuthController.java` | Modify | Add `@Tag`, `@Operation`, `@ApiResponse` |
| `src/main/java/co/com/votapp/ws/.../EleccionController.java` | Modify | Add `@Tag`, `@Operation`, `@ApiResponse` |
| `src/main/java/co/com/votapp/ws/.../CandidatesController.java` | Modify | Add `@Tag`, `@Operation`, `@ApiResponse` |
| `src/main/java/co/com/votapp/ws/.../OrganizationController.java` | Modify | Add `@Tag`, `@Operation`, `@ApiResponse` |

## Interfaces / Contracts

### pom.xml — dependency block

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>3.0.1</version>
</dependency>
```

---

### application.yaml — springdoc block

```yaml
springdoc:
  api-docs:
    enabled: true
    path: /v3/api-docs
  swagger-ui:
    enabled: true
    path: /swagger-ui.html
```

---

### OpenApiConfig.java — full class

```java
package co.com.votapp.ws.common.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Electoral Votapp API",
        description = "REST API for the Electoral Votapp voting platform",
        version = "1.0.0",
        contact = @Contact(
            name = "Votapp Team",
            email = "dev@votapp.com.co"
        )
    )
)
@SecurityScheme(
    name = "basicAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "basic",
    in = SecuritySchemeIn.HEADER,
    description = "HTTP Basic authentication. Required for all /api/** endpoints."
)
public class OpenApiConfig {
    // SpringDoc reads annotations; no bean definitions required
}
```

---

### SecurityConfig.java — updated requestMatchers

```java
// Before (existing):
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/**").authenticated()
    .anyRequest().permitAll()
)

// After (updated — explicit SpringDoc whitelist):
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/**").authenticated()
    .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
    .anyRequest().permitAll()
)
```

---

### VoteController — example annotation pattern (all controllers follow this)

```java
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/votes")
@Tag(name = "Votes", description = "Cast and manage votes")
public class VoteController {

    @PostMapping
    @Operation(
        summary = "Cast a vote",
        description = "Registers a vote for a candidate in a given election category. Each token can only be used once.",
        security = @SecurityRequirement(name = "basicAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Vote cast successfully"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "409", description = "Token already used for this election")
    })
    public ResponseEntity<Void> castVote(@RequestBody CastVoteRequest request) {
        // existing implementation unchanged
    }
}
```

**AuthController pattern**:
- `@Tag(name = "Authentication", description = "Login and session management")`
- `POST /api/v1/auth/login`: `@Operation(summary = "Login", ...)` with `@ApiResponse` 200, 401, 409

**EleccionController pattern**:
- `@Tag(name = "Elections", description = "Query active elections")`
- `GET /api/v1/elecciones/{codigo}`: `@Operation(summary = "Get election by code", ...)` with `@ApiResponse` 200, 401, 404

**CandidatesController pattern**:
- `@Tag(name = "Candidates", description = "Query candidates")`
- `GET /api/v1/candidatos`: `@Operation(summary = "List all candidates", ...)` with `@ApiResponse` 200, 401

**OrganizationController pattern**:
- `@Tag(name = "Organization", description = "Query organizational units")`
- `GET /api/v1/departamentos`: `@Operation(summary = "List all departments", ...)` with `@ApiResponse` 200, 401

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Smoke (manual) | `/swagger-ui.html` renders without credentials | Open browser, verify UI loads and shows all 5 tag groups |
| Smoke (manual) | `GET /v3/api-docs` returns valid OpenAPI JSON | `curl http://localhost:8080/v3/api-docs` — verify `openapi: 3.x` field present |
| Smoke (manual) | `basicAuth` scheme appears in Swagger UI Authorize dialog | Verify "Authorize" button is present in UI |
| Smoke (manual) | Existing `/api/**` endpoints still require HTTP Basic | `curl -X POST /api/v1/votes` without credentials → expect 401 |
| Existing tests | All current unit and integration tests still pass | Run `mvn test` — zero new test files needed; annotations are additive and do not alter behavior |

No automated tests are written for the documentation UI itself. The SpringDoc library is well-tested upstream; verifying that the dependency resolves and the UI loads is sufficient for this change.

## Migration / Rollout

No migration required. This change is purely additive:
- No database schema changes
- No existing endpoint behavior altered
- No new runtime dependencies beyond the SpringDoc JAR (included in the build)
- Rollback via `git revert` or by reverting the 5 file changes listed above (see proposal rollback plan)

## Open Questions

- [ ] Confirm the exact package path for each of the 5 controllers (the context shows the package root `co.com.votapp.ws` but not the sub-package — implementer should resolve before annotating)
- [ ] Decide whether to suppress SpringDoc endpoints in the `prod` Spring profile via `springdoc.swagger-ui.enabled=false` (low priority; out of scope per proposal but worth a team decision)
