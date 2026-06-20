# Proposal: Arquitectura Base para el Sistema de Votaciones

## Intent

El objetivo de este cambio es implementar la arquitectura base para el sistema de votaciones institucional, garantizando un diseño escalable, modular y alineado con las mejores prácticas de ingeniería. El diseño seguirá principios de modularidad (Monolithic Modular con Package by Feature) y aislamiento de lógica de dominio (Hexagonal Architecture) para cumplir con los requisitos definidos. Esto incluye la integración de Redis para operaciones críticas y Docker Compose para orquestación de infraestructura.

## Scope

### In Scope
- Crear una arquitectura monolítica modular basada en Spring Boot:
  - Organizar los paquetes del proyecto utilizando Package by Feature (Autenticación, Organización, Elecciones, etc.).
- Implementar Hexagonal Architecture (Ports & Adapters) dentro de cada funcionalidad principal del backend.
- Crear los módulos necesarios:
  - Gestión de autenticación y autorización (Administradores, Roles).
  - Administración de la estructura organizacional (Departamentos y Cargos).
  - Gestión electoral (Elecciones, Tipos de Elecciones, Periodos Electorales).
  - Manejo de candidatos y categorías.
  - Mantenimiento de tokens y lógica para votaciones anónimas.
  - Sistemas de auditoría y logs.
- Ampliar el alcance para incluir el frontend:
  - Diseño e integración de un cliente web inicial.
- Configuración de PostgreSQL como base de datos central para persistencia.
- Cachear operaciones críticas en Redis (SETNX para garantizar atomicidad en votos y validación de tokens).
- Docker Compose:
  - Backend, base de datos, Redis y frontend deben correr en contenedores con redes privadas y rutas seguras, enfocadas en intranet.

### Out of Scope
- Implementación avanzada de notificaciones.
- Reportes visuales y dashboards estadísticos.
- Integraciones con sistemas externos (e.g., LDAP).

## Approach

Se adoptará un enfoque modular dividido en 6 dominios principales según la estructura SQL y necesidades funcionales:

1. **Autenticación y Usuarios:** Gestión de administradores, hashing de contraseñas y manejo de roles (`administradores`, `roles_sistema`).
2. **Organización Institucional:** Manejo de departamentos y cargos (`departamentos`, `cargos`).
3. **Gestión Electoral:** Configuración de elecciones, periodos electorales y tipos de elección (`elecciones`, `periodos_electorales`, `tipos_eleccion`).
4. **Candidatos y Categorías:** Administración de candidatos y sus categorías (`candidatos`, `categorias_candidatos`).
5. **Votación y Participación:** Generación y validación de tokens, registro anónimo de votos (`tokens_votacion`, `votos`, `participacion_electoral`) y uso de Redis para garantizar rendimiento y atomicidad.
6. **Auditoría:** Registro centralizado de acciones críticas (`auditoria_sistema`).

Cada módulo contará con su propia capa de Dominio, Aplicación e Infraestructura, en línea con los principios de la arquitectura hexagonal:
- **Dominio:** Clases puras que representan lógica de negocio, completamente independientes de frameworks.
- **Aplicación:** Servicios que exponen funcionalidades del dominio a través de puertos.
- **Infraestructura:** Adapters para interactuar con base de datos, caché y sistemas externos.

La aplicación será desarrollada siguiendo TDD, principios de Clean Code y SOLID para asegurar una base robusta y fácilmente mantenible.

## Affected Areas

| Area                           | Impacto  | Descripción                     |
|--------------------------------|----------|---------------------------------|
| Backend (Spring Boot)          | Nuevo    | Implementación completa de la arquitectura inicial. |
| Base de datos (PostgreSQL)     | Nuevo    | Definición de entidades y sincronización con JPA/Hibernate. |
| Cache (Redis)                  | Nuevo    | Operaciones críticas para tokens y atomicidad. |
| Infraestructura (Docker)       | Nuevo    | Orquestación con Docker Compose para contenedores. |
| Frontend                       | Ampliado | Desarrollo inicial del cliente web como parte del alcance. |

## Risks

| Riesgo                                | Probabilidad | Mitigación                                      |
|--------------------------------------|--------------|------------------------------------------------|
| Complejidad en integración inicial   | Media        | Priorizar un enfoque iterativo para el desarrollo de módulos. |
| Errores de sincronización con DB SQL | Baja         | Verificación continua, pruebas de integración con scripts SQL. |
| Problemas en red Docker aislada      | Media        | Validar configuración de redes privadas desde el inicio. |
| Rendimiento de Redis como caché      | Baja         | Realizar pruebas de carga simulando concurrencia. |

## Rollback Plan

En caso de errores:
- Restaurar configuraciones preexistentes desde backups de Git y base de datos.
- Documentar todos los problemas encontrados para rediseño incremental.

## Dependencies

- PostgreSQL como base de datos relacional central.
- Redis para atomicidad y cacheo de operaciones críticas.
- Spring Boot (Java 21) para el stack backend.
- Frontend basado en tecnología web (a decidir).
- Docker Compose para orquestación.

## Success Criteria

- [ ] Arquitectura modular basada en 6 dominios implementada exitosamente.
- [ ] API REST completamente funcional con autenticación y servicios para elecciones, votos y auditorías.
- [ ] Redis integrado para tokens de votación.
- [ ] Infraestructura Dockerizada con redes privadas seguras.
- [ ] Base de datos sincronizada entre backend y SQL (JPA/Hibernate).
- [ ] Frontend funcional inicial integrado con el backend y Docker.