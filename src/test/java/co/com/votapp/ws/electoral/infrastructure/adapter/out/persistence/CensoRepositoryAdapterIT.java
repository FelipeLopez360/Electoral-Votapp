package co.com.votapp.ws.electoral.infrastructure.adapter.out.persistence;

import co.com.votapp.ws.TestcontainersDockerConfig;
import co.com.votapp.ws.electoral.domain.model.CensoEntry;
import co.com.votapp.ws.common.domain.model.PageResult;
import co.com.votapp.ws.electoral.domain.port.out.CensoRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link CensoRepositoryAdapter}.
 *
 * <p>Verifies the full persistence contract with a real PostgreSQL instance
 * via Testcontainers. Tests cover: save, saveAll (bulk with ON CONFLICT DO NOTHING),
 * paginated findByEleccionId, existsByEleccionIdAndFuncionarioId, countByEleccionId,
 * deleteByEleccionIdAndFuncionarioId, deleteAllByEleccionId, and hasCensus.
 *
 * <p>TDD RED → GREEN: tests written before any implementation changes.
 */
@SpringBootTest
@Testcontainers
@DisplayName("CensoRepositoryAdapter - Integration tests with real PostgreSQL")
class CensoRepositoryAdapterIT {

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
    private CensoRepositoryPort censoRepository;

    @Autowired
    private JdbcTemplate jdbc;

    /**
     * Election UUID created per test class — seeded once, all tests share it.
     * We use a fixed codigo to allow ON CONFLICT DO NOTHING in setup.
     */
    private UUID eleccionId;

    /**
     * A real funcionario_id from seed data (ACTIVO + puede_votar = true).
     */
    private Integer funcionarioId;
    private Integer funcionarioId2;

    @BeforeEach
    void setUp() {
        // Seed one test election per run (idempotent)
        jdbc.update("""
                INSERT INTO elecciones (id, codigo, nombre, estado, fecha_inicio, fecha_fin)
                VALUES (gen_random_uuid(), 'IT-CENSO-ADAPTER', 'IT Census Test Election', 'PROGRAMADA',
                        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '1 day')
                ON CONFLICT (codigo) DO NOTHING
                """);

        eleccionId = jdbc.queryForObject(
                "SELECT id FROM elecciones WHERE codigo = 'IT-CENSO-ADAPTER'",
                UUID.class);

        // Grab two real funcionarios from seed data
        List<Integer> ids = jdbc.queryForList(
                "SELECT id FROM funcionarios WHERE estado_laboral = 'ACTIVO' AND puede_votar = true LIMIT 2",
                Integer.class);
        assertThat(ids).as("Seed data must have at least 2 ACTIVO+puede_votar funcionarios").hasSizeGreaterThanOrEqualTo(2);
        funcionarioId = ids.get(0);
        funcionarioId2 = ids.get(1);

        // Clean census entries from previous tests
        jdbc.update("DELETE FROM censo_electoral WHERE eleccion_id = ?", eleccionId);
    }

    // ─── save ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should save a census entry and return it with a generated id")
    void save_shouldPersistEntry_andReturnWithId() {
        // Given
        var entry = new CensoEntry(null, eleccionId, funcionarioId, null, Instant.now());

        // When
        CensoEntry saved = censoRepository.save(entry);

        // Then
        assertThat(saved.id()).isNotNull();
        assertThat(saved.eleccionId()).isEqualTo(eleccionId);
        assertThat(saved.funcionarioId()).isEqualTo(funcionarioId);
    }

    // ─── saveAll / bulkInsert ─────────────────────────────────────────────────

    @Test
    @DisplayName("Should save all entries via saveAll and persist them to the database")
    void saveAll_shouldPersistAllEntries() {
        // Given
        var entry1 = new CensoEntry(null, eleccionId, funcionarioId, null, Instant.now());
        var entry2 = new CensoEntry(null, eleccionId, funcionarioId2, null, Instant.now());

        // When
        censoRepository.saveAll(List.of(entry1, entry2));

        // Then — verify persistence via count (native query returns input entries without DB-generated IDs)
        assertThat(censoRepository.countByEleccionId(eleccionId)).isEqualTo(2);
        assertThat(censoRepository.existsByEleccionIdAndFuncionarioId(eleccionId, funcionarioId)).isTrue();
        assertThat(censoRepository.existsByEleccionIdAndFuncionarioId(eleccionId, funcionarioId2)).isTrue();
    }

    @Test
    @DisplayName("Should skip duplicate on second saveAll (idempotent bulk insert)")
    void saveAll_shouldSkipDuplicates_whenConflict() {
        // Given — save once
        var entry = new CensoEntry(null, eleccionId, funcionarioId, null, Instant.now());
        censoRepository.save(entry);

        // When — saveAll with the same funcionario (duplicate) + a new one
        var duplicate = new CensoEntry(null, eleccionId, funcionarioId, null, Instant.now());
        var newEntry = new CensoEntry(null, eleccionId, funcionarioId2, null, Instant.now());
        censoRepository.saveAll(List.of(duplicate, newEntry));

        // Then — only 2 total entries (1 original + 1 new, duplicate skipped)
        assertThat(censoRepository.countByEleccionId(eleccionId)).isEqualTo(2);
    }

    // ─── findByEleccionId (paginated) ─────────────────────────────────────────

    @Test
    @DisplayName("Should return paginated census entries for an election")
    void findByEleccionId_shouldReturnPage_whenEntriesExist() {
        // Given
        censoRepository.saveAll(List.of(
                new CensoEntry(null, eleccionId, funcionarioId, null, Instant.now()),
                new CensoEntry(null, eleccionId, funcionarioId2, null, Instant.now())
        ));

        // When
        PageResult<CensoEntry> page = censoRepository.findByEleccionId(eleccionId, 0, 10);

        // Then
        assertThat(page.totalElements()).isEqualTo(2);
        assertThat(page.content()).allSatisfy(e -> assertThat(e.eleccionId()).isEqualTo(eleccionId));
    }

    @Test
    @DisplayName("Should return empty page when election has no census entries")
    void findByEleccionId_shouldReturnEmptyPage_whenNoCensus() {
        // Given — no entries added

        // When
        PageResult<CensoEntry> page = censoRepository.findByEleccionId(eleccionId, 0, 10);

        // Then
        assertThat(page.totalElements()).isEqualTo(0);
        assertThat(page.content()).isEmpty();
    }

    // ─── existsByEleccionIdAndFuncionarioId ──────────────────────────────────

    @Test
    @DisplayName("Should return true when funcionario is in election census")
    void existsByEleccionIdAndFuncionarioId_shouldReturnTrue_whenPresent() {
        // Given
        censoRepository.save(new CensoEntry(null, eleccionId, funcionarioId, null, Instant.now()));

        // When / Then
        assertThat(censoRepository.existsByEleccionIdAndFuncionarioId(eleccionId, funcionarioId)).isTrue();
    }

    @Test
    @DisplayName("Should return false when funcionario is not in election census")
    void existsByEleccionIdAndFuncionarioId_shouldReturnFalse_whenAbsent() {
        // Given — empty census

        // When / Then
        assertThat(censoRepository.existsByEleccionIdAndFuncionarioId(eleccionId, funcionarioId)).isFalse();
    }

    // ─── countByEleccionId ───────────────────────────────────────────────────

    @Test
    @DisplayName("Should return correct count of census entries")
    void countByEleccionId_shouldReturnAccurateCount() {
        // Given
        censoRepository.saveAll(List.of(
                new CensoEntry(null, eleccionId, funcionarioId, null, Instant.now()),
                new CensoEntry(null, eleccionId, funcionarioId2, null, Instant.now())
        ));

        // When / Then
        assertThat(censoRepository.countByEleccionId(eleccionId)).isEqualTo(2);
    }

    // ─── hasCensus ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return true when election has at least one entry")
    void hasCensus_shouldReturnTrue_whenCensusNotEmpty() {
        // Given
        censoRepository.save(new CensoEntry(null, eleccionId, funcionarioId, null, Instant.now()));

        // When / Then
        assertThat(censoRepository.hasCensus(eleccionId)).isTrue();
    }

    @Test
    @DisplayName("Should return false when election census is empty")
    void hasCensus_shouldReturnFalse_whenCensusEmpty() {
        // Given — no entries

        // When / Then
        assertThat(censoRepository.hasCensus(eleccionId)).isFalse();
    }

    // ─── deleteByEleccionIdAndFuncionarioId ──────────────────────────────────

    @Test
    @DisplayName("Should remove a specific funcionario from election census")
    void deleteByEleccionIdAndFuncionarioId_shouldRemoveEntry() {
        // Given — two entries
        censoRepository.saveAll(List.of(
                new CensoEntry(null, eleccionId, funcionarioId, null, Instant.now()),
                new CensoEntry(null, eleccionId, funcionarioId2, null, Instant.now())
        ));
        assertThat(censoRepository.countByEleccionId(eleccionId)).isEqualTo(2);

        // When
        censoRepository.deleteByEleccionIdAndFuncionarioId(eleccionId, funcionarioId);

        // Then
        assertThat(censoRepository.countByEleccionId(eleccionId)).isEqualTo(1);
        assertThat(censoRepository.existsByEleccionIdAndFuncionarioId(eleccionId, funcionarioId)).isFalse();
        assertThat(censoRepository.existsByEleccionIdAndFuncionarioId(eleccionId, funcionarioId2)).isTrue();
    }

    // ─── deleteAllByEleccionId ───────────────────────────────────────────────

    @Test
    @DisplayName("Should clear all census entries for an election")
    void deleteAllByEleccionId_shouldRemoveAllEntries() {
        // Given — two entries
        censoRepository.saveAll(List.of(
                new CensoEntry(null, eleccionId, funcionarioId, null, Instant.now()),
                new CensoEntry(null, eleccionId, funcionarioId2, null, Instant.now())
        ));
        assertThat(censoRepository.countByEleccionId(eleccionId)).isEqualTo(2);

        // When
        censoRepository.deleteAllByEleccionId(eleccionId);

        // Then
        assertThat(censoRepository.countByEleccionId(eleccionId)).isEqualTo(0);
        assertThat(censoRepository.hasCensus(eleccionId)).isFalse();
    }
}
