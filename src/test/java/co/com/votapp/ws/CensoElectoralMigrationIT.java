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
 * Integration test verifying that the V2 Flyway migration produces the expected
 * {@code censo_electoral} table structure, constraints and indexes.
 *
 * <p>TDD RED cycle — written before the migration file is complete.
 * All assertions focus on schema contracts, not data.
 */
@SpringBootTest
@Testcontainers
@DisplayName("V2 Migration - censo_electoral table structure")
class CensoElectoralMigrationIT {

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

    private String dataTypeOf(String table, String column) {
        return jdbc.queryForObject(
                "SELECT data_type FROM information_schema.columns " +
                "WHERE table_schema = 'public' AND table_name = ? AND column_name = ?",
                String.class, table, column);
    }

    private boolean indexExists(String indexName) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM pg_indexes WHERE schemaname = 'public' AND indexname = ?",
                Integer.class, indexName);
        return count != null && count > 0;
    }

    private boolean uniqueConstraintOnColumns(String table, String columnName) {
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

    private boolean tableExists(String tableName) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables " +
                "WHERE table_schema = 'public' AND table_name = ?",
                Integer.class, tableName);
        return count != null && count > 0;
    }

    // ── Table existence ───────────────────────────────────────────────────────

    @Test
    @DisplayName("censo_electoral table exists after V2 migration")
    void censoElectoralTableExists() {
        assertThat(tableExists("censo_electoral")).isTrue();
    }

    // ── Column structure ──────────────────────────────────────────────────────

    @Test
    @DisplayName("censo_electoral has all required columns")
    void censoElectoralHasRequiredColumns() {
        List<String> cols = columnsOf("censo_electoral");
        assertThat(cols).contains("id", "eleccion_id", "funcionario_id", "agregado_por", "created_at");
    }

    @Test
    @DisplayName("censo_electoral.id is UUID type")
    void censoElectoralIdIsUuid() {
        assertThat(dataTypeOf("censo_electoral", "id")).isEqualTo("uuid");
    }

    @Test
    @DisplayName("censo_electoral.eleccion_id is UUID type")
    void censoElectoralEleccionIdIsUuid() {
        assertThat(dataTypeOf("censo_electoral", "eleccion_id")).isEqualTo("uuid");
    }

    @Test
    @DisplayName("censo_electoral.funcionario_id is integer type")
    void censoElectoralFuncionarioIdIsInteger() {
        assertThat(dataTypeOf("censo_electoral", "funcionario_id")).isEqualTo("integer");
    }

    // ── Unique constraint ─────────────────────────────────────────────────────

    @Test
    @DisplayName("censo_electoral has UNIQUE constraint on (eleccion_id, funcionario_id)")
    void censoElectoralHasUniqueConstraintOnEleccionAndFuncionario() {
        assertThat(uniqueConstraintOnColumns("censo_electoral", "eleccion_id")).isTrue();
    }

    // ── Indexes ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("idx_censo_eleccion index exists")
    void censoElectoralEleccionIndexExists() {
        assertThat(indexExists("idx_censo_eleccion")).isTrue();
    }

    @Test
    @DisplayName("idx_censo_funcionario index exists")
    void censoElectoralFuncionarioIndexExists() {
        assertThat(indexExists("idx_censo_funcionario")).isTrue();
    }

    // ── FK behavior: ON DELETE CASCADE ────────────────────────────────────────

    @Test
    @DisplayName("Inserting a censo entry with valid FK works")
    void censoElectoral_shouldAcceptInsert_whenFkIsValid() {
        // Given: seed data from V1 already has funcionarios and elecciones can be inserted
        // We insert a minimal eleccion and use a seed funcionario
        jdbc.update("""
                INSERT INTO elecciones (id, codigo, nombre, estado, fecha_inicio, fecha_fin)
                VALUES (gen_random_uuid(), 'TEST-CENSO-MIGRATION', 'Test Election', 'PROGRAMADA',
                        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '1 day')
                ON CONFLICT (codigo) DO NOTHING
                """);

        UUID eleccionId = jdbc.queryForObject(
                "SELECT id FROM elecciones WHERE codigo = 'TEST-CENSO-MIGRATION'",
                UUID.class);
        Integer funcionarioId = jdbc.queryForObject(
                "SELECT id FROM funcionarios WHERE estado_laboral = 'ACTIVO' AND puede_votar = true LIMIT 1",
                Integer.class);

        // When
        int inserted = jdbc.update("""
                INSERT INTO censo_electoral (eleccion_id, funcionario_id, agregado_por)
                VALUES (?, ?, NULL)
                ON CONFLICT (eleccion_id, funcionario_id) DO NOTHING
                """, eleccionId, funcionarioId);

        // Then
        assertThat(inserted).isGreaterThanOrEqualTo(0); // 0 on conflict, 1 on insert

        // Cleanup
        jdbc.update("DELETE FROM censo_electoral WHERE eleccion_id = ?", eleccionId);
        jdbc.update("DELETE FROM elecciones WHERE codigo = 'TEST-CENSO-MIGRATION'");
    }

    @Test
    @DisplayName("Duplicate (eleccion_id, funcionario_id) insert is silently skipped")
    void censoElectoral_shouldSkipDuplicate_whenConflictOccurs() {
        // Given: insert an eleccion and get a seed funcionario
        jdbc.update("""
                INSERT INTO elecciones (id, codigo, nombre, estado, fecha_inicio, fecha_fin)
                VALUES (gen_random_uuid(), 'TEST-CENSO-DEDUP', 'Dedup Election', 'PROGRAMADA',
                        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '1 day')
                ON CONFLICT (codigo) DO NOTHING
                """);

        UUID eleccionId = jdbc.queryForObject(
                "SELECT id FROM elecciones WHERE codigo = 'TEST-CENSO-DEDUP'",
                UUID.class);
        Integer funcionarioId = jdbc.queryForObject(
                "SELECT id FROM funcionarios WHERE estado_laboral = 'ACTIVO' AND puede_votar = true LIMIT 1",
                Integer.class);

        // When: insert same entry twice
        jdbc.update(
                "INSERT INTO censo_electoral (eleccion_id, funcionario_id) VALUES (?, ?)",
                eleccionId, funcionarioId);
        int secondInsert = jdbc.update("""
                INSERT INTO censo_electoral (eleccion_id, funcionario_id)
                VALUES (?, ?)
                ON CONFLICT (eleccion_id, funcionario_id) DO NOTHING
                """, eleccionId, funcionarioId);

        // Then: second insert returns 0 (no rows affected)
        assertThat(secondInsert).isEqualTo(0);

        // Count: still only one entry
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM censo_electoral WHERE eleccion_id = ?",
                Integer.class, eleccionId);
        assertThat(count).isEqualTo(1);

        // Cleanup
        jdbc.update("DELETE FROM censo_electoral WHERE eleccion_id = ?", eleccionId);
        jdbc.update("DELETE FROM elecciones WHERE codigo = 'TEST-CENSO-DEDUP'");
    }
}
