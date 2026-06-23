package co.com.votapp.ws.electoral.application.service;

import co.com.votapp.ws.TestcontainersDockerConfig;
import co.com.votapp.ws.electoral.application.command.CreateElectionCommand;
import co.com.votapp.ws.electoral.domain.Candidate;
import co.com.votapp.ws.electoral.domain.Election;
import co.com.votapp.ws.electoral.domain.port.out.CandidateRepositoryPort;
import co.com.votapp.ws.electoral.domain.port.out.ElectionRepositoryPort;
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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link CreateElectionWithCandidatesAppService}.
 *
 * <p>V5: Updated to use funcionarioId instead of numeroOrden; afiliacionPolitica removed.
 * Verifies that election and candidates are persisted correctly in a real PostgreSQL instance.
 */
@SpringBootTest
@Testcontainers
@DisplayName("CreateElectionWithCandidatesAppService - Integration: comprehensive election creation")
class CreateElectionWithCandidatesIT {

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
    private CreateElectionWithCandidatesAppService createElectionWithCandidatesAppService;

    @Autowired
    private ElectionRepositoryPort electionRepository;

    @Autowired
    private CandidateRepositoryPort candidateRepository;

    @Autowired
    private JdbcTemplate jdbc;

    // ── Comprehensive election creation with ballot config ─────────────────────

    @Test
    @DisplayName("Should persist election with permiteVotoBlanco and maxVotosPorElector in DB")
    void createWithCandidates_shouldPersistBallotConfig_inDB() {
        // Given
        String uniqueCodigo = "IT-FULL-" + System.nanoTime();
        LocalDateTime start = LocalDateTime.now().plusDays(5);
        LocalDateTime end = start.plusDays(1);

        CreateElectionCommand electionCmd = new CreateElectionCommand(
                uniqueCodigo, "Full Election IT", null, start, end, false, 3);

        List<CreateElectionWithCandidatesAppService.CandidateCreationData> candidatesData = List.of(
                new CreateElectionWithCandidatesAppService.CandidateCreationData(
                        "Candidato IT-A", 1, "http://foto-a.png", "Bio A", "Propuestas A"),
                new CreateElectionWithCandidatesAppService.CandidateCreationData(
                        "Candidato IT-B", 2, null, null, null)
        );

        // When
        Election election = createElectionWithCandidatesAppService
                .createWithCandidates(electionCmd, candidatesData);

        // Then — election fields
        assertThat(election.id()).isNotNull();
        assertThat(election.codigo()).isEqualTo(uniqueCodigo);
        assertThat(election.permiteVotoBlanco()).isFalse();
        assertThat(election.maxVotosPorElector()).isEqualTo(3);

        // Verify ballot config persisted in DB
        Map<String, Object> dbRow = jdbc.queryForMap(
                "SELECT permite_voto_blanco, max_votos_por_elector FROM elecciones WHERE codigo = ?",
                uniqueCodigo);
        assertThat(dbRow.get("permite_voto_blanco")).isEqualTo(false);
        assertThat(dbRow.get("max_votos_por_elector")).isEqualTo(3);
    }

    @Test
    @DisplayName("Should persist candidates with rich profile fields including nullable fields")
    void createWithCandidates_shouldPersistCandidatesWithRichProfile_inDB() {
        // Given
        String uniqueCodigo = "IT-RICH-" + System.nanoTime();
        LocalDateTime start = LocalDateTime.now().plusDays(5);
        LocalDateTime end = start.plusDays(1);

        CreateElectionCommand electionCmd = new CreateElectionCommand(
                uniqueCodigo, "Rich Candidates IT", null, start, end, true, 2);

        List<CreateElectionWithCandidatesAppService.CandidateCreationData> candidatesData = List.of(
                new CreateElectionWithCandidatesAppService.CandidateCreationData(
                        "Candidato Completo", 1,
                        "http://foto.png", "Una bio completa", "Mis propuestas aqui")
        );

        // When
        Election election = createElectionWithCandidatesAppService
                .createWithCandidates(electionCmd, candidatesData);

        // Then — candidate exists with rich fields
        List<Candidate> candidates = candidateRepository
                .findByEleccionIdOrderByNombre(election.id());

        assertThat(candidates).hasSize(1);
        Candidate saved = candidates.get(0);
        assertThat(saved.nombre()).isEqualTo("Candidato Completo");
        assertThat(saved.funcionarioId()).isEqualTo(1);
        assertThat(saved.fotoUrl()).isEqualTo("http://foto.png");
        assertThat(saved.biografia()).isEqualTo("Una bio completa");
        assertThat(saved.propuestas()).isEqualTo("Mis propuestas aqui");
    }

    @Test
    @DisplayName("Should persist N candidates for a comprehensive election payload")
    void createWithCandidates_shouldPersistAllCandidates_whenMultipleCandidatesProvided() {
        // Given
        String uniqueCodigo = "IT-MULTI-CAND-" + System.nanoTime();
        LocalDateTime start = LocalDateTime.now().plusDays(5);
        LocalDateTime end = start.plusDays(1);

        CreateElectionCommand electionCmd = new CreateElectionCommand(
                uniqueCodigo, "Multi Candidate IT", null, start, end, true, 3);

        List<CreateElectionWithCandidatesAppService.CandidateCreationData> candidatesData = List.of(
                new CreateElectionWithCandidatesAppService.CandidateCreationData(
                        "Candidato Alpha", 1, null, null, null),
                new CreateElectionWithCandidatesAppService.CandidateCreationData(
                        "Candidato Beta", 2, null, null, null),
                new CreateElectionWithCandidatesAppService.CandidateCreationData(
                        "Candidato Gamma", 3, null, null, null)
        );

        // When
        Election election = createElectionWithCandidatesAppService
                .createWithCandidates(electionCmd, candidatesData);

        // Then — all 3 candidates persisted, sorted alphabetically
        List<Candidate> candidates = candidateRepository
                .findByEleccionIdOrderByNombre(election.id());

        assertThat(candidates).hasSize(3);
        assertThat(candidates).extracting(Candidate::nombre)
                .containsExactlyInAnyOrder("Candidato Alpha", "Candidato Beta", "Candidato Gamma");
    }
}
