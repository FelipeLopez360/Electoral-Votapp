# Auth Specification

## Purpose

Authentication and authorization context for the Electoral Votapp, including funcionario management.

## Capabilities

### funcionarios-management

Administración (CRUD) de funcionarios desde el panel de admin, incluyendo generación de contraseña temporal y control de elegibilidad.

#### Requirement: List Funcionarios

The system MUST allow an admin to list all funcionarios with optional search/filter by nombre, documento, departamento, and estado_laboral.

##### Scenario: List all funcionarios

- GIVEN there are funcionarios in the system
- WHEN an admin requests GET /api/v1/funcionarios
- THEN the system returns a paginated/simple list with: id, numeroEmpleado, documentoIdentidad, nombres, apellidos, email, departamento, cargo, estadoLaboral, puedeVotar, debeCambiarPassword

##### Scenario: Filter by search term

- GIVEN funcionarios exist with various names
- WHEN an admin requests GET /api/v1/funcionarios?search=Juan
- THEN the system returns funcionarios matching by nombres or apellidos or documentoIdentidad

#### Requirement: Get Funcionario Detail

The system MUST return full details of a single funcionario including eligibility status.

##### Scenario: Get existing funcionario

- GIVEN a funcionario with id=1 exists
- WHEN an admin requests GET /api/v1/funcionarios/1
- THEN the system returns the full funcionario detail

##### Scenario: Get non-existent funcionario

- GIVEN funcionario id=999 does not exist
- WHEN an admin requests GET /api/v1/funcionarios/999
- THEN the system returns 404

#### Requirement: Create Funcionario

The system MUST create a new funcionario with auto-generated temporary password.

##### Scenario: Create successfully

- GIVEN valid funcionario data (nombres, apellidos, documentoIdentidad, email, departamentoId, cargoId, estadoLaboral, puedeVotar)
- WHEN an admin creates a funcionario via POST /api/v1/funcionarios
- THEN the system:
  - Generates a random temporary password
  - Hashes it with BCrypt
  - Stores the funcionario with debeCambiarPassword=true
  - Returns the created funcionario (without password)
  - Returns status 201

##### Scenario: Missing required fields

- GIVEN incomplete data (missing nombres or documentoIdentidad)
- WHEN the admin tries to create
- THEN the system returns 400

##### Scenario: Duplicate documentoIdentidad

- GIVEN a funcionario already exists with documentoIdentidad=X
- WHEN an admin tries to create another with the same documento
- THEN the system returns 409

#### Requirement: Update Funcionario

The system MUST allow updating a funcionario's editable fields.

##### Scenario: Update successfully

- GIVEN a funcionario exists
- WHEN an admin updates fields (nombres, email, estadoLaboral, puedeVotar, departamentoId, cargoId) via PUT /api/v1/funcionarios/{id}
- THEN the system updates and returns the funcionario

##### Scenario: Toggle puede_votar

- GIVEN a funcionario with puedeVotar=false
- WHEN an admin sets puedeVotar=true via PUT
- THEN the system updates and returns the funcionario with puedeVotar=true

##### Scenario: Update non-existent

- GIVEN funcionario id=999 does not exist
- WHEN an admin tries to update
- THEN the system returns 404

#### Requirement: Password change tracking

The system MUST track whether a funcionario still needs to change their password.

##### Scenario: New funcionario needs password change

- GIVEN a newly created funcionario
- THEN debeCambiarPassword is true

##### Scenario: Admin notes password was changed

- GIVEN a funcionario with debeCambiarPassword=true
- WHEN an admin sets debeCambiarPassword=false via PUT
- THEN the system updates the flag

---

## Source of Truth

Last updated: 2026-06-08 via SDD change `gestion-funcionarios`
