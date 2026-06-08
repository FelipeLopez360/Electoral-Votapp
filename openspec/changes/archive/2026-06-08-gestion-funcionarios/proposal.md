# Proposal: Gestión de Funcionarios

## Intent
El panel de administración necesita una sección para gestionar funcionarios (ver, crear, editar, alternar elegibilidad de voto, generar contraseñas temporales) para operar el sistema electoral de manera integral.

## Scope

### In Scope
- **Backend**: Endpoints CRUD en `/api/v1/funcionarios`
  - `GET /funcionarios` — listado con búsqueda/filtro (nombre, documento, departamento, estado_laboral).
  - `GET /funcionarios/{id}` — vista de detalle (incluye estado de elegibilidad e info de participación).
  - `POST /funcionarios` — creación con contraseña temporal generada automáticamente.
  - `PUT /funcionarios/{id}` — actualización de campos, toggle de `puede_votar` y `estado_laboral`.
- **Backend**: Expandir la entidad `FuncionarioEntity` y el modelo de dominio `Funcionario.java` con los campos de la BD (`nombres`, `apellidos`, `tipo_documento`, `telefono`, `departamento_id`, `cargo_id`, `fecha_ingreso`) y la bandera `debeCambiarPassword`.
- **Backend**: Soporte de lectura en runtime para `Cargo` (entidad + repositorio + adapter + endpoint `GET /cargos` para lookups).
- **Frontend**: Nueva página `/funcionarios` con tabla, filtros, formularios de creación/edición (con selectores de departamento y cargo), toggle de acción rápida para `puede_votar` y badge de `estado_laboral`.
- **Frontend**: Entrada en la navegación principal ("Funcionarios") y ruta en `App.tsx`.

### Out of Scope
- Flujo real de login de funcionario (el cambio de contraseña en primer login es post-MVP, pero la bandera y generación sí entran en este scope).
- Mejoras en el algoritmo de hashing de contraseñas (se usará el patrón placeholder actual).
- CRUD completo de `Cargo` (solo lectura).

## Capabilities

### New Capabilities
- `funcionarios-management`: Administración (CRUD) de funcionarios desde el panel de admin, incluyendo generación de contraseña temporal y control de elegibilidad.
- `cargos-lookup`: Soporte de solo lectura para listar cargos disponibles.

### Modified Capabilities
- `voter-eligibility`: El repositorio que verifica la elegibilidad debe seguir funcionando y posiblemente adaptarse al nuevo modelo expandido si hay impactos a nivel base de datos.

## Approach
- **Backend**: Extender el módulo `auth` para alojar la gestión de funcionarios, ya que este módulo contiene actualmente el dominio mínimo de `Funcionario`.
- **Backend**: Añadir la lógica de `cargos` al módulo `organization` (junto a los departamentos existentes, usando el mismo patrón).
- **Frontend**: Seguir el mismo patrón de `TokensPage`/`ElectionsPage` (React + TypeScript, peticiones fetch con Auth Header).
- **Arquitectura**: Respetar estrictamente la arquitectura hexagonal (Cero dependencias de Spring o JPA en `domain/`).

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `co.com.votapp.ws.auth` | Modified | Ampliación de dominio, entidad y creación de puertos/controladores de Funcionario. |
| `co.com.votapp.ws.organization` | Modified | Nuevo sub-dominio y endpoint para consulta de Cargos. |
| `VoterEligibilityRepositoryAdapter` | Modified | Validación de que la ampliación del repositorio de funcionario no rompa la adaptación actual de elegibilidad. |
| `frontend/src/pages/FuncionariosPage.tsx` | New | UI para gestión y lista de funcionarios. |
| `frontend/src/Layout.tsx` & `App.tsx` | Modified | Nuevas rutas y entradas de menú. |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| `password_hash` es NOT NULL en esquema | Alto | La generación de contraseña temporal debe asegurar que se produce un hash dummy/válido compatible con la tabla en la BD. |
| `AuthenticateFuncionarioUseCase` ignora `rawPassword` (deuda técnica) | Alto | Mantenerlo fuera del alcance tal cual; sin embargo, al crear funcionarios asegurar que el flujo no bloquee el login base actual. |
| Rotura en dependencias del adaptador `VoterEligibilityRepositoryAdapter` | Medio | Asegurar mediante test de integración que las consultas de elegibilidad a `FuncionarioJpaRepository` continúan estables. |

## Rollback Plan
- Backend: Revertir los commits de las carpetas/paquetes `auth` y `organization` y restaurar los DTOs al estado base.
- Frontend: Revertir los cambios en `App.tsx` / `Layout.tsx` y eliminar `FuncionariosPage.tsx`.

## Dependencies
- El modelo actual de DB (`votos`, `funcionarios`, `cargos`, `departamentos`).

## Success Criteria
- [ ] Un usuario administrador puede ver, buscar y filtrar funcionarios en el frontend.
- [ ] El administrador puede crear un nuevo funcionario con la auto-asignación de una contraseña temporal (`debeCambiarPassword = true`).
- [ ] El administrador puede alternar la bandera `puede_votar` y editar el `estado_laboral`.
- [ ] Los selectores de Cargo y Departamento en el frontend cargan opciones reales desde la API.
