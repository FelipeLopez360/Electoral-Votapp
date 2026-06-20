# Architecture Specification

## Purpose
Defines the base architectural rules, structure, and infrastructure requirements for the Electoral Votapp system, ensuring modularity, scalability, and concurrency safety.

## Requirements

### Requirement: Monolithic Modular Structure
The system MUST be structured as a Monolithic application divided into independent modules using a "Package by Feature" approach.

#### Scenario: Module Isolation
- GIVEN the application is running
- WHEN a developer inspects the codebase
- THEN each module (auth, organization, electoral, candidates, voting, audit) MUST have its own independent package structure
- AND modules MUST NOT bypass defined interfaces to communicate with each other.

### Requirement: Hexagonal Architecture (Ports & Adapters)
Each module MUST implement Hexagonal Architecture to isolate domain logic from external concerns (frameworks, databases).

#### Scenario: Domain Independence
- GIVEN a module's core domain logic
- WHEN an external dependency (like the database or web framework) changes
- THEN the domain logic MUST NOT require modification
- AND communication MUST occur exclusively through defined input/output ports and adapters.

### Requirement: Redis Concurrency Control (SETNX)
The system MUST use Redis with the `SETNX` (Set if Not eXists) operation to guarantee atomicity and prevent race conditions during critical concurrent operations, such as vote casting and token validation.

#### Scenario: Concurrent Vote Casting
- GIVEN two concurrent requests attempting to cast a vote using the same valid token
- WHEN both requests reach the voting service simultaneously
- THEN Redis SETNX MUST allow only one request to acquire the lock/process the token
- AND the second request MUST be rejected or fail gracefully, ensuring the vote is only counted once.

### Requirement: Containerized Infrastructure
The system MUST use Docker Compose to orchestrate the backend, database, Redis, and frontend within a private, secure network.

#### Scenario: Secure Network Isolation
- GIVEN the Docker Compose environment is deployed
- WHEN an external entity attempts to directly access the PostgreSQL database or Redis cache
- THEN the connection MUST be refused
- AND these services MUST only be accessible by the backend container within the private Docker network.