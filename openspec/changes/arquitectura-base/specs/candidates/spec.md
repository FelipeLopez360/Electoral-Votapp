# Candidates & Categories Specification

## Purpose
Administers electoral candidates and their respective categories.

## Requirements

### Requirement: Candidate Registration
The system MUST allow registering candidates and assigning them to specific categories within an election.

#### Scenario: Register Candidate
- GIVEN an active election setup
- WHEN an admin registers a candidate providing their details and category
- THEN the candidate MUST be saved and visible in the election's ballot.

### Requirement: Category Management
The system MUST allow creating categories for candidates to group them logically.

#### Scenario: Create Category
- GIVEN an admin managing an election
- WHEN the admin creates a "President" category
- THEN the system MUST store the category for later candidate assignment.