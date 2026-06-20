# Institutional Organization Specification

## Purpose
Manages the institutional structure, specifically departments and job positions (cargos).

## Requirements

### Requirement: Department Management
The system MUST allow creation, reading, updating, and deletion of departments.

#### Scenario: Create Department
- GIVEN a user with administrative privileges
- WHEN the user submits valid department details
- THEN the system MUST create the department and store it in the database.

### Requirement: Position (Cargo) Management
The system MUST link job positions to specific departments.

#### Scenario: Assign Position to Department
- GIVEN an existing department
- WHEN an admin creates a new position specifying the department ID
- THEN the position MUST be successfully linked to the department.