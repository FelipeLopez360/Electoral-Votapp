# Delta for Electoral

## ADDED Requirements

### Requirement: Election Ballot Configuration

The system MUST support election configuration properties for `permiteVotoBlanco` and `maxVotosPorElector` via API contracts and persistence schema.

#### Scenario: Create election with ballot config
- GIVEN a valid request payload containing `permiteVotoBlanco` and `maxVotosPorElector`
- WHEN the election is created via the backend API
- THEN the system MUST persist these fields accurately in the database

#### Scenario: Fetch election with ballot config
- GIVEN an existing election with custom ballot configuration
- WHEN the election details are requested via the API
- THEN the system MUST return the `permiteVotoBlanco` and `maxVotosPorElector` fields

### Requirement: Rich Candidate Profiles

The system MUST support rich candidate profiles including `fotoUrl`, `biografia`, `propuestas`, and `afiliacionPolitica` via API contracts and persistence schema.

#### Scenario: Create candidate with rich profile
- GIVEN a valid request payload containing rich candidate fields
- WHEN the candidate is created via the backend API
- THEN the system MUST persist the `fotoUrl`, `biografia`, `propuestas`, and `afiliacionPolitica` in the database

#### Scenario: Fetch candidate with rich profile
- GIVEN an existing candidate with a rich profile
- WHEN the candidate details are requested via the API
- THEN the system MUST return all rich profile fields

### Requirement: Automatic Blank Vote Configuration

The system MUST configure the synthetic blank vote candidate appropriately upon election activation, respecting the `permiteVotoBlanco` flag.

#### Scenario: Activate election with blank vote permitted
- GIVEN an election configured with `permiteVotoBlanco=true` in `PROGRAMADA` state
- WHEN the election is activated
- THEN the system MUST create the synthetic "Voto en Blanco" candidate

#### Scenario: Activate election with blank vote disabled
- GIVEN an election configured with `permiteVotoBlanco=false` in `PROGRAMADA` state
- WHEN the election is activated
- THEN the system MUST NOT create the synthetic "Voto en Blanco" candidate