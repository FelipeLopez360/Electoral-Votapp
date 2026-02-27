# Electoral Votapp - Institutional Voting System

![Java 21](https://img.shields.io/badge/Java-21-orange.svg)
![Spring Boot 3.3](https://img.shields.io/badge/Spring%20Boot-3.3-brightgreen.svg)
![Docker](https://img.shields.io/badge/Docker-Enabled-blue.svg)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Ready-blue.svg)
![Redis](https://img.shields.io/badge/Redis-SETNX-red.svg)

Electoral Votapp es un sistema de votación institucional diseñado para garantizar la transparencia, atomicidad y escalabilidad en procesos electorales.

## 🏛 Arquitectura

El sistema está construido siguiendo el patrón de **Monolito Modular (Package by Feature)**, lo que permite una alta cohesión dentro de cada dominio de negocio manteniendo un despliegue unificado. 

Dentro de cada módulo, se aplica **Arquitectura Hexagonal (Ports & Adapters)** para lograr una separación estricta:
- **Dominio**: Contiene la lógica de negocio pura y las reglas electorales, independiente de frameworks.
- **Aplicación (Casos de Uso)**: Orquesta las entidades de dominio y define puertos.
- **Infraestructura (Adaptadores)**: Implementa los puertos hacia el exterior (Spring Boot, JPA, Redis, Controladores REST).

Toda la implementación se guía estrictamente por **TDD (Test-Driven Development)**, **Clean Code** y principios **SOLID**.

## 🚀 Características Clave

- **Votación Atómica con Redis**: El sistema integra Redis para el almacenamiento en caché de tokens de validación. Al momento de registrar un voto, se utiliza la operación `SETNX` (Set if Not eXists) para garantizar la atomicidad absoluta de la transacción, previniendo condiciones de carrera y asegurando que un usuario no pueda emitir dos votos simultáneamente.
- **Redes Docker Aisladas**: Todo el stack (Frontend, Backend, PostgreSQL, Redis) se despliega mediante `docker-compose` en redes privadas aisladas, garantizando que el sistema solo pueda recibir tráfico desde la intranet de la institución.
- **Migraciones de Base de Datos**: Versionado automático de esquemas SQL gestionado por Flyway.

## 📦 Estructura de Módulos (Dominios)

El sistema se divide en 6 dominios de negocio claramente aislados:

1. **Authentication (Autenticación y Usuarios)**: Gestión de administradores, hashing de contraseñas y manejo de roles.
2. **Organization (Organización Institucional)**: Administración de la estructura jerárquica, departamentos y cargos habilitados para participar.
3. **Electoral (Gestión Electoral)**: Configuración core de las elecciones, periodos electorales y sus tipos.
4. **Candidates (Candidatos y Categorías)**: Administración de candidatos, listas y las categorías por las que compiten.
5. **Voting (Votación y Participación)**: Motor principal de alta concurrencia. Generación y validación de tokens en Redis, registro de votos anónimos y cálculo de participación.
6. **Audit (Auditoría)**: Registro inmutable y centralizado de acciones críticas en el sistema.

## 🛠 Setup y Ejecución

El proyecto incluye contenedores pre-configurados para todas las dependencias externas.

### Prerrequisitos
- Docker & Docker Compose instalados.
- Java 21 (opcional, si no se desea usar el wrapper incluido).

### Levantar la Infraestructura (Base de Datos y Caché)
Para iniciar PostgreSQL y Redis en la red privada de Docker:
```bash
docker-compose up -d
```

### Compilar y Ejecutar la Aplicación
El proyecto incluye Maven Wrapper, por lo que no es necesario tener Maven instalado globalmente:

```bash
# Limpiar, compilar dependencias y ejecutar migraciones Flyway:
./mvnw clean install

# Levantar la aplicación Spring Boot:
./mvnw spring-boot:run
```

La aplicación backend arrancará en el puerto configurado (típicamente `8080`). Las migraciones de Flyway (como `V1__Initial_schema.sql`) se ejecutarán automáticamente al iniciar la aplicación.
