# Tasks: Arquitectura Base para el Sistema de Votaciones

## Phase 1: Foundation & Infrastructure
- [x] 1.1 Modify `pom.xml` to include `spring-boot-starter-data-redis`, `spring-boot-starter-data-jpa`, `postgresql`, `flyway-core`, and `testcontainers`.
- [x] 1.2 Create `docker-compose.yml` to define `postgres` and `redis` services within a private network `votapp-network`.
- [x] 1.3 Update `src/main/resources/application.yaml` with DB and Redis connection strings, using environment variables and enabling Flyway.
- [x] 1.4 Create `src/main/resources/db/migration/V1__Initial_schema.sql` containing the DDL for all 6 domains (auth, organization, electoral, candidates, voting, audit).

## Phase 2: Shared Kernel
- [x] 2.1 Create `src/main/java/co/com/votapp/ws/common/annotation/UseCase.java` (custom stereotype annotation).
- [x] 2.2 Create `src/main/java/co/com/votapp/ws/common/exception/DomainException.java` to handle core business logic errors.

## Phase 3: Voting Module (Concurrency & Redis Highlight)
- [x] 3.1 Create domain models `Token.java` and `Vote.java` in `src/main/java/co/com/votapp/ws/voting/domain/`.
- [x] 3.2 Create port `TokenLockPort.java` in `src/main/java/co/com/votapp/ws/voting/application/port/out/`.
- [x] 3.3 Create `RedisTokenLockAdapter.java` in `src/main/java/co/com/votapp/ws/voting/infrastructure/adapter/out/` implementing `TokenLockPort` using Redis `SETNX`.
- [x] 3.4 Create JPA entity `VoteEntity.java` and repository `VoteRepository.java` in `src/main/java/co/com/votapp/ws/voting/infrastructure/adapter/out/persistence/`.
- [x] 3.5 Create use case `CastVoteUseCase.java` in `src/main/java/co/com/votapp/ws/voting/application/usecase/` that invokes `TokenLockPort`.
- [x] 3.6 Create `VoteController.java` in `src/main/java/co/com/votapp/ws/voting/infrastructure/adapter/in/web/` exposing `POST /api/v1/votes`.
- [x] 3.7 Write `RedisTokenLockAdapterTest.java` in `src/test/java/co/com/votapp/ws/voting/infrastructure/adapter/out/` using Testcontainers to verify atomic token locking.

## Phase 4: Core Modules Implementation (Auth, Electoral, Audit)
- [ ] 4.1 Auth Module: Create `Admin.java`, `LoginUseCase.java`, `AdminRepositoryPort.java`, `JpaAdminAdapter.java` (`AdminEntity.java`), and `AuthController.java` (`/api/v1/auth/login`).
- [ ] 4.2 Electoral Module: Create `Election.java`, `CreateElectionUseCase.java`, `ElectionRepositoryPort.java`, `JpaElectionAdapter.java` (`ElectionEntity.java`), and `ElectionController.java` (`/api/v1/elections`).
- [ ] 4.3 Audit Module: Create `AuditLog.java`, `LogActionUseCase.java`, `AuditRepositoryPort.java`, `JpaAuditAdapter.java` (`AuditLogEntity.java`).

## Phase 5: Module Scaffolding (Organization & Candidates)
- [ ] 5.1 Organization Module: Scaffold `Department.java`, `Position.java`, `JpaOrganizationAdapter.java` (`DepartmentEntity.java`, `PositionEntity.java`), and `OrganizationController.java`.
- [ ] 5.2 Candidates Module: Scaffold `Candidate.java`, `Category.java`, `JpaCandidateAdapter.java` (`CandidateEntity.java`, `CategoryEntity.java`), and `CandidateController.java`.

## Phase 6: E2E Integration & Verification
- [x] 6.1 Update `src/main/java/co/com/votapp/ws/VotappApplication.java` (if necessary) to ensure correct component scanning for `@UseCase`.
- [x] 6.2 Write `VoteControllerE2ETest.java` in `src/test/java/co/com/votapp/ws/voting/infrastructure/adapter/in/web/` testing the full HTTP -> Redis -> DB flow.
- [x] 6.3 Write `LoginUseCaseTest.java` in `src/test/java/co/com/votapp/ws/auth/application/usecase/` verifying valid/invalid credentials flow.
