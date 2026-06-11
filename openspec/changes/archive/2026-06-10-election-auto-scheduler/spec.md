# Election Auto Scheduler Specification

## Purpose

Automate the state transitions of elections (`PROGRAMADA → ACTIVA → FINALIZADA`) based on their configured `fechaInicio` and `fechaFin` dates, replacing the need for manual administrative triggers.

## Requirements

### Requirement: Auto-Activate Elections

The system MUST automatically transition elections from `PROGRAMADA` to `ACTIVA` state when their `fechaInicio` has been reached or passed. The system MUST also generate the default blank vote candidate during this transition.

#### Scenario: Auto-activate election when fechaInicio arrives

- GIVEN an election in PROGRAMADA status
- AND its fechaInicio has passed
- WHEN the scheduler runs
- THEN the election becomes ACTIVA
- AND a blank vote candidate is created

#### Scenario: Scheduler does not activate future elections

- GIVEN an election in PROGRAMADA status
- AND its fechaInicio is in the future
- WHEN the scheduler runs
- THEN the election remains PROGRAMADA

### Requirement: Auto-Finalize Elections

The system MUST automatically transition elections from `ACTIVA` to `FINALIZADA` state when their `fechaFin` has been reached or passed.

#### Scenario: Auto-finalize election when fechaFin arrives

- GIVEN an election in ACTIVA status
- AND its fechaFin has passed
- WHEN the scheduler runs
- THEN the election becomes FINALIZADA

#### Scenario: Scheduler does not finalize active elections before fechaFin

- GIVEN an election in ACTIVA status
- AND its fechaFin is in the future
- WHEN the scheduler runs
- THEN the election remains ACTIVA

### Requirement: Fault Isolation

The system MUST process each election independently during the scheduled execution. If one election fails to transition, it MUST NOT prevent other eligible elections from transitioning.

#### Scenario: One failing election does not block others

- GIVEN two PROGRAMADA elections with fechaInicio passed
- AND one has an invalid state
- WHEN the scheduler runs
- THEN the valid election is activated
- AND the invalid one is skipped with an error logged

### Requirement: Manual Override Preservation

The system MUST continue to support manual activation and finalization via existing endpoints independently of the scheduler.

#### Scenario: Manual activate still works alongside scheduler

- GIVEN an election in PROGRAMADA status
- WHEN admin calls POST /api/v1/elections/{id}/activate
- THEN the election is activated immediately (not waiting for scheduler)

## Non-Functional Requirements

### Requirement: Polling Frequency
The scheduler SHOULD poll for eligible elections every 60 seconds.

### Requirement: Atomicity
The system MUST perform all database writes for a single election transition (e.g., updating election state and creating a blank candidate) atomically.

### Requirement: Pure Domain Layer
The system MUST maintain the purity of the domain layer, ensuring zero Spring annotations or dependencies are introduced within the domain packages.

### Requirement: Timezone Consistency
The system MUST use UTC consistently for all date and time evaluations during the scheduling process.

### Requirement: Inclusive Date Boundary
The system MUST use inclusive comparison (`<= now`) when evaluating whether an election's `fechaInicio` or `fechaFin` has been "reached or passed". An election with `fechaInicio == now` MUST be activated on that tick, not deferred to the next.
