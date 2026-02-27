# System Audit Specification

## Purpose
Provides centralized logging of critical system actions for auditing and security monitoring.

## Requirements

### Requirement: Log Critical Actions
The system MUST record critical administrative and system actions.

#### Scenario: Log Admin Login
- GIVEN an administrator logs into the system
- WHEN the login is successful
- THEN the audit system MUST record the event, timestamp, and admin ID in the `auditoria_sistema` log.

### Requirement: Immutability of Audit Logs
The system SHOULD NOT allow modification or deletion of existing audit logs through the standard application interfaces.

#### Scenario: Attempted Audit Log Tampering
- GIVEN an administrator with standard privileges
- WHEN they attempt to delete an entry from the audit log
- THEN the system MUST reject the request.