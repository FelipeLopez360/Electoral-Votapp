package co.com.votapp.ws;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test verifying the V5 migration contract:
 * - {@code numero_orden} and {@code afiliacion_politica} columns are DROPPED from {@code candidatos}
 * - {@code funcionario_id} column is ADDED with FK → {@code funcionarios(id)}
 * - Old UNIQUE constraint on {@code (eleccion_id, numero_orden)} is REMOVED
 * - Partial UNIQUE index on {@code (eleccion_id, funcionario_id) WHERE funcionario_id IS NOT NULL}
 *   exists and prevents duplicate funcionario per election
 *
 * <p>RED cycle: written first. Will FAIL until V5__candidate_funcionario_and_ordering.sql is created.
 */
@SpringBootTest
@Testcontainers
@DisplayName("V5 Candidate Migration — funcionario_id + drop orden/afiliacion")
class V5CandidateMigrationIT {

    static {
        TestcontainersDockerConfig.configure();
    }

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("votapp_test")
            .withUsername("postgres")
            .withPassword("votapp_password");

    @Container
    private static final GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private JdbcTemplate jdbc;

    // ── helpers ──────────────────────────────────────────────────────────────

    private List<String> columnsOf(String table) {
        return jdbc.queryForList(
                "SELECT column_name FROM information_schema.columns " +
                "WHERE table_schema = 'public' AND table_name = ? ORDER BY ordinal_position",
                String.class, table);
    }

    private boolean indexExists(String indexName) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM pg_indexes WHERE schemaname = 'public' AND indexname = ?",
                Integer.class, indexName);
        return count != null && count > 0;
    }

    private boolean uniqueConstraintOnColumn(String table, String columnName) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.key_column_usage kcu " +
                "JOIN information_schema.table_constraints tc " +
                "  ON kcu.constraint_name = tc.constraint_name " +
                "  AND kcu.table_schema = tc.table_schema " +
                "WHERE tc.table_schema = 'public' AND kcu.table_name = ? " +
                "  AND kcu.column_name = ? AND tc.constraint_type = 'UNIQUE'",
                Integer.class, table, columnName);
        return count != null && count > 0;
    }

    private boolean columnExists(String table, String column) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns " +
                "WHERE table_schema = 'public' AND table_name = ? AND column_name = ?",
                Integer.class, table, column);
        return count != null && count > 0;
    }

    // ── DROPPED columns ───────────────────────────────────────────────────────

    @Test
    @DisplayName("candidatos.numero_orden column does NOT exist after V5")
    void candidatosNumeroOrdenDropped() {
        assertThat(columnExists("candidatos", "numero_orden")).isFalse();
    }

    @Test
    @DisplayName("candidatos.afiliacion_politica column does NOT exist after V5")
    void candidatosAfiliacionPoliticaDropped() {
        assertThat(columnExists("candidatos", "afiliacion_politica")).isFalse();
    }

    // ── Old UNIQUE constraint removed ─────────────────────────────────────────

    @Test
    @DisplayName("Old UNIQUE(eleccion_id, numero_orden) constraint is gone after V5")
    void oldUniqueConstraintRemoved() {
        // numero_orden is gone, so no unique constraint on it either
        assertThat(uniqueConstraintOnColumn("candidatos", "numero_orden")).isFalse();
    }

    // ── ADDED column ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("candidatos.funcionario_id column exists after V5")
    void candidatosFuncionarioIdAdded() {
        assertThat(columnExists("candidatos", "funcionario_id")).isTrue();
    }

    @Test
    @DisplayName("candidatos.funcionario_id is nullable (synthetic candidates have NULL)")
    void candidatosFuncionarioIdIsNullable() {
        Integer isNullable = jdbc.queryForObject(
                "SELECT CASE WHEN is_nullable = 'YES' THEN 1 ELSE 0 END " +
                "FROM information_schema.columns " +
                "WHERE table_schema = 'public' AND table_name = 'candidatos' AND column_name = 'funcionario_id'",
                Integer.class);
        assertThat(isNullable).isEqualTo(1);
    }

    @Test
    @DisplayName("candidatos.funcionario_id has FK to funcionarios(id)")
    void candidatosFuncionarioIdHasFk() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.referential_constraints rc " +
                "JOIN information_schema.key_column_usage kcu " +
                "  ON rc.constraint_name = kcu.constraint_name " +
                "  AND kcu.table_schema = rc.constraint_schema " +
                "WHERE kcu.table_schema = 'public' AND kcu.table_name = 'candidatos' " +
                "  AND kcu.column_name = 'funcionario_id'",
                Integer.class);
        assertThat(count).isGreaterThan(0);
    }

    // ── Partial unique index ──────────────────────────────────────────────────

    @Test
    @DisplayName("Partial unique index on (eleccion_id, funcionario_id) WHERE NOT NULL exists")
    void partialUniqueIndexExists() {
        assertThat(indexExists("uq_candidatos_eleccion_funcionario")).isTrue();
    }

    @Test
    @DisplayName("Partial index enforces uniqueness: same (eleccion_id, funcionario_id) rejected")
    void partialIndexEnforcesUniqueness() {
        // Use the seed funcionario id=1 (EMP001 from V1 seed data)
        UUID eleccionId = insertTestEleccion();

        // Insert first candidate with funcionario_id=1
        jdbc.update(
            "INSERT INTO candidatos (id, eleccion_id, nombre, es_voto_en_blanco, es_voto_nulo, funcionario_id) " +
            "VALUES (gen_random_uuid(), ?, 'Candidato A', false, false, 1)",
            eleccionId);

        // Attempting to insert another candidate with the same (eleccion_id, funcionario_id=1) should fail
        boolean threw = false;
        try {
            jdbc.update(
                "INSERT INTO candidatos (id, eleccion_id, nombre, es_voto_en_blanco, es_voto_nulo, funcionario_id) " +
                "VALUES (gen_random_uuid(), ?, 'Candidato B', false, false, 1)",
                eleccionId);
        } catch (Exception e) {
            threw = true;
        }
        assertThat(threw).as("Duplicate (eleccion_id, funcionario_id) should violate partial unique index").isTrue();
    }

    @Test
    @DisplayName("Multiple NULL funcionario_id rows in same election are allowed (synthetic candidates)")
    void multipleNullFuncionarioIdAllowed() {
        UUID eleccionId = insertTestEleccion();

        // Two synthetic candidates (NULL funcionario_id) in same election should both succeed
        jdbc.update(
            "INSERT INTO candidatos (id, eleccion_id, nombre, es_voto_en_blanco, es_voto_nulo) " +
            "VALUES (gen_random_uuid(), ?, 'Voto en Blanco', true, false)",
            eleccionId);

        // Should NOT throw — NULL is excluded from the partial index
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() ->
            jdbc.update(
                "INSERT INTO candidatos (id, eleccion_id, nombre, es_voto_en_blanco, es_voto_nulo) " +
                "VALUES (gen_random_uuid(), ?, 'Voto Nulo', false, true)",
                eleccionId)
        );
    }

    // ── Existing columns still present ───────────────────────────────────────

    @Test
    @DisplayName("candidatos still has expected remaining columns after V5")
    void candidatosRemainingColumnsPresent() {
        List<String> cols = columnsOf("candidatos");
        assertThat(cols).contains("id", "eleccion_id", "nombre", "descripcion",
                "es_voto_en_blanco", "es_voto_nulo", "foto_url", "biografia",
                "propuestas", "created_at", "funcionario_id");
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private UUID insertTestEleccion() {
        UUID id = UUID.randomUUID();
        jdbc.update(
            "INSERT INTO elecciones (id, codigo, nombre, estado, fecha_inicio, fecha_fin) " +
            "VALUES (?, ?, 'Test Election V5', 'PROGRAMADA', NOW() + INTERVAL '1 day', NOW() + INTERVAL '2 days')",
            id, "V5-TEST-" + id.toString().substring(0, 8));
        return id;
    }
}
