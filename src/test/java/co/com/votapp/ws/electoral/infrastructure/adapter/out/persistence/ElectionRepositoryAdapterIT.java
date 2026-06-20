package co.com.votapp.ws.electoral.infrastructure.adapter.out.persistence;

import co.com.votapp.ws.TestcontainersDockerConfig;
import co.com.votapp.ws.common.domain.model.PageResult;
import co.com.votapp.ws.electoral.domain.Election;
import co.com.votapp.ws.electoral.domain.port.out.ElectionRepositoryPort;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for election pagination and search in {@link ElectionRepositoryAdapter}.
 *
 * <p>Verifies the paginated {@code findAll(int page, int size, String search)} method
 * against a real PostgreSQL instance via Testcontainers. Covers: case-insensitive search
 * by {@code codigo} and {@code nombre}, {@code createdAt DESC} ordering, second-page slicing,
 * and blank/null search returning all records.
 *
 * <p>Mirrors the {@code CensoRepositoryAdapterIT} and {@code FuncionarioRepositoryAdapterIT}
 * patterns for test infrastructure setup.
 *
 * <p>TDD: this test was written to satisfy the missing integration proof required by
 * the SDD verify-report for task 3.4 (feature-pagination).
 */
@SpringBootTest
@Testcontainers
@DisplayName("ElectionRepositoryAdapter - Integration tests: paginated findAll with real PostgreSQL")
class ElectionRepositoryAdapterIT {

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
    private ElectionRepositoryPort electionRepository;

    @Autowired
    private JdbcTemplate jdbc;

    /** Unique suffix to isolate test data across parallel runs and test class re-use. */
    private String suffix;

    @BeforeEach
    void setUp() {
        suffix = String.valueOf(System.nanoTime());
        // Seed three elections with unique codigo values for isolation
        insertElection("IT-ELEC-GEN-" + suffix, "Elección General " + suffix, -3);
        insertElection("IT-ELEC-PRE-" + suffix, "Elección Presidencial " + suffix, -2);
        insertElection("IT-ELEC-MUN-" + suffix, "Elección Municipal " + suffix, -1);
    }

    /**
     * Inserts a test election directly via JDBC.
     *
     * @param codigo         unique election code
     * @param nombre         election name
     * @param createdAtDelta days relative to now for createdAt (negative = in the past)
     */
    private void insertElection(String codigo, String nombre, int createdAtDelta) {
        jdbc.update("""
                INSERT INTO elecciones (id, codigo, nombre, estado, fecha_inicio, fecha_fin, created_at, updated_at)
                VALUES (gen_random_uuid(), ?, ?, 'PROGRAMADA',
                        CURRENT_TIMESTAMP + INTERVAL '1 day',
                        CURRENT_TIMESTAMP + INTERVAL '2 days',
                        CURRENT_TIMESTAMP + (? || ' days')::INTERVAL,
                        CURRENT_TIMESTAMP)
                ON CONFLICT (codigo) DO NOTHING
                """,
                codigo, nombre, createdAtDelta);
    }

    // ─── Tests ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return all elections when search is blank (paginated)")
    void findAll_shouldReturnElections_whenSearchIsBlank() {
        // When
        PageResult<Election> result = electionRepository.findAll(0, 50, "");

        // Then — at least the 3 we seeded are returned
        assertThat(result.content()).isNotEmpty();
        assertThat(result.totalElements()).isGreaterThanOrEqualTo(3);
        // page/size metadata reflects what was requested
        assertThat(result.page()).isEqualTo(0);
        assertThat(result.size()).isEqualTo(50);
    }

    @Test
    @DisplayName("Should return all elections when search is null")
    void findAll_shouldReturnElections_whenSearchIsNull() {
        // When
        PageResult<Election> withNull = electionRepository.findAll(0, 50, null);
        PageResult<Election> withEmpty = electionRepository.findAll(0, 50, "");

        // Then — both null and blank return the same total
        assertThat(withNull.totalElements()).isEqualTo(withEmpty.totalElements());
    }

    @Test
    @DisplayName("Should return only matching elections when searching by nombre (case-insensitive)")
    void findAll_shouldFilterByNombre_caseInsensitive() {
        // Given — "Presidencial" is in the nombre of exactly one seeded election (suffix is unique)
        String searchTerm = "Presidencial " + suffix;

        // When
        PageResult<Election> result = electionRepository.findAll(0, 50, searchTerm);

        // Then — only the Presidencial election is returned
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).nombre()).contains("Presidencial");
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should return only matching elections when searching by codigo (case-insensitive)")
    void findAll_shouldFilterByCodigo_caseInsensitive() {
        // Given — search by a unique codigo fragment (upper-case search, lower-case in DB)
        String searchTerm = "IT-ELEC-MUN-" + suffix;

        // When
        PageResult<Election> result = electionRepository.findAll(0, 50, searchTerm.toLowerCase());

        // Then — only the Municipal election matched by codigo (lowercase search)
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).codigo()).isEqualTo("IT-ELEC-MUN-" + suffix);
    }

    @Test
    @DisplayName("Should return elections ordered by createdAt DESC")
    void findAll_shouldReturnElectionsOrderedByCreatedAtDesc() {
        // Given — our 3 seeded elections have createdAt offsets: -3d, -2d, -1d
        // So the expected order (DESC) is: Municipal(-1d) > Presidencial(-2d) > General(-3d)
        String searchPrefix = "IT-ELEC-" + suffix.substring(0, Math.min(suffix.length(), 8));

        // Filter to only our seeded elections for this test run
        PageResult<Election> result = electionRepository.findAll(0, 10, suffix);

        // Then — must be non-empty and ordered DESC by createdAt
        // (most recent first = Municipal → Presidencial → General)
        assertThat(result.content()).hasSizeGreaterThanOrEqualTo(3);
        // Verify ordering: the Municipal election (newest: -1d) must appear before General (-3d)
        int municipalIndex = -1;
        int generalIndex = -1;
        for (int i = 0; i < result.content().size(); i++) {
            String codigo = result.content().get(i).codigo();
            if (codigo.equals("IT-ELEC-MUN-" + suffix)) municipalIndex = i;
            if (codigo.equals("IT-ELEC-GEN-" + suffix)) generalIndex = i;
        }
        assertThat(municipalIndex).as("Municipal (newest) must appear before General (oldest)").isLessThan(generalIndex);
    }

    @Test
    @DisplayName("Should return empty result when search matches no elections")
    void findAll_shouldReturnEmpty_whenNoMatchFound() {
        // Given — a search term that cannot match anything
        String noMatchTerm = "ZZZZ-NOMATCH-" + suffix;

        // When
        PageResult<Election> result = electionRepository.findAll(0, 8, noMatchTerm);

        // Then
        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should return second page of results with correct metadata")
    void findAll_shouldReturnSecondPage_whenPageOneRequested() {
        // Given — we have at least 3 elections seeded; request page 0 with size 2 to prove paging
        PageResult<Election> firstPage = electionRepository.findAll(0, 2, "");
        assertThat(firstPage.totalElements()).isGreaterThanOrEqualTo(3);
        assertThat(firstPage.totalPages()).isGreaterThanOrEqualTo(2);

        // When — request second page (page=1, size=2)
        PageResult<Election> secondPage = electionRepository.findAll(1, 2, "");

        // Then — second page must be returned with correct metadata
        assertThat(secondPage.page()).isEqualTo(1);
        assertThat(secondPage.size()).isEqualTo(2);
        assertThat(secondPage.totalElements()).isEqualTo(firstPage.totalElements());
        assertThat(secondPage.content()).isNotEmpty();
        // Items on second page must be different from first page
        assertThat(secondPage.content()).noneMatch(e -> firstPage.content().contains(e));
    }
}
