package co.com.votapp.ws.electoral.infrastructure.adapter.out.persistence;

import co.com.votapp.ws.TestcontainersDockerConfig;
import co.com.votapp.ws.electoral.domain.model.ResultAggregate;
import co.com.votapp.ws.electoral.domain.port.out.ResultsRepositoryPort;
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

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link ResultsRepositoryAdapter}.
 *
 * <p>Verifies the native aggregation SQL against a real PostgreSQL instance via Testcontainers.
 * Seeds a finalized election with known votes (real, blank, null) and asserts the aggregate
 * counts, participation, and eligible voter counts match expectations.
 *
 * <p>Schema alignment (V1 + V2 + V3):
 * <ul>
 *   <li>{@code funcionario_id} is INTEGER (SERIAL FK to {@code funcionarios.id}) — seed data provides IDs 1–4</li>
 *   <li>{@code censo_electoral} column is {@code created_at} (not {@code creado_en})</li>
 *   <li>{@code participacion_electoral} column is {@code created_at} (not {@code participo_en})</li>
 *   <li>{@code votos} column is {@code created_at} (not {@code emitido_en}); {@code token_id} is NOT NULL UNIQUE</li>
 * </ul>
 *
 * <p>TDD: tests written to verify the existing implementation produces correct results
 * from actual database records.
 */
@SpringBootTest
@Testcontainers
@DisplayName("ResultsRepositoryAdapter - Integration tests with real PostgreSQL")
class ResultsRepositoryAdapterIT {

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
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private ResultsRepositoryPort resultsRepositoryPort;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID electionId;
    private UUID blankCandidateId;
    private UUID nullCandidateId;
    private UUID realCandidateAId;
    private UUID realCandidateBId;

    /**
     * Seeds a complete FINALIZADA election scenario for each test.
     *
     * <p>Schema notes:
     * <ul>
     *   <li>{@code funcionario_id} is INTEGER — use seeded funcionario IDs (1, 2, 3, 4) from V1 seed data</li>
     *   <li>{@code votos.token_id} is NOT NULL UNIQUE — must insert one token per vote</li>
     *   <li>Column names follow V1/V2 schema: {@code created_at} (not {@code creado_en} / {@code participo_en})</li>
     * </ul>
     */
    @BeforeEach
    void setUp() {
        // Clean all dependent tables; CASCADE propagates to votos, tokens_votacion, etc.
        jdbcTemplate.execute(
                "TRUNCATE TABLE auditoria_eventos, participacion_electoral, votos, " +
                "tokens_votacion, censo_electoral, candidatos, elecciones CASCADE"
        );

        electionId = UUID.randomUUID();
        blankCandidateId = UUID.randomUUID();
        nullCandidateId = UUID.randomUUID();
        realCandidateAId = UUID.randomUUID();
        realCandidateBId = UUID.randomUUID();

        // --- Seeded funcionario IDs from V1 seed data (INTEGER) ---
        // EMP001 → id=1, EMP002 → id=2, EMP003 → id=3, EMP004 → id=4
        int funcionario1 = 1;
        int funcionario2 = 2;
        int funcionario3 = 3;
        int funcionario4 = 4;
        int funcionario5 = 3; // re-use id=3 in censo (they are unique per election, not globally)
        // Note: only 4 seeded funcionarios exist (ids 1-4); we use 4 for census + 1 abstainer modelled
        // by having 5 census entries requires a 5th funcionario. Since only 4 exist in seed data,
        // we model: 4 eligible voters, 3 voted, 1 abstained.

        // Election in FINALIZADA state
        jdbcTemplate.update(
                "INSERT INTO elecciones (id, codigo, nombre, estado, fecha_inicio, fecha_fin) " +
                        "VALUES (?, ?, ?, 'FINALIZADA', ?, ?)",
                electionId, "ELEC-IT-001", "Eleccion IT Test",
                LocalDateTime.now().minusDays(5), LocalDateTime.now().minusDays(1)
        );

        // Candidates: 2 real + blank + null (es_voto_nulo added by V3 migration)
        jdbcTemplate.update(
                "INSERT INTO candidatos (id, eleccion_id, nombre, numero_orden, es_voto_en_blanco, es_voto_nulo) " +
                        "VALUES (?, ?, ?, ?, ?, ?)",
                realCandidateAId, electionId, "Candidate A", 1, false, false
        );
        jdbcTemplate.update(
                "INSERT INTO candidatos (id, eleccion_id, nombre, numero_orden, es_voto_en_blanco, es_voto_nulo) " +
                        "VALUES (?, ?, ?, ?, ?, ?)",
                realCandidateBId, electionId, "Candidate B", 2, false, false
        );
        jdbcTemplate.update(
                "INSERT INTO candidatos (id, eleccion_id, nombre, numero_orden, es_voto_en_blanco, es_voto_nulo) " +
                        "VALUES (?, ?, ?, ?, ?, ?)",
                blankCandidateId, electionId, "Voto en Blanco", 0, true, false
        );
        jdbcTemplate.update(
                "INSERT INTO candidatos (id, eleccion_id, nombre, numero_orden, es_voto_en_blanco, es_voto_nulo) " +
                        "VALUES (?, ?, ?, ?, ?, ?)",
                nullCandidateId, electionId, "Voto Nulo", -1, false, true
        );

        // --- Tokens de votación (required by votos.token_id NOT NULL UNIQUE FK) ---
        // One token per funcionario per election (ISSUED status)
        UUID tokenForFun1 = UUID.randomUUID();
        UUID tokenForFun2 = UUID.randomUUID();
        UUID tokenForFun3 = UUID.randomUUID();
        // hash must be unique (VARCHAR 64)
        jdbcTemplate.update(
                "INSERT INTO tokens_votacion (id, eleccion_id, funcionario_id, token_hash, status) " +
                        "VALUES (?, ?, ?, ?, 'USED')",
                tokenForFun1, electionId, funcionario1,
                "hash_fun1_" + tokenForFun1.toString().substring(0, 20)
        );
        jdbcTemplate.update(
                "INSERT INTO tokens_votacion (id, eleccion_id, funcionario_id, token_hash, status) " +
                        "VALUES (?, ?, ?, ?, 'USED')",
                tokenForFun2, electionId, funcionario2,
                "hash_fun2_" + tokenForFun2.toString().substring(0, 20)
        );
        jdbcTemplate.update(
                "INSERT INTO tokens_votacion (id, eleccion_id, funcionario_id, token_hash, status) " +
                        "VALUES (?, ?, ?, ?, 'ISSUED')",
                tokenForFun3, electionId, funcionario3,
                "hash_fun3_" + tokenForFun3.toString().substring(0, 20)
        );

        // 4 eligible voters in census (funcionario IDs 1-4, INTEGER)
        for (int fId : new int[]{funcionario1, funcionario2, funcionario3, funcionario4}) {
            jdbcTemplate.update(
                    "INSERT INTO censo_electoral (id, eleccion_id, funcionario_id) " +
                            "VALUES (?, ?, ?)",
                    UUID.randomUUID(), electionId, fId
            );
        }

        // 3 votes: A=1, B=1, blank=1 (one voter — funcionario4 — abstained, no token/vote)
        // Uses token_id NOT NULL UNIQUE → each vote references a distinct token
        jdbcTemplate.update(
                "INSERT INTO votos (id, eleccion_id, candidato_id, token_id) VALUES (?, ?, ?, ?)",
                UUID.randomUUID(), electionId, realCandidateAId, tokenForFun1
        );
        jdbcTemplate.update(
                "INSERT INTO votos (id, eleccion_id, candidato_id, token_id) VALUES (?, ?, ?, ?)",
                UUID.randomUUID(), electionId, realCandidateBId, tokenForFun2
        );
        // Use a separate token (status ISSUED for blank-voter — simulates they voted blank)
        UUID tokenForBlankVoter = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO tokens_votacion (id, eleccion_id, funcionario_id, token_hash, status) " +
                        "VALUES (?, ?, ?, ?, 'USED')",
                tokenForBlankVoter, electionId, funcionario4,
                "hash_blank_" + tokenForBlankVoter.toString().substring(0, 19)
        );
        jdbcTemplate.update(
                "INSERT INTO votos (id, eleccion_id, candidato_id, token_id) VALUES (?, ?, ?, ?)",
                UUID.randomUUID(), electionId, blankCandidateId, tokenForBlankVoter
        );

        // 3 participation records (funcionarios 1, 2, 4 voted; funcionario 3 abstained)
        for (int fId : new int[]{funcionario1, funcionario2, funcionario4}) {
            jdbcTemplate.update(
                    "INSERT INTO participacion_electoral (id, eleccion_id, funcionario_id, completado, completed_at) " +
                            "VALUES (?, ?, ?, true, NOW())",
                    UUID.randomUUID(), electionId, fId
            );
        }
    }

    @Test
    @DisplayName("Should return correct total vote count for all candidates")
    void aggregate_shouldReturnCorrectTotalVotes_whenElectionHasVotes() {
        // When
        ResultAggregate aggregate = resultsRepositoryPort.aggregate(electionId);

        // Then — 3 votes total (A=1, B=1, blank=1, null=0)
        assertThat(aggregate.totalVotes()).isEqualTo(3L);
    }

    @Test
    @DisplayName("Should return correct blank vote count")
    void aggregate_shouldReturnCorrectBlankVotes_whenBlankVotesExist() {
        // When
        ResultAggregate aggregate = resultsRepositoryPort.aggregate(electionId);

        // Then — 1 blank vote
        assertThat(aggregate.blankVotes()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should return zero null votes when no null votes were cast")
    void aggregate_shouldReturnZeroNullVotes_whenNoNullVotesCast() {
        // When
        ResultAggregate aggregate = resultsRepositoryPort.aggregate(electionId);

        // Then — no null votes were inserted
        assertThat(aggregate.nullVotes()).isEqualTo(0L);
    }

    @Test
    @DisplayName("Should return correct participation count from participacion_electoral")
    void aggregate_shouldReturnCorrectParticipationCount() {
        // When
        ResultAggregate aggregate = resultsRepositoryPort.aggregate(electionId);

        // Then — 3 participation records inserted (1 abstained)
        assertThat(aggregate.participationCount()).isEqualTo(3L);
    }

    @Test
    @DisplayName("Should return correct eligible voter count from censo_electoral")
    void aggregate_shouldReturnCorrectEligibleCount() {
        // When
        ResultAggregate aggregate = resultsRepositoryPort.aggregate(electionId);

        // Then — 4 censo records inserted (ids 1, 2, 3, 4)
        assertThat(aggregate.eligibleCount()).isEqualTo(4L);
    }

    @Test
    @DisplayName("Should return candidate counts with correct vote distribution")
    void aggregate_shouldReturnPerCandidateVoteCounts_withCorrectDistribution() {
        // When
        ResultAggregate aggregate = resultsRepositoryPort.aggregate(electionId);

        // Then — find Candidate A with 1 vote
        ResultAggregate.CandidateCount candidateA = aggregate.candidateCounts().stream()
                .filter(cc -> cc.candidateId().equals(realCandidateAId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Candidate A not found in aggregate"));

        assertThat(candidateA.votes()).isEqualTo(1L);
        assertThat(candidateA.esVotoEnBlanco()).isFalse();
        assertThat(candidateA.esVotoNulo()).isFalse();
    }

    @Test
    @DisplayName("Should sum candidate vote counts to match total votes")
    void aggregate_shouldHaveConsistentVoteSum_acrossCandidateCounts() {
        // When
        ResultAggregate aggregate = resultsRepositoryPort.aggregate(electionId);

        // Then — sum of per-candidate counts matches totalVotes
        long sumFromCounts = aggregate.candidateCounts().stream()
                .mapToLong(ResultAggregate.CandidateCount::votes)
                .sum();
        assertThat(sumFromCounts).isEqualTo(aggregate.totalVotes());
    }
}
