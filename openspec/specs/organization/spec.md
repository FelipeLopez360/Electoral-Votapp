# Organization Specification

## Purpose

Organizational structure context for the Electoral Votapp, including departments, positions (cargos), and related lookups.

## Capabilities

### cargos-lookup

Soporte de solo lectura para listar cargos disponibles.

#### Requirement: List Cargos

The system MUST return all active cargos for use in form dropdowns.

##### Scenario: List all cargos

- GIVEN cargos exist in the database
- WHEN an admin requests GET /api/v1/organization/cargos
- THEN the system returns a list with id, codigo, nombre, nivelJerarquico

---

## Source of Truth

Last updated: 2026-06-08 via SDD change `gestion-funcionarios`
