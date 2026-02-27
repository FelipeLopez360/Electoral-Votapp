# Skill: Spring Boot Hexagonal & Redis Architecture
**Scope:** `backend`, `java`, `spring-boot`, `redis`, `security`
**Trigger:** Utiliza esta skill siempre que debas diseñar, planificar o implementar código Java, endpoints, lógica de negocio o configuraciones de base de datos.

## 1. Arquitectura Hexagonal (Puertos y Adaptadores)
- **Prohibido:** Los Controladores (Web) y los Repositorios (JPA) NUNCA deben interactuar entre sí directamente ni conocerse.
- **Dominio Central:** Contiene la lógica pura. No debe tener anotaciones de Spring (`@Service`, `@Entity`, etc.).
- **Puertos (Interfaces):** Define puertos de entrada (Use Cases) y puertos de salida (ej. `VotacionRepositoryPort`).
- **Adaptadores:** Implementan los puertos. Aquí van los `@RestController` (Adaptador Web) y las implementaciones de repositorios con JPA (Adaptador de Persistencia).

## 2. Java 21 & Spring Boot 3.3+
- Usa `Record` para todos los DTOs (Request/Response) de manera obligatoria.
- Usa Pattern Matching para `switch` y características modernas del lenguaje.
- Las entidades JPA deben usar UUID autogenerados como Primary Key (`@GeneratedValue(strategy = GenerationType.UUID)`), no IDs incrementales.

## 3. Concurrencia y Redis
- Usa Redis para almacenamiento de sesión, caché y tokens de votación.
- **Votación Segura:** Para evitar condiciones de carrera (race conditions) al votar, es obligatorio implementar bloqueos distribuidos o comandos atómicos como `SETNX` en Redis antes de persistir en PostgreSQL.

## 4. Spring Security
- Los endpoints estarán asegurados vía JWT (JSON Web Tokens).
- Aplica seguridad basada en roles (`@PreAuthorize`) según las tablas del dominio.

## 5. Clean Code & TDD
- Aplica el ciclo RED-GREEN-REFACTOR obligatoriamente. Escribe el test que falle primero, luego la implementación.