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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test that verifies the MVP schema migration (V1) produces
 * the expected tables, columns, constraints and indexes.
 *
 * <p>RED cycle: written first. Will FAIL until V1__Initial_schema.sql is rewritten
 * with the MVP schema (elecciones with UUID PK, candidatos, tokens_votacion with
 * status enum, votos without funcionario_id, participacion_electoral, auditoria_eventos).
 */
@SpringBootTest
@Testcontainers
class InitialSchemaMigrationIT {

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

    private List<String> tablesInPublicSchema() {
        return jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables " +
                "WHERE table_schema = 'public' AND table_type = 'BASE TABLE'",
                String.class);
    }

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

    private boolean constraintExists(String table, String type) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.table_constraints " +
                "WHERE table_schema = 'public' AND table_name = ? AND constraint_type = ?",
                Integer.class, table, type);
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

    private boolean indexExists(String indexName) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM pg_indexes WHERE schemaname = 'public' AND indexname = ?",
                Integer.class, indexName);
        return count != null && count > 0;
    }

    private boolean checkConstraintContains(String table, String keyword) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.check_constraints cc " +
                "JOIN information_schema.table_constraints tc " +
                "  ON cc.constraint_name = tc.constraint_name " +
                "WHERE tc.table_schema = 'public' AND tc.table_name = ? " +
                "  AND cc.check_clause LIKE ?",
                Integer.class, table, "%" + keyword + "%");
        return count != null && count > 0;
    }

    // ── Table existence ───────────────────────────────────────────────────────

    @Test
    @DisplayName("MVP required tables exist in public schema")
    void mvpTablesExist() {
        List<String> tables = tablesInPublicSchema();
        assertThat(tables).contains(
                "funcionarios",
                "departamentos",
                "cargos",
                "elecciones",
                "candidatos",
                "tokens_votacion",
                "votos",
                "participacion_electoral",
                "auditoria_eventos"
        );
    }

    // ── elecciones ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("elecciones.id is UUID type")
    void eleccionesIdIsUuid() {
        String dataType = dataTypeOf("elecciones", "id");
        assertThat(dataType).isEqualTo("uuid");
    }

    @Test
    @DisplayName("elecciones.codigo has UNIQUE constraint")
    void eleccionesCodigoIsUnique() {
        assertThat(uniqueConstraintOnColumns("elecciones", "codigo")).isTrue();
    }

    @Test
    @DisplayName("elecciones.estado CHECK constraint includes all MVP states")
    void eleccionesEstadoCheckConstraint() {
        assertThat(checkConstraintContains("elecciones", "PROGRAMADA")).isTrue();
        assertThat(checkConstraintContains("elecciones", "ACTIVA")).isTrue();
        assertThat(checkConstraintContains("elecciones", "FINALIZADA")).isTrue();
        assertThat(checkConstraintContains("elecciones", "CANCELADA")).isTrue();
        assertThat(checkConstraintContains("elecciones", "SUSPENDIDA")).isTrue();
    }

    @Test
    @DisplayName("elecciones has required MVP columns")
    void eleccionesHasRequiredColumns() {
        List<String> cols = columnsOf("elecciones");
        assertThat(cols).contains("id", "codigo", "nombre", "estado",
                "fecha_inicio", "fecha_fin", "created_at", "updated_at");
    }

    // ── candidatos ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("candidatos.id is UUID type")
    void candidatosIdIsUuid() {
        String dataType = dataTypeOf("candidatos", "id");
        assertThat(dataType).isEqualTo("uuid");
    }

    @Test
    @DisplayName("candidatos has es_voto_en_blanco column")
    void candidatosHasEsVotoEnBlancoColumn() {
        List<String> cols = columnsOf("candidatos");
        assertThat(cols).contains("es_voto_en_blanco");
    }

    @Test
    @DisplayName("candidatos CHECK: es_voto_en_blanco constraint exists (nombre = Voto en Blanco)")
    void candidatosEsVotoEnBlancoCheckExists() {
        assertThat(checkConstraintContains("candidatos", "es_voto_en_blanco")).isTrue();
    }

    @Test
    @DisplayName("candidatos.eleccion_id + numero_orden has UNIQUE constraint")
    void candidatosEleccionNumeroOrdenIsUnique() {
        assertThat(uniqueConstraintOnColumns("candidatos", "eleccion_id")).isTrue();
    }

    // ── tokens_votacion ───────────────────────────────────────────────────────

    @Test
    @DisplayName("tokens_votacion.id is UUID type")
    void tokensVotacionIdIsUuid() {
        String dataType = dataTypeOf("tokens_votacion", "id");
        assertThat(dataType).isEqualTo("uuid");
    }

    @Test
    @DisplayName("tokens_votacion.token_hash has UNIQUE constraint")
    void tokensVotacionTokenHashIsUnique() {
        assertThat(uniqueConstraintOnColumns("tokens_votacion", "token_hash")).isTrue();
    }

    @Test
    @DisplayName("tokens_votacion has status column (ISSUED/USED/INVALIDATED)")
    void tokensVotacionHasStatusColumn() {
        List<String> cols = columnsOf("tokens_votacion");
        assertThat(cols).contains("status");
    }

    @Test
    @DisplayName("tokens_votacion has issued_at and used_at columns")
    void tokensVotacionHasTimeColumns() {
        List<String> cols = columnsOf("tokens_votacion");
        assertThat(cols).contains("issued_at", "used_at");
    }

    @Test
    @DisplayName("tokens_votacion partial unique index exists for ISSUED status")
    void tokensVotacionPartialUniqueIndexExists() {
        assertThat(indexExists("uq_token_issued_funcionario_eleccion")).isTrue();
    }

    // ── votos ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("votos.id is UUID type")
    void votosIdIsUuid() {
        String dataType = dataTypeOf("votos", "id");
        assertThat(dataType).isEqualTo("uuid");
    }

    @Test
    @DisplayName("votos does NOT have funcionario_id column (anonymity)")
    void votosDoesNotHaveFuncionarioId() {
        List<String> cols = columnsOf("votos");
        assertThat(cols).doesNotContain("funcionario_id");
    }

    /**
     * V4 migration ({@code V4__Ballot_config_and_candidate_profiles.sql}) dropped the
     * {@code votos_token_id_key} UNIQUE constraint on {@code votos.token_id} to allow
     * multi-vote: one token can now produce N rows (one per selected candidate).
     *
     * <p>Atomicity is guaranteed by Redis SETNX lock + {@code markUsed()} in {@code CastVoteByTokenIdUseCaseImpl}.
     * This test was updated from the V1 assertion to reflect the V4 contract.
     */
    @Test
    @DisplayName("votos.token_id does NOT have UNIQUE constraint after V4 migration (multi-vote support)")
    void votosTokenIdHasNoUniqueConstraint_afterV4() {
        assertThat(uniqueConstraintOnColumns("votos", "token_id")).isFalse();
    }

    @Test
    @DisplayName("votos has required columns: eleccion_id, candidato_id, token_id, created_at")
    void votosHasRequiredColumns() {
        List<String> cols = columnsOf("votos");
        assertThat(cols).contains("eleccion_id", "candidato_id", "token_id", "created_at");
    }

    // ── participacion_electoral ───────────────────────────────────────────────

    @Test
    @DisplayName("participacion_electoral.id is UUID type")
    void participacionIdIsUuid() {
        String dataType = dataTypeOf("participacion_electoral", "id");
        assertThat(dataType).isEqualTo("uuid");
    }

    @Test
    @DisplayName("participacion_electoral has UNIQUE constraint on (eleccion_id, funcionario_id)")
    void participacionHasUniqueConstraint() {
        assertThat(uniqueConstraintOnColumns("participacion_electoral", "eleccion_id")).isTrue();
    }

    @Test
    @DisplayName("participacion_electoral has completado and completed_at columns")
    void participacionHasCompletadoColumn() {
        List<String> cols = columnsOf("participacion_electoral");
        assertThat(cols).contains("completado", "completed_at");
    }

    // ── auditoria_eventos ─────────────────────────────────────────────────────

    @Test
    @DisplayName("auditoria_eventos.id is UUID type")
    void auditoriaEventosIdIsUuid() {
        String dataType = dataTypeOf("auditoria_eventos", "id");
        assertThat(dataType).isEqualTo("uuid");
    }

    @Test
    @DisplayName("auditoria_eventos has tipo column with audit event types")
    void auditoriaEventosHasTipoColumn() {
        List<String> cols = columnsOf("auditoria_eventos");
        assertThat(cols).contains("tipo");
    }

    @Test
    @DisplayName("auditoria_eventos CHECK: tipo includes TOKEN_ISSUED and VOTE_ACCEPTED")
    void auditoriaEventosTipoCheckConstraint() {
        assertThat(checkConstraintContains("auditoria_eventos", "TOKEN_ISSUED")).isTrue();
        assertThat(checkConstraintContains("auditoria_eventos", "VOTE_ACCEPTED")).isTrue();
    }

    @Test
    @DisplayName("auditoria_eventos.funcionario_id is nullable (VOTE_ACCEPTED anonymity)")
    void auditoriaEventosFuncionarioIdIsNullable() {
        Integer isNullable = jdbc.queryForObject(
                "SELECT CASE WHEN is_nullable = 'YES' THEN 1 ELSE 0 END " +
                "FROM information_schema.columns " +
                "WHERE table_schema = 'public' AND table_name = 'auditoria_eventos' " +
                "AND column_name = 'funcionario_id'",
                Integer.class);
        assertThat(isNullable).isEqualTo(1);
    }

    @Test
    @DisplayName("auditoria_eventos has metadata JSONB column")
    void auditoriaEventosHasMetadataJsonb() {
        String dataType = dataTypeOf("auditoria_eventos", "metadata");
        assertThat(dataType).isEqualTo("jsonb");
    }

    // ── funcionarios base structure ───────────────────────────────────────────

    @Test
    @DisplayName("funcionarios has estado_laboral and puede_votar columns (eligibility)")
    void funcionariosHasEligibilityColumns() {
        List<String> cols = columnsOf("funcionarios");
        assertThat(cols).contains("estado_laboral", "puede_votar");
    }

    @Test
    @DisplayName("seed data: at least one funcionario with ACTIVO and puede_votar=true exists")
    void funcionariosSeedDataExists() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM funcionarios WHERE estado_laboral = 'ACTIVO' AND puede_votar = true",
                Integer.class);
        assertThat(count).isGreaterThan(0);
    }
}
