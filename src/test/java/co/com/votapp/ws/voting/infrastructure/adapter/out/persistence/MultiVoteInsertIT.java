package co.com.votapp.ws.voting.infrastructure.adapter.out.persistence;

import co.com.votapp.ws.TestcontainersDockerConfig;
import co.com.votapp.ws.electoral.application.command.AddCandidateCommand;
import co.com.votapp.ws.electoral.application.command.CreateElectionCommand;
import co.com.votapp.ws.electoral.application.service.CreateElectionWithCandidatesAppService;
import co.com.votapp.ws.electoral.application.service.ElectionTransitionAppService;
import co.com.votapp.ws.electoral.domain.Candidate;
import co.com.votapp.ws.electoral.domain.Election;
import co.com.votapp.ws.electoral.domain.port.out.CandidateRepositoryPort;
import co.com.votapp.ws.electoral.domain.port.out.ElectionRepositoryPort;
import co.com.votapp.ws.voting.application.service.CastVoteAppService;
import co.com.votapp.ws.voting.domain.TokenStatus;
import co.com.votapp.ws.voting.domain.VotingToken;
import co.com.votapp.ws.voting.domain.port.out.VotingTokenRepository;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for multi-row vote insertion (Task 2.6).
 *
 * <p>Verifies:
 * <ol>
 *   <li>After V4 migration drops {@code votos_token_id_key}, a single token can produce N rows
 *       in the {@code votos} table (one per selected candidate).</li>
 *   <li>{@link CastVoteAppService#castVoteByTokenId} inserts exactly N anonymous vote rows
 *       for N distinct candidates in a single atomic call.</li>
 * </ol>
 *
 * <p>Uses real PostgreSQL + Redis via Testcontainers. The full Spring context is loaded
 * so real transactions, Flyway migrations, and the domain wiring are exercised.
 */
@SpringBootTest
@Testcontainers
@DisplayName("Multi-vote insertion - Integration: N votos rows per token after V4 migration")
class MultiVoteInsertIT {

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

    @Autowired private CreateElectionWithCandidatesAppService createElectionWithCandidatesAppService;
    @Autowired private ElectionTransitionAppService electionTransitionAppService;
    @Autowired private CastVoteAppService castVoteAppService;
    @Autowired private VotingTokenRepository votingTokenRepository;
    @Autowired private CandidateRepositoryPort candidateRepository;
    @Autowired private ElectionRepositoryPort electionRepository;
    @Autowired private JdbcTemplate jdbc;

    /**
     * Creates a complete election with candidates, activates it, and issues a token
     * to funcionario ID 1 (seeded by V1 migration). Returns the issued token.
     */
    private VotingToken setupActiveElectionWithToken(int maxVotos, int candidateCount)
            throws Exception {
        String codigo = "IT-MULTIVOTE-" + System.nanoTime();
        LocalDateTime start = LocalDateTime.now().minusHours(1);
        LocalDateTime end = start.plusDays(30);

        // Create election + candidates in one call
        CreateElectionCommand electionCmd = new CreateElectionCommand(
                codigo, "Multi Vote IT Election", null, start, end, false, maxVotos);

        List<CreateElectionWithCandidatesAppService.CandidateCreationData> candidatesData =
                java.util.stream.IntStream.rangeClosed(1, candidateCount)
                        .mapToObj(i -> new CreateElectionWithCandidatesAppService.CandidateCreationData(
                                "Candidato " + i, i, null, null, null))
                        .toList();

        Election election = createElectionWithCandidatesAppService
                .createWithCandidates(electionCmd, candidatesData);

        // Activate: adds synthetic Voto Nulo (not Voto en Blanco since permiteVotoBlanco=false)
        // and issues bulk tokens
        electionTransitionAppService.activate(election.id());

        // Find the token issued to funcionario 1 (seeded by V1 migration)
        return votingTokenRepository.findAllByFuncionarioId(1)
                .stream()
                .filter(t -> t.eleccionId().equals(election.id())
                        && t.status() == TokenStatus.ISSUED)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No ISSUED token found for funcionario 1"));
    }

    // ── Multi-row vote insertion ────────────────────────────────────────────────

    @Test
    @DisplayName("Should insert N votos rows for N selected candidates using a single token")
    void castVote_shouldInsertNVotosRows_forNSelectedCandidates() throws Exception {
        // Given — election with maxVotosPorElector=2, 2 regular candidates
        VotingToken token = setupActiveElectionWithToken(2, 2);
        UUID eleccionId = token.eleccionId();

        // Get the 2 regular candidates (excluding synthetic Voto Nulo)
        List<Candidate> candidates = candidateRepository
                .findByEleccionIdOrderByNombre(eleccionId)
                .stream()
                .filter(c -> !c.esVotoNulo() && !c.esVotoEnBlanco())
                .toList();

        assertThat(candidates).hasSizeGreaterThanOrEqualTo(2);
        List<UUID> selectedIds = List.of(candidates.get(0).id(), candidates.get(1).id());

        // When — cast vote with 2 candidates
        castVoteAppService.castVoteByTokenId(token.id(), selectedIds);

        // Then — exactly 2 rows in votos for this token
        Integer voteCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM votos WHERE token_id = ?",
                Integer.class, token.id());
        assertThat(voteCount).isEqualTo(2);

        // Verify the token is now USED
        VotingToken usedToken = votingTokenRepository.findById(token.id())
                .orElseThrow();
        assertThat(usedToken.status()).isEqualTo(TokenStatus.USED);
    }

    @Test
    @DisplayName("Should insert exactly 1 voto row when only one candidate is selected")
    void castVote_shouldInsertOneVotoRow_whenSingleCandidateSelected() throws Exception {
        // Given — triangulation: single-vote case
        VotingToken token = setupActiveElectionWithToken(3, 3);
        UUID eleccionId = token.eleccionId();

        List<Candidate> candidates = candidateRepository
                .findByEleccionIdOrderByNombre(eleccionId)
                .stream()
                .filter(c -> !c.esVotoNulo() && !c.esVotoEnBlanco())
                .toList();

        assertThat(candidates).hasSizeGreaterThanOrEqualTo(1);
        List<UUID> selectedIds = List.of(candidates.get(0).id());

        // When — cast vote with only 1 candidate
        castVoteAppService.castVoteByTokenId(token.id(), selectedIds);

        // Then — exactly 1 row in votos for this token
        Integer voteCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM votos WHERE token_id = ?",
                Integer.class, token.id());
        assertThat(voteCount).isEqualTo(1);
    }

    @Test
    @DisplayName("votos.token_id does NOT have a UNIQUE constraint after V4 migration")
    void votos_tokenId_shouldNotHaveUniqueConstraint_afterV4Migration() {
        // This test verifies that V4 migration successfully dropped votos_token_id_key.
        // If the UNIQUE constraint still existed, the 2-row insert test above would fail.
        Integer uniqueCount = jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.key_column_usage kcu
                JOIN information_schema.table_constraints tc
                  ON kcu.constraint_name = tc.constraint_name
                  AND kcu.table_schema = tc.table_schema
                WHERE tc.table_schema = 'public'
                  AND kcu.table_name = 'votos'
                  AND kcu.column_name = 'token_id'
                  AND tc.constraint_type = 'UNIQUE'
                """,
                Integer.class);
        assertThat(uniqueCount)
                .as("votos.token_id must NOT have a UNIQUE constraint after V4 migration")
                .isEqualTo(0);
    }

    @Test
    @DisplayName("elecciones should have permite_voto_blanco and max_votos_por_elector columns after V4")
    void elecciones_shouldHaveBallotConfigColumns_afterV4Migration() {
        // Verify V4 migration added ballot config columns
        List<String> cols = jdbc.queryForList(
                "SELECT column_name FROM information_schema.columns "
                        + "WHERE table_schema = 'public' AND table_name = 'elecciones' "
                        + "ORDER BY ordinal_position",
                String.class);
        assertThat(cols).contains("permite_voto_blanco", "max_votos_por_elector");
    }

    @Test
    @DisplayName("candidatos should have rich profile columns after V4 migration")
    void candidatos_shouldHaveRichProfileColumns_afterV4Migration() {
        // Verify V4 migration added rich profile columns to candidatos
        List<String> cols = jdbc.queryForList(
                "SELECT column_name FROM information_schema.columns "
                        + "WHERE table_schema = 'public' AND table_name = 'candidatos' "
                        + "ORDER BY ordinal_position",
                String.class);
        assertThat(cols).contains("foto_url", "biografia", "propuestas", "funcionario_id");
        assertThat(cols).doesNotContain("afiliacion_politica", "numero_orden");
    }
}
