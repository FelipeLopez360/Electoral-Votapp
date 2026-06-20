# Tasks: Add Swagger / OpenAPI Documentation

## Phase 1: Infrastructure — Dependency & Configuration

- [x] 1.1 Add `springdoc-openapi-starter-webmvc-ui:3.0.1` dependency to `pom.xml` inside the `<dependencies>` block (groupId: `org.springdoc`, artifactId: `springdoc-openapi-starter-webmvc-ui`, version: `3.0.1`, no scope override needed)
- [x] 1.2 Append the `springdoc:` YAML block to `src/main/resources/application.yaml` enabling both `api-docs` (path: `/v3/api-docs`) and `swagger-ui` (path: `/swagger-ui.html`, enabled: true) — required because SpringDoc v3.x disables both endpoints by default
- [x] 1.3 Run `mvn dependency:resolve` (or `mvn compile`) to verify the dependency resolves without version conflicts and the project still compiles

## Phase 2: Core Configuration

- [x] 2.1 Create `src/main/java/co/com/votapp/ws/common/config/OpenApiConfig.java` — `@Configuration` class annotated with `@OpenAPIDefinition` (title: "Electoral Votapp API", description, version: "1.0.0", contact) and `@SecurityScheme(name="basicAuth", type=HTTP, scheme="basic", in=HEADER)`
- [x] 2.2 Modify `src/main/java/co/com/votapp/ws/common/config/SecurityConfig.java` — add explicit `requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()` before the existing `.anyRequest().permitAll()` rule; ensure the `/api/**` authenticated rule is not disturbed

## Phase 3: Controller Annotations

- [x] 3.1 Annotate `src/main/java/co/com/votapp/ws/voting/infrastructure/adapter/in/web/VoteController.java` — add `@Tag(name="Votes", description="Cast and manage votes")` at class level; add `@Operation(summary=..., description=..., security=@SecurityRequirement(name="basicAuth"))` and `@ApiResponses({@ApiResponse(201), @ApiResponse(400), @ApiResponse(401), @ApiResponse(409)})` on the `castVote` handler method
- [x] 3.2 Annotate `src/main/java/co/com/votapp/ws/auth/infrastructure/adapter/in/web/AuthController.java` — add `@Tag(name="Authentication", description="Login and session management")` at class level; add `@Operation` and `@ApiResponses` (200, 401, 409) on the login handler method with `@SecurityRequirement(name="basicAuth")`
- [x] 3.3 Annotate `src/main/java/co/com/votapp/ws/electoral/infrastructure/adapter/in/web/EleccionController.java` — add `@Tag(name="Elections", description="Query active elections")` at class level; for each handler method add `@Operation(summary=..., description=...)` and `@ApiResponses` including 200, 401, and 404 where the method looks up by identifier, with `@SecurityRequirement(name="basicAuth")`
- [x] 3.4 Annotate `src/main/java/co/com/votapp/ws/candidates/infrastructure/adapter/in/web/CandidatesController.java` — add `@Tag(name="Candidates", description="Query candidates")` at class level; for each handler method add `@Operation` and `@ApiResponses` (200, 401) with `@SecurityRequirement(name="basicAuth")`
- [x] 3.5 Annotate `src/main/java/co/com/votapp/ws/organization/infrastructure/adapter/in/web/OrganizationController.java` — add `@Tag(name="Organizations", description="Query organizational units")` at class level; for each handler method add `@Operation` and `@ApiResponses` (200, 401) with `@SecurityRequirement(name="basicAuth")`

## Phase 4: Verification

- [x] 4.1 Run `mvn test` and confirm all existing tests pass — zero new test failures expected since annotations are additive and do not alter business logic
- [ ] 4.2 Start the application (`mvn spring-boot:run`) and verify `GET /swagger-ui.html` returns HTTP 200 (or a 3xx redirect that resolves to the Swagger UI) without any `Authorization` header — manual smoke test for REQ-2
- [ ] 4.3 Verify `GET /v3/api-docs` returns HTTP 200 with `Content-Type: application/json` and a body containing top-level keys `openapi`, `info`, and `paths` — manual smoke test for REQ-1; can use `curl http://localhost:8080/v3/api-docs | python3 -m json.tool`
- [ ] 4.4 Verify the Swagger UI "Authorize" dialog lists a `basicAuth` entry of type `http (Basic)` — manual smoke test for REQ-3; open browser, click "Authorize" button
- [ ] 4.5 Verify all 5 tag groups (`Votes`, `Authentication`, `Elections`, `Candidates`, `Organizations`) appear as collapsed sections in the Swagger UI — manual smoke test for REQ-4
- [ ] 4.6 Verify security boundary is intact: send `curl -X POST http://localhost:8080/api/v1/votes` with no `Authorization` header and confirm the response is `401 Unauthorized` — manual regression check for REQ-7
