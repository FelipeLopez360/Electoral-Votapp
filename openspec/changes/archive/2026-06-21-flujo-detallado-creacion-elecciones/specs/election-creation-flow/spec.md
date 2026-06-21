# Election Creation Flow Specification

## Purpose

Admin multi-step wizard for configuring elections and candidates with a final review step before publishing.

## Requirements

### Requirement: Multi-step Wizard Navigation

The system MUST provide a multi-step UI for creating elections, transitioning sequentially through Basic Info, Candidates, Ballot Config, and Review.

#### Scenario: Navigate through wizard steps
- GIVEN the admin is creating a new election
- WHEN they complete the required fields on a step and proceed
- THEN the system MUST persist the state locally and advance to the next step

#### Scenario: Maintain state across steps
- GIVEN the admin has progressed past the first step
- WHEN they navigate backwards to a previous step
- THEN the system MUST preserve their previously entered data

### Requirement: Rich Candidate Management

The system MUST allow admins to manage candidates with rich profile fields (photo, bio, proposals, affiliation).

#### Scenario: Add candidate with rich fields
- GIVEN the admin is on the Candidates step
- WHEN they add a new candidate providing photo URL, bio, proposals, and political affiliation
- THEN the system MUST store the candidate details in the wizard state

#### Scenario: Edit candidate details
- GIVEN a candidate has been added to the wizard state
- WHEN the admin edits the candidate's rich fields
- THEN the system MUST update the details before the final review

### Requirement: Ballot Configuration

The system MUST allow admins to configure ballot rules including `permiteVotoBlanco` and `maxVotosPorElector`.

#### Scenario: Configure ballot rules
- GIVEN the admin is on the Ballot Config step
- WHEN they enable `permiteVotoBlanco` and set `maxVotosPorElector` to a value greater than 0
- THEN the system MUST save these preferences in the wizard state

#### Scenario: Restrict invalid ballot config
- GIVEN the admin is on the Ballot Config step
- WHEN they attempt to set `maxVotosPorElector` to a value less than 1
- THEN the system MUST display a validation error and prevent progression

### Requirement: Final Review and Publish

The system MUST present a final review screen summarizing all configurations and candidates before submission.

#### Scenario: Review and confirm publication
- GIVEN the admin reaches the final Review step
- WHEN they confirm the data is correct and submit the election
- THEN the system MUST send the comprehensive payload to the backend
- AND navigate the admin to the election list upon success