# Delta for bulk-token-issuance and election-activation

## ADDED Requirements

### Requirement: Bulk Token Issuance on Activation
The system MUST automatically issue voting tokens in bulk when an election is activated. The system MUST NOT return the `rawToken` to the caller; it MUST discard it after hashing.

#### Scenario: Bulk issuance on manual activation (with census)
- GIVEN election is PROGRAMADA with 50 funcionarios in census
- WHEN admin activates election
- THEN election becomes ACTIVA 
- AND 50 tokens are issued 
- AND each token has status ISSUED

#### Scenario: Bulk issuance on manual activation (empty census)
- GIVEN election is PROGRAMADA with empty census (0 entries)
- WHEN admin activates election
- THEN election becomes ACTIVA 
- AND tokens are issued for ALL globally eligible funcionarios (ACTIVO + puede_votar=true)

#### Scenario: Bulk issuance on scheduled activation
- GIVEN election is PROGRAMADA with fecha_inicio <= now and has censo
- WHEN scheduler runs
- THEN election becomes ACTIVA 
- AND tokens are issued for census members

### Requirement: Token Idempotency
The system MUST ensure token issuance is idempotent. If a token already exists for a `funcionarioId` and `eleccionId`, the system MUST skip issuance for that funcionario without throwing an error.

#### Scenario: Partial existing tokens
- GIVEN election is being activated and some funcionarios already have ISSUED tokens
- WHEN bulk issuance runs
- THEN existing tokens are skipped
- AND new tokens are created for the rest

#### Scenario: Complete retry
- GIVEN activation+issuance succeeds but is retried (e.g., scheduler re-run)
- WHEN tokens already exist for all census members
- THEN 0 new tokens are created
- AND no error thrown

### Requirement: Transactional Atomicity
The system MUST execute election activation and bulk token issuance within the same database transaction.

#### Scenario: Rollback on token issuance failure
- GIVEN election is PROGRAMADA with census
- WHEN activation succeeds but token issuance fails mid-batch
- THEN entire transaction rolls back — election stays PROGRAMADA
- AND no partial tokens exist

#### Scenario: No tokens if activation fails
- GIVEN election activation itself fails (invalid state)
- THEN no tokens are issued

### Requirement: Eligibility Filtering at Activation Time
The system MUST filter census members based on their real-time eligibility at the moment of activation.

#### Scenario: Skips INACTIVO funcionarios
- GIVEN a funcionario is in the census but has estado_laboral=INACTIVO at activation time
- WHEN bulk issuance runs
- THEN that funcionario is skipped (no token issued)

#### Scenario: Skips puede_votar=false funcionarios
- GIVEN a funcionario is in the census but puede_votar=false
- WHEN bulk issuance runs
- THEN that funcionario is skipped

### Requirement: Token Properties
The system MUST generate tokens matching the established security standards.

#### Scenario: Token properties structure
- GIVEN a token is generated during bulk issuance
- THEN the token MUST have a unique UUID id
- AND the token MUST contain a SHA-256 hash of a random rawToken
- AND the token MUST have status=ISSUED and issued_at=now
- AND the rawToken MUST be generated but NOT stored or returned (discarded after hashing)
- AND the token MUST be linked to (eleccion_id, funcionario_id)

### Requirement: Result Reporting
The system MUST return a summary of the bulk issuance operation.

#### Scenario: Return BulkIssueResult
- GIVEN a bulk token issuance completes
- THEN it MUST return a BulkIssueResult
- AND it MUST contain `issued` (count of new tokens)
- AND it MUST contain `skipped` (already had token or ineligible)
- AND it MUST contain `total` (census size)

### Requirement: Error Handling
The system MUST reject activation and issuance for invalid states.

#### Scenario: Election not found
- GIVEN election is not found
- WHEN activation is attempted
- THEN an error is thrown

#### Scenario: Invalid state
- GIVEN election is not in valid pre-activation state (e.g., ACTIVA or FINALIZADA)
- WHEN activation is attempted
- THEN an error is thrown
- AND no tokens are issued