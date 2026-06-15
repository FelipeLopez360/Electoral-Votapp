package co.com.votapp.ws.voting.infrastructure.adapter.out.persistence;

import co.com.votapp.ws.TestcontainersDockerConfig;
import co.com.votapp.ws.electoral.domain.port.out.CensoRepositoryPort;
import co.com.votapp.ws.electoral.application.service.ElectionTransitionAppService;
import co.com.votapp.ws.voting.domain.TokenStatus;
import co.com.votapp.ws.voting.domain.VotingToken;
import co.com.votapp.ws.voting.domain.port.out.VotingTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

/**
 * Integration tests for {@link VotingTokenRepositoryAdapter#saveAllIssued} with a real PostgreSQL.
 *
 * <p>TDD RED → GREEN: written before production changes to prove the contracts:
 * <ol>
 *   <li>Duplicate tokens are silently skipped (ON CONFLICT DO NOTHING), adapter returns only inserted.</li>
 *   <li>A complete retry returns 0 new tokens and does not fail.</li>
 *   <li>If issuance fails mid-activation, the @Transactional boundary rolls back:
 *       election stays PROGRAMADA and zero partial tokens exist.</li>
 * </ol>
 *
 * <p>Uses {@link ElectionTransitionAppService} (via Spring context) for the rollback test so the
 * real {@code @Transactional} proxy is active.
 */
@SpringBootTest
@Testcontainers
@DisplayName("VotingTokenRepositoryAdapter - Integration tests with real PostgreSQL")
class VotingTokenRepositoryAdapterIT {

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
    private VotingTokenRepository tokenRepository;

    @Autowired
    private ElectionTransitionAppService electionTransitionAppService;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private CensoRepositoryPort censoRepositoryPort;

    private UUID eleccionId;
    private Integer funcionarioId;
    private Integer funcionarioId2;

    @BeforeEach
    void setUp() {
        // Seed one test election per run (idempotent via ON CONFLICT DO NOTHING)
        jdbc.update("""
                INSERT INTO elecciones (id, codigo, nombre, estado, fecha_inicio, fecha_fin)
                VALUES (gen_random_uuid(), 'IT-TOKEN-ADAPTER', 'IT Token Adapter Test', 'PROGRAMADA',
                        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '1 day')
                ON CONFLICT (codigo) DO NOTHING
                """);

        eleccionId = jdbc.queryForObject(
                "SELECT id FROM elecciones WHERE codigo = 'IT-TOKEN-ADAPTER'",
                UUID.class);

        // Ensure election is PROGRAMADA for rollback test reproducibility
        jdbc.update("UPDATE elecciones SET estado = 'PROGRAMADA' WHERE id = ?", eleccionId);

        // Grab two real funcionarios from seed data
        List<Integer> ids = jdbc.queryForList(
                "SELECT id FROM funcionarios WHERE estado_laboral = 'ACTIVO' AND puede_votar = true LIMIT 2",
                Integer.class);
        assertThat(ids).as("Seed data must have at least 2 ACTIVO+puede_votar funcionarios")
                .hasSizeGreaterThanOrEqualTo(2);
        funcionarioId = ids.get(0);
        funcionarioId2 = ids.get(1);

        // Clean token state before each test
        jdbc.update("DELETE FROM tokens_votacion WHERE eleccion_id = ?", eleccionId);
    }

    // ── saveAllIssued — duplicate skipping ───────────────────────────────────

    @Test
    @DisplayName("Should skip duplicate on second saveAllIssued call and return only newly inserted")
    void saveAllIssued_shouldSkipDuplicate_andReturnOnlyInserted() {
        // Given — insert the first token
        VotingToken firstToken = buildToken(UUID.randomUUID(), Long.valueOf(funcionarioId));
        List<VotingToken> firstResult = tokenRepository.saveAllIssued(List.of(firstToken));
        assertThat(firstResult).hasSize(1);
        assertThat(tokenCount()).isEqualTo(1);

        // When — retry with the same funcionario (duplicate) plus a new one
        VotingToken duplicateToken = buildToken(UUID.randomUUID(), Long.valueOf(funcionarioId));
        VotingToken newToken = buildToken(UUID.randomUUID(), Long.valueOf(funcionarioId2));
        List<VotingToken> secondResult = tokenRepository.saveAllIssued(List.of(duplicateToken, newToken));

        // Then — only the new token was inserted; duplicate was silently skipped
        assertThat(secondResult).hasSize(1);
        assertThat(secondResult.get(0).funcionarioId()).isEqualTo(Long.valueOf(funcionarioId2));
        assertThat(tokenCount()).isEqualTo(2);
    }

    // ── saveAllIssued — complete retry creates 0 new tokens ──────────────────

    @Test
    @DisplayName("Should return empty list on complete retry when all tokens already exist")
    void saveAllIssued_shouldReturnEmpty_andNotFailOnCompleteRetry() {
        // Given — issue tokens for both funcionarios
        VotingToken token1 = buildToken(UUID.randomUUID(), Long.valueOf(funcionarioId));
        VotingToken token2 = buildToken(UUID.randomUUID(), Long.valueOf(funcionarioId2));
        List<VotingToken> firstResult = tokenRepository.saveAllIssued(List.of(token1, token2));
        assertThat(firstResult).hasSize(2);
        assertThat(tokenCount()).isEqualTo(2);

        // When — complete retry with different UUIDs for same (eleccion, funcionario) pairs
        VotingToken retryToken1 = buildToken(UUID.randomUUID(), Long.valueOf(funcionarioId));
        VotingToken retryToken2 = buildToken(UUID.randomUUID(), Long.valueOf(funcionarioId2));
        List<VotingToken> retryResult = tokenRepository.saveAllIssued(List.of(retryToken1, retryToken2));

        // Then — retry does NOT throw, returns empty list, no new rows created
        assertThat(retryResult).isEmpty();
        assertThat(tokenCount()).isEqualTo(2); // unchanged
    }

    // ── Rollback — failed issuance leaves no partial tokens ──────────────────

    @Test
    @DisplayName("Should rollback: election stays PROGRAMADA and no tokens exist when issuance fails mid-activation")
    void activate_shouldRollback_whenBulkIssueFailsMidway() {
        // Given — censoRepositoryPort is mocked; force it to throw during activation
        // so that ElectionTransitionAppService.activate() rolls back the whole tx
        doThrow(new RuntimeException("Simulated issuance failure during census read"))
                .when(censoRepositoryPort).findAllFuncionarioIdsByEleccionId(any());

        // Ensure election starts as PROGRAMADA
        String estadoBefore = jdbc.queryForObject(
                "SELECT estado FROM elecciones WHERE id = ?", String.class, eleccionId);
        assertThat(estadoBefore).isEqualTo("PROGRAMADA");

        // When — activation triggers ActivateElectionUseCase then BulkIssueTokensUseCase;
        // the RuntimeException from censoRepositoryPort bubbles up through the @Transactional boundary
        assertThatThrownBy(() -> electionTransitionAppService.activate(eleccionId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Simulated issuance failure during census read");

        // Then — full rollback: election stays PROGRAMADA, no tokens were persisted
        String estadoAfter = jdbc.queryForObject(
                "SELECT estado FROM elecciones WHERE id = ?", String.class, eleccionId);
        assertThat(estadoAfter)
                .as("Election must remain PROGRAMADA after rollback")
                .isEqualTo("PROGRAMADA");

        assertThat(tokenCount())
                .as("No partial tokens must exist after rollback")
                .isEqualTo(0);
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private VotingToken buildToken(UUID id, Long funcionarioId) {
        return new VotingToken(id, eleccionId, funcionarioId,
                "sha256hash-" + funcionarioId + "-" + id,
                TokenStatus.ISSUED, Instant.now());
    }

    private int tokenCount() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM tokens_votacion WHERE eleccion_id = ?",
                Integer.class, eleccionId);
        return count != null ? count : 0;
    }
}
