package co.com.votapp.ws.auth.infrastructure.adapter.out.persistence;

import co.com.votapp.ws.TestcontainersDockerConfig;
import co.com.votapp.ws.auth.domain.port.out.FuncionarioRepositoryPort;
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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the aggregate count methods added to {@link FuncionarioRepositoryAdapter}.
 *
 * <p>TDD: tests written FIRST (RED) for tasks 1.3/1.4.
 *
 * <ul>
 *   <li>{@code countByEstadoLaboral()} — GROUP BY estadoLaboral, zero-fill for known statuses</li>
 *   <li>{@code countActiveEligibleVoters()} — exact predicate: estadoLaboral='ACTIVO' AND puedeVotar=true</li>
 * </ul>
 */
@SpringBootTest
@Testcontainers
@DisplayName("FuncionarioRepositoryAdapter - countByEstadoLaboral and countActiveEligibleVoters IT")
class FuncionarioRepositoryAdapterCountsIT {

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
    private FuncionarioRepositoryPort funcionarioRepository;

    @Autowired
    private JdbcTemplate jdbc;

    /** Unique per test run to avoid constraint collisions. */
    private String suffix;

    @BeforeEach
    void setUp() {
        suffix = String.valueOf(System.nanoTime());
        // Seed test data with known estado_laboral and puede_votar combinations
        insertFuncionario("CNT-ACT-A-" + suffix, "ACTIVO", true);
        insertFuncionario("CNT-ACT-B-" + suffix, "ACTIVO", true);
        insertFuncionario("CNT-ACT-C-" + suffix, "ACTIVO", false);  // ACTIVO but puede_votar=false
        insertFuncionario("CNT-INA-A-" + suffix, "INACTIVO", true); // INACTIVO but puede_votar=true
        insertFuncionario("CNT-INA-B-" + suffix, "INACTIVO", false);
    }

    /**
     * Inserts a test funcionario directly via JDBC to avoid complexity of the adapter's
     * password-hash handling.
     *
     * <p>Both {@code numero_empleado} and {@code documento_identidad} must be globally unique.
     * We derive {@code numero_empleado} from the documento (already unique via nanosecond suffix),
     * keeping only the last 20 characters to respect the {@code VARCHAR(20)} schema constraint.
     * Since documento is unique per test invocation, the derived employee number is also unique.
     */
    private void insertFuncionario(String documento, String estadoLaboral, boolean puedeVotar) {
        // Derive numero_empleado from the documento; truncate to 20 chars (schema VARCHAR(20) limit)
        String empNum = documento.length() <= 20
                ? documento
                : documento.substring(documento.length() - 20);
        jdbc.update("""
                INSERT INTO funcionarios
                    (numero_empleado, documento_identidad, nombres, apellidos, tipo_documento,
                     email, password_hash, puede_votar, estado_laboral, debe_cambiar_password)
                VALUES (?, ?, 'Test', 'User', 'CC',
                        ?, '$2a$10$test-hash', ?, ?, true)
                """,
                empNum, documento,
                documento.toLowerCase() + "@test.com",
                puedeVotar,
                estadoLaboral);
    }

    // ─── countByEstadoLaboral tests ────────────────────────────────────────────

    @Test
    @DisplayName("Should group funcionarios by estadoLaboral correctly")
    void countByEstadoLaboral_shouldGroupByLaborStatus_whenFuncionariosExist() {
        // When
        Map<String, Long> counts = funcionarioRepository.countByEstadoLaboral();

        // Then — the map must include our seeded labor statuses
        assertThat(counts).isNotNull();
        assertThat(counts).containsKey("ACTIVO");
        assertThat(counts).containsKey("INACTIVO");
        // We seeded 3 ACTIVO (CNT-ACT-A, CNT-ACT-B, CNT-ACT-C) and 2 INACTIVO (CNT-INA-A, CNT-INA-B)
        // Actual counts may be higher due to data seeded by other tests, so use >=
        assertThat(counts.get("ACTIVO")).isGreaterThanOrEqualTo(3L);
        assertThat(counts.get("INACTIVO")).isGreaterThanOrEqualTo(2L);
    }

    @Test
    @DisplayName("Should return distinct labor status keys (no duplicates)")
    void countByEstadoLaboral_shouldReturnDistinctKeys_withNoNullValues() {
        // When
        Map<String, Long> counts = funcionarioRepository.countByEstadoLaboral();

        // Then
        assertThat(counts).isNotNull();
        // Every value must be a positive count (GROUP BY only returns rows that exist)
        counts.forEach((status, count) -> {
            assertThat(status).as("Status key must not be null").isNotNull();
            assertThat(count).as("Count for %s must be positive", status).isGreaterThan(0L);
        });
    }

    // ─── countActiveEligibleVoters tests ──────────────────────────────────────

    @Test
    @DisplayName("Should count exactly funcionarios where estadoLaboral=ACTIVO and puedeVotar=true")
    void countActiveEligibleVoters_shouldCountExactPredicate_whenMixedDataExists() {
        // Given — setUp seeded:
        //   CNT-ACT-A  : ACTIVO  + puedeVotar=true  → ELIGIBLE
        //   CNT-ACT-B  : ACTIVO  + puedeVotar=true  → ELIGIBLE
        //   CNT-ACT-C  : ACTIVO  + puedeVotar=false → NOT eligible
        //   CNT-INA-A  : INACTIVO + puedeVotar=true → NOT eligible (wrong estado)
        //   CNT-INA-B  : INACTIVO + puedeVotar=false → NOT eligible

        // When
        long eligibleCount = funcionarioRepository.countActiveEligibleVoters();

        // Then — must be ≥ 2 (the two ACTIVO+puedeVotar=true rows we seeded)
        // Other tests may have seeded eligible rows too, so we use >= to be robust
        assertThat(eligibleCount).isGreaterThanOrEqualTo(2L);
    }

    @Test
    @DisplayName("Should not count ACTIVO funcionarios with puedeVotar=false as eligible voters")
    void countActiveEligibleVoters_shouldExcludeActivoWithPuedeVotarFalse_whenPredicate() {
        // Given — count before we add a non-eligible ACTIVO row
        long countBefore = funcionarioRepository.countActiveEligibleVoters();

        // Insert an ACTIVO funcionario with puedeVotar=false (must NOT be counted)
        insertFuncionario("CNT-EXTRA-NV-" + suffix, "ACTIVO", false);

        // When
        long countAfter = funcionarioRepository.countActiveEligibleVoters();

        // Then — count must not have changed (puedeVotar=false was excluded)
        assertThat(countAfter).isEqualTo(countBefore);
    }

    @Test
    @DisplayName("Should not count INACTIVO funcionarios with puedeVotar=true as eligible voters")
    void countActiveEligibleVoters_shouldExcludeInactivoWithPuedeVotarTrue_whenPredicate() {
        // Given — count before
        long countBefore = funcionarioRepository.countActiveEligibleVoters();

        // Insert an INACTIVO funcionario with puedeVotar=true (must NOT be counted)
        insertFuncionario("CNT-EXTRA-IA-" + suffix, "INACTIVO", true);

        // When
        long countAfter = funcionarioRepository.countActiveEligibleVoters();

        // Then — count must not have changed (wrong estado)
        assertThat(countAfter).isEqualTo(countBefore);
    }
}
