# Electoral Votapp — Visión del Sistema para Stakeholders

## ¿Qué hace este software?

**Electoral Votapp** es un sistema de votación institucional diseñado para que organizaciones públicas o privadas puedan realizar procesos electorales internos de forma **transparente, anónima, auditada y escalable**.

El sistema permite que una institución (ministerio, empresa, organismo) administre elecciones completas: desde la configuración de la estructura organizativa hasta el conteo de votos, pasando por la emisión de credenciales de voto, la votación anónima y el registro de auditoría.

Está construido como un monolito modular con **Arquitectura Hexagonal**, lo que significa que las reglas de negocio están aisladas de la tecnología (bases de datos, frameworks, APIs REST), garantizando que el sistema pueda evolucionar sin reescribir su núcleo.

---

## Las Fases del Sistema

El ciclo de vida completo de una elección dentro del sistema se divide en **5 fases**. Cada fase resuelve un problema concreto y tiene requisitos de seguridad, anonimato o atomicidad específicos.

```
┌────────────────────────────────────────────────────────────────────┐
│                                                                   │
│  1. CONFIGURACIÓN ORGANIZACIONAL                                  │
│       ↓                                                            │
│  2. PREPARACIÓN ELECTORAL                                          │
│       ↓                                                            │
│  3. EMISIÓN DE TOKENS DE VOTACIÓN                                  │
│       ↓                                                            │
│  4. VOTACIÓN (el núcleo del sistema)                               │
│       ↓                                                            │
│  5. CIERRE, AUDITORÍA Y PARTICIPACIÓN                              │
│                                                                   │
└────────────────────────────────────────────────────────────────────┘
```
---

### Fase 1 — Configuración Organizacional

**Problema que resuelve**: La institución necesita definir QUIÉNES pueden votar, desde qué cargos y departamentos.

**Lo que hace el sistema**:

- Registra la **estructura jerárquica** de la organización: departamentos, cargos y niveles jerárquicos.
- Incorpora a los **funcionarios** (empleados) con sus datos, estado laboral (activo/inactivo/suspendido/retirado) y elegibilidad para votar.
- Establece autenticación por credenciales para que los funcionarios accedan al sistema (mediante hash de contraseñas).

**Entidades principales**: `departamentos`, `cargos`, `funcionarios`

**Valor para la organización**: Una sola vez se carga la estructura, y queda disponible para todas las elecciones futuras. La elegibilidad se controla a nivel de cada funcionario.

---

### Fase 2 — Preparación Electoral

**Problema que resuelve**: El comité electoral necesita definir QUÉ se vota, QUIÉNES son los candidatos y CUÁNDO ocurre.

**Lo que hace el sistema**:

- Crea la **elección** con nombre, código, fechas de inicio y fin, y estado inicial `PROGRAMADA`.
- Registra los **candidatos** que participan en la elección.
- Activa la elección (transición a estado `ACTIVA`), momento en el cual el sistema **genera automáticamente el candidato sintético "Voto en Blanco"** — nadie lo crea manualmente.
- Durante la vigencia de la elección, puede suspenderse temporalmente (estado `SUSPENDIDA`, reanudable) o cancelarse definitivamente.

**Ciclo de vida de una elección**:

```
PROGRAMADA ──→ ACTIVA ──→ FINALIZADA
     │              │
     └──→ CANCELADA │
                    │
               SUSPENDIDA ──→ ACTIVA (reanudable)
                    │
                    └──→ CANCELADA
```

**Valor para la organización**: Las transiciones de estado están gobernadas por reglas de negocio, no por acciones arbitrarias. No se puede votar en una elección que no está `ACTIVA`, ni se pueden modificar candidatos una vez iniciada.

---

### Fase 3 — Emisión de Tokens de Votación

**Problema que resuelve**: Cada funcionario hábil debe recibir UNA credencial de voto única e intransferible, sin exponer su identidad durante la votación.

**Lo que hace el sistema**:

- Un administrador emite un **token de votación** para un funcionario en una elección específica.
- El sistema genera un **rawToken** (secreto efímero mostrado UNA SOLA VEZ al administrador).
- **El rawToken jamás se persiste**. Solo se guarda su **hash SHA-256** en la base de datos.
- Cada funcionario puede tener **un solo token activo** por elección (garantizado por un índice único parcial en la base de datos).
- El token atraviesa los estados: `ISSUED` → `USED` (cuando se vota) o `INVALIDATED` (si el administrador lo revoca antes de usarse).

**Modelo de token**:

```
rawToken (secreto efímero, se muestra 1 vez)
    │
    ▼ SHA-256
tokenHash (único, se persiste en la DB)
    │
    ▼ UUID interno
tokenId (identificador para referencias internas)
```

**Valor para la organización**:

- **Seguridad**: el rawToken es la única credencial que necesita el funcionario para votar. No se almacena. Si alguien accede a la base de datos, no puede robar tokens activos.
- **Un voto por persona**: el índice único parcial impide emitir dos tokens activos al mismo funcionario en la misma elección.
- **Anonimato**: durante la votación, el sistema solo necesita el rawToken para validar; nunca sabe QUIÉN está votando.

---

### Fase 4 — Votación (el núcleo del sistema)

**Problema que resuelve**: Un funcionario debe emitir su voto de forma **anónima, atómica e irreversible**. El sistema debe garantizar que no haya doble votación, incluso bajo alta concurrencia.

**Lo que hace el sistema** — el flujo de `CastVote`:

1. El funcionario presenta su **rawToken** y elige un **candidato**.
2. El sistema adquiere un **bloqueo atómico en Redis** mediante `SETNX` (Set if Not eXists) para evitar que dos solicitudes concurrentes usen el mismo token.
3. Si el bloqueo falla → el token ya está siendo usado → se responde con `409 Conflict`.
4. Si el bloqueo se adquiere → se ejecuta una **transacción en PostgreSQL** que:
   - Revalida el token (existe, no está usado, no está invalidado).
   - Marca el token como `USED`.
   - Inserta el voto anónimo en la tabla `votos`.
   - Registra la participación del funcionario en `participacion_electoral`.
   - Escribe un evento de auditoría.
5. Se libera el bloqueo en Redis.

**Anonimato garantizado por diseño**:

- La tabla `votos` **NO** almacena `funcionario_id`. Nunca. Ni siquiera cifrado. No hay forma de conectar un voto con la persona que lo emitió.
- La participación se registra por separado en `participacion_electoral` (solo para saber quién cumplió con su deber cívico, no qué votó).

```
┌──────────────────────────────────────────────────┐
│                   CASTVOTE                        │
│                                                   │
│   rawToken + candidateId                          │
│       │                                           │
│       ▼                                           │
│   ┌────────────────┐                              │
│   │  Redis SETNX   │ ← Bloqueo atómico            │
│   └───────┬────────┘                              │
│           │ (locked)                               │
│           ▼                                       │
│   ┌────────────────┐                              │
│   │ PostgreSQL TXN  │                             │
│   │  • Revalidate   │                             │
│   │  • Mark USED    │                             │
│   │  • Insert voto  │ ← No tiene funcionario_id   │
│   │  • Insert part. │ ← Tiene funcionario_id      │
│   │  • Insert audit │                             │
│   └───────┬────────┘                              │
│           │ (committed)                            │
│           ▼                                       │
│   ┌────────────────┐                              │
│   │  Release lock  │                              │
│   └────────────────┘                              │
└──────────────────────────────────────────────────┘
```

**Valor para la organización**:

- **Atomicidad real**: no hay condición de carrera. Si 100 personas envían el mismo token simultáneamente, solo un voto se registra.
- **Anonimato irreversible**: la separación entre `votos` y `participacion_electoral` garantiza que nadie —ni siquiera un administrador del sistema— pueda saber qué votó cada persona.
- **Trazabilidad**: cada intento de voto queda registrado en auditoría.

---

### Fase 5 — Cierre, Auditoría y Participación

**Problema que resuelve**: Una vez finalizada la elección, la organización necesita saber quién participó y tener un registro inmutable de todos los eventos críticos.

**Lo que hace el sistema**:

- La elección se marca como `FINALIZADA`, y no se admiten más votos.
- El sistema de **auditoría** registra todos los eventos críticos:
  - `TOKEN_ISSUED` — emisión de un token
  - `VOTE_ACCEPTED` — voto registrado exitosamente
  - `VOTE_REJECTED` — intento de voto rechazado
  - `TOKEN_INVALID` — token inválido presentado
  - `TOKEN_INVALIDATED` — token revocado por administrador
- La tabla `participacion_electoral` permite saber qué funcionarios votaron (solo participación, no contenido del voto).
- Los resultados electorales se obtienen consultando la cantidad de votos por candidato en la tabla `votos`.

**Valor para la organización**:

- **Auditoría forense**: cada acción crítica tiene un registro inmutable con timestamp, tipo de evento y metadatos.
- **Transparencia**: se puede verificar la participación sin violar el anonimato.

---

## Resumen de Arquitectura para Stakeholders

| Componente | Rol |
|---|---|
| **Java 21 + Spring Boot** | Plataforma de ejecución del backend |
| **PostgreSQL** | Base de datos transaccional — esquemas, restricciones, índices |
| **Redis** | Bloqueo atómico para evitar doble votación concurrente |
| **Docker** | Contenerización y redes aisladas para toda la infraestructura |
| **Flyway** | Versionado automático del esquema de base de datos |
| **Hexagonal Architecture** | Aislamiento de las reglas de negocio de los frameworks y bases de datos |

## Principios de Diseño Clave

1. **El voto es anónimo por diseño, no por configuración**: la tabla `votos` no tiene columna de funcionario, ni siquiera opcional. Es una decisión estructural.
2. **El secreto del token nunca se persiste**: el rawToken se muestra una vez y se olvida. Solo su hash vive en la base de datos.
3. **La atomicidad se logra con Redis + PostgreSQL**: Redis evita la contención (rápido), PostgreSQL garantiza la consistencia (confiable).
4. **Las reglas de negocio viven en el dominio Java, no en la base de datos**: no hay funciones PL/pgSQL con lógica electoral. El código es testeable, versionable y revisable.

---

*Documento generado para stakeholders del proyecto Electoral Votapp MVP.*
