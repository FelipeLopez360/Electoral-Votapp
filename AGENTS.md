# AGENTS.md — Electoral-Votapp

Agent-oriented documentation for this codebase. All agents, subagents, and orchestrators working in this repo MUST read this file before writing any code.

## Project

Electoral voting system MVP. Token-only voting flow (no user authentication in MVP). Java 21 + Spring Boot 4.0.1 + PostgreSQL + Redis.

## Mandatory Skills by Context

These skills MUST be loaded before any task that touches the listed file patterns. No exceptions.

| File pattern | Load these skills FIRST |
|---|---|
| `*.java` / `src/main/java/**` / `src/test/java/**` | `spring-boot-hexagonal` + `junit-mockito` |
| `src/main/resources/db/migration/**` | `spring-boot-hexagonal` |
| `docker-compose.yml` / `Dockerfile` | `docker-expert` |
| Any PR creation or review | `branch-pr` or `code-review` |

Skill paths are in `.atl/skill-registry.md`.

## Architecture: Hexagonal (Ports & Adapters) — Non-Negotiable

Package structure per `spring-boot-hexagonal` skill. Any deviation is a bug.

```
co.com.votapp.ws.{context}/
├── domain/                  # Pure Java — ZERO Spring/JPA imports
│   ├── model/               # Records, value objects, enums
│   ├── port/
│   │   ├── in/              # Input ports (use case interfaces)
│   │   └── out/             # Output ports (repository/service interfaces)
│   ├── usecase/             # Use case implementations (implement input ports)
│   └── exception/           # Domain exceptions only
├── application/             # Spring OK here — orchestration, DTOs
│   ├── service/             # Application services (@Service allowed)
│   └── dto/                 # Records, self-validating
├── infrastructure/
│   └── adapter/
│       ├── in/web/          # REST controllers (@RestController)
│       └── out/
│           ├── persistence/ # JPA entities + repositories + adapters
│           └── redis/       # Redis adapters
└── config/                  # ALL @Configuration classes — never in adapter/
```

**The one law that cannot be broken**: `domain/` has ZERO `import org.springframework.*` or `import jakarta.persistence.*`. Ever.

## Bounded Contexts (MVP)

| Context | Package suffix | Responsibility |
|---|---|---|
| Electoral Setup | `.electoral` | Election lifecycle, candidates, ballot |
| Voter Eligibility | `.votereligibility` | Funcionario eligibility checks |
| Voting Core | `.voting` | Token issuance, validation, atomic cast |
| Audit | `.audit` | Critical event logging |

## Key Domain Decisions

- **Token model**: `tokenHash` SHA-256 (persisted in DB) → `tokenId` UUID (resolved from portal session). The rawToken manual-issuance flow has been removed. Tokens are bulk-issued automatically on election activation.
- **Anonymity**: `votos` table NEVER stores `funcionario_id`. Participation tracked separately in `participacion_electoral`.
- **Blank vote**: synthetic `Candidato` with `es_voto_en_blanco=true`, created automatically when an election is activated. Never managed manually.
- **CastVote atomicity**: Redis SETNX lock → DB transaction (revalidate by tokenId + markUsed + insert voto + mark participación + audit) → release lock. Portal flow uses `CastVoteByTokenIdPort` (no rawToken in flight).
- **Audit**: synchronous port call from use case. Domain does not know about audit.
- **Schema**: V1 is the canonical MVP schema. No V2 until production data exists.

## Testing Conventions

- `*Test.java` → unit test (JUnit 5 + Mockito, no Spring context)
- `*IT.java` → integration test (Testcontainers, `@SpringBootTest`)
- Run unit tests: `./mvnw test`
- Run all (including IT): `./mvnw verify`
- **Strict TDD active**: RED → GREEN → TRIANGULATE → REFACTOR. No production code before a failing test.
- Domain unit tests: mock output ports only, never implementations
- Integration tests: real PostgreSQL + Redis via Testcontainers

## What NOT to Do

- `@Service` or `@Component` on domain use cases → wire manually via `DomainConfig`
- `@Autowired` field injection anywhere → constructor injection only
- JPA `@Entity` in `domain/` → entities live in `adapter/out/persistence/`
- `rawToken` in logs, DB, or any persistence layer — the manual token-issuance flow has been removed
- `funcionario_id` in `votos` table
- `@Configuration` inside `adapter/` packages → always in `config/`
- Manually issuing single tokens via `IssueVotingTokenUseCase` — that flow is deleted; bulk issuance on activation is the only mechanism
- Accepting votes via `POST /api/v1/votes` or ballot loading via `GET /api/v1/elections/{id}/ballot` — those endpoints are removed; portal flow (`/api/v1/portal/**`) is canonical

## SDD Context

- Change in progress: `remove-admin-legacy-token-vote`
- Artifacts in Engram under project `electoral-votapp`
- Topic keys: `sdd/remove-admin-legacy-token-vote/{spec,design,tasks,apply-progress}`
- Chain strategy: two separate PRs (one per repo, frontend-first)
- PR-FE: frontend legacy deletion — COMPLETE
- PR-BE: backend legacy deletion — this change
