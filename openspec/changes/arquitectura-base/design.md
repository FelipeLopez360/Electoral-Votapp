# Design: Arquitectura Base para el Sistema de Votaciones

## Technical Approach

The system will be built as a Spring Boot modular monolith using a "Package by Feature" organizational strategy. The core application logic will be separated into top-level domain packages (`auth`, `organization`, `electoral`, `candidates`, `voting`, `audit`). Within each domain package, we will strictly apply Hexagonal Architecture (Ports and Adapters), isolating the `domain` logic from the `application` (use cases) and `infrastructure` (REST APIs, DB, Redis) layers.
A Docker Compose infrastructure will be set up to provide a PostgreSQL database and a Redis instance, isolated within a private Docker network. 
For critical concurrency operations—specifically to guarantee atomic voting and prevent double-voting—we will use Redis with the `SETNX` (Set if Not eXists) command to manage voting token consumption.

## Architecture Decisions

### Decision: Monolithic Modular (Package by Feature)

**Choice**: Organize code by feature (domain modules) at the top-level packages, rather than grouping by technical layer (e.g., controllers, services, repositories).
**Alternatives considered**: Traditional N-Tier Layered Architecture or Microservices.
**Rationale**: Microservices introduce unnecessary operational overhead for the current phase, while traditional layers make it hard to understand features and enforce boundaries. Package by Feature provides the isolation benefits of microservices and the deployment simplicity of a monolith.

### Decision: Hexagonal Architecture (Ports & Adapters)

**Choice**: Strict separation of Domain, Application, and Infrastructure inside each feature module.
**Alternatives considered**: Standard Spring Layered architecture (Controller -> Service -> Repository) heavily coupled to Spring framework.
**Rationale**: Hexagonal architecture isolates core business rules from external technologies (Spring Data JPA, Web MVC, Redis). This ensures domain logic is testable without heavy framework contexts and protects the core from infrastructure changes.

### Decision: Redis SETNX for Atomic Token Validation

**Choice**: Use Redis `SETNX` (via Spring Data Redis `setIfAbsent`) to handle voting token consumption and concurrent vote locks.
**Alternatives considered**: PostgreSQL row-level locking or Java `synchronized` blocks.
**Rationale**: Given the high concurrency expected during election voting windows, Redis provides a highly performant, distributed atomic operation. `SETNX` guarantees that even if two identical requests arrive at the exact same millisecond, only one will successfully lock and use the token.

### Decision: Containerized Infrastructure with Docker Compose

**Choice**: Provide a `docker-compose.yml` to orchestrate `backend`, `postgres`, `redis`, and potentially a `frontend` service in a private network.
**Alternatives considered**: Local installations or direct cloud connections during development.
**Rationale**: Ensures complete consistency across all development environments, simplifies onboarding, and explicitly tests network bridge isolation to ensure DB and Redis are completely private and only accessible by the backend.

## Data Flow

Data flows from the external actors (Frontend) through the inbound adapters, interacts with the domain via application use cases, and reaches external persistence through outbound adapters.

    [Frontend/Client]
          │ (REST API via HTTP)
          ▼
    [Infrastructure: Inbound Adapter / REST Controller] ─┐
                                                         │ (Uses Inbound Port)
                                                         ▼
    [Application: Use Case / Service] ◄──────────── [Domain: Core Logic & Models]
          │ (Uses Outbound Port)
          ▼
    [Infrastructure: Outbound Adapter]
          ├──> [PostgreSQL DB] (Persistence via JPA/Hibernate)
          └──> [Redis] (Atomic Token Validation with SETNX)

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `docker-compose.yml` | Create | Orchestrates Postgres, Redis, Backend, and a private network. |
| `src/main/resources/application.yaml` | Modify | Add DB and Redis connection strings (using environment variables). |
| `src/main/java/co/com/votapp/ws/{domain}/*` | Create | Scaffold base packages (`auth`, `organization`, `electoral`, `candidates`, `voting`, `audit`) with `domain`, `application`, and `infrastructure` sub-packages. |
| `src/main/java/co/com/votapp/ws/voting/infrastructure/adapter/out/RedisTokenLockAdapter.java` | Create | Redis implementation of the token validation port using `SETNX`. |
| `src/main/java/co/com/votapp/ws/common/*` | Create | Shared kernel (e.g., domain exceptions, custom annotations like `@UseCase`). |
| `pom.xml` | Modify | Add dependencies for Spring Data Redis, Spring Data JPA, Postgres driver, and Testcontainers. |

## Interfaces / Contracts

### Token Validation Port (Voting Domain)
```java
package co.com.votapp.ws.voting.application.port.out;

public interface TokenLockPort {
    /**
     * Attempts to exclusively lock a token for voting.
     * Guaranteed atomic operation (SETNX).
     * 
     * @param tokenId The unique token identifier.
     * @return true if the lock was acquired (token unused), false if already locked/used.
     */
    boolean acquireTokenLock(String tokenId);
}
```

### Redis Adapter Implementation Concept
```java
package co.com.votapp.ws.voting.infrastructure.adapter.out;

import co.com.votapp.ws.voting.application.port.out.TokenLockPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import java.time.Duration;

@Component
public class RedisTokenLockAdapter implements TokenLockPort {
    private final StringRedisTemplate redisTemplate;

    public RedisTokenLockAdapter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean acquireTokenLock(String tokenId) {
        // Uses SETNX under the hood to ensure atomicity
        Boolean success = redisTemplate.opsForValue()
            .setIfAbsent("token_lock:" + tokenId, "locked", Duration.ofHours(24));
        return Boolean.TRUE.equals(success);
    }
}
```

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit | Domain logic (Entities, Value Objects), Use Cases | Plain JUnit 5 + Mockito. No Spring context to ensure speed and domain purity. |
| Integration | Outbound Adapters (JPA Repositories, Redis Adapters) | `@DataJpaTest` and `@SpringBootTest` with Testcontainers for PostgreSQL and Redis to verify actual DB queries and Redis SETNX behavior. |
| E2E | Full API endpoints (e.g., POST `/api/v1/votes`) | `@SpringBootTest` with `@AutoConfigureMockMvc` testing the entire HTTP -> Controller -> UseCase -> DB/Redis flow. |

## Migration / Rollout

No existing data migration required since this is the baseline architecture and initial schema setup. We will introduce a database migration tool (e.g., Flyway) as part of the setup tasks to manage future schema iterations reliably.

## Open Questions

- [ ] Which database migration tool (Flyway vs Liquibase) should we use for managing the PostgreSQL schema?
- [ ] What specific frontend technology stack will be used, and will it be served via Spring Boot or a separate Nginx container in Docker?
- [ ] How will voting tokens be distributed initially to guarantee security and anonymity?