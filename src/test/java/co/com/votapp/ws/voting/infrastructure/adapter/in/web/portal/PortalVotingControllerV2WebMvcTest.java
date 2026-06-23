package co.com.votapp.ws.voting.infrastructure.adapter.in.web.portal;

import co.com.votapp.ws.auth.domain.port.out.PortalSessionPort;
import co.com.votapp.ws.common.config.SecurityConfig;
import co.com.votapp.ws.common.exception.DomainException;
import co.com.votapp.ws.common.exception.GlobalExceptionHandler;
import co.com.votapp.ws.common.exception.NotFoundException;
import co.com.votapp.ws.electoral.domain.Candidate;
import co.com.votapp.ws.electoral.domain.Election;
import co.com.votapp.ws.electoral.domain.ElectionStatus;
import co.com.votapp.ws.electoral.domain.port.out.CandidateRepositoryPort;
import co.com.votapp.ws.electoral.domain.port.out.ElectionRepositoryPort;
import co.com.votapp.ws.voting.application.service.CastVoteAppService;
import co.com.votapp.ws.voting.domain.TokenStatus;
import co.com.votapp.ws.voting.domain.VotingToken;
import co.com.votapp.ws.voting.domain.port.out.ParticipacionRepositoryPort;
import co.com.votapp.ws.voting.domain.port.out.VotingTokenRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * WebMvc slice tests for updated {@link PortalVotingController} endpoints (Phase 2 / Task 2.4).
 *
 * <p>Verifies:
 * <ul>
 *   <li>GET /api/v1/portal/ballot/{eleccionId} exposes {@code maxVotosPorElector},
 *       {@code permiteVotoBlanco}, and rich candidate fields</li>
 *   <li>POST /api/v1/portal/votar/{eleccionId} accepts JSON body {@code {candidatoIds:[...]}}
 *       instead of path parameter</li>
 * </ul>
 */
@DisplayName("PortalVotingController - Phase 2 ballot config and multi-vote body endpoint")
@WebMvcTest(PortalVotingController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class PortalVotingControllerV2WebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private CastVoteAppService castVoteAppService;
    @MockitoBean private CandidateRepositoryPort candidateRepository;
    @MockitoBean private VotingTokenRepository votingTokenRepository;
    @MockitoBean private ElectionRepositoryPort electionRepository;
    @MockitoBean private ParticipacionRepositoryPort participacionRepository;
    @MockitoBean private PortalSessionPort sessionPort;

    private static final UUID ELECCION_ID  = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final UUID CANDIDATO_ID = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
    private static final Integer FUNCIONARIO_ID = 77;
    private static final String VALID_SESSION = "valid-session-777";

    private VotingToken issuedToken() {
        return new VotingToken(
                UUID.randomUUID(), ELECCION_ID, 77L, "hash-777",
                TokenStatus.ISSUED, Instant.now());
    }

    private Election multiVoteElection() {
        return new Election(ELECCION_ID, "ELEC-MULTI", "Multi Vote Election",
                ElectionStatus.ACTIVA,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(30),
                true, 3);
    }

    // ── GET /api/v1/portal/ballot/{eleccionId} — ballot config fields ──────

    @Test
    @DisplayName("Should expose maxVotosPorElector and permiteVotoBlanco in ballot response")
    void portalBallot_shouldExposeBallotConfig_inResponse() throws Exception {
        // Given
        when(sessionPort.getFuncionarioIdFromSession(VALID_SESSION))
                .thenReturn(Optional.of(FUNCIONARIO_ID));
        when(votingTokenRepository.findIssuedByFuncionarioAndEleccion(FUNCIONARIO_ID, ELECCION_ID))
                .thenReturn(Optional.of(issuedToken()));
        when(electionRepository.findById(ELECCION_ID))
                .thenReturn(Optional.of(multiVoteElection()));
        when(candidateRepository.findByEleccionIdOrderByNombre(ELECCION_ID))
                .thenReturn(List.of(
                        new Candidate(CANDIDATO_ID, ELECCION_ID, "Candidato A", false, false, 1,
                                "http://foto.png", "Bio del candidato", "Mis propuestas"),
                        new Candidate(UUID.randomUUID(), ELECCION_ID, "Voto en Blanco", true, false,
                                null, null, null, null)
                ));

        // When & Then
        mockMvc.perform(get("/api/v1/portal/ballot/{eleccionId}", ELECCION_ID)
                        .header("Authorization", "Bearer " + VALID_SESSION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxVotosPorElector").value(3))
                .andExpect(jsonPath("$.permiteVotoBlanco").value(true))
                .andExpect(jsonPath("$.candidates[0].fotoUrl").value("http://foto.png"))
                .andExpect(jsonPath("$.candidates[0].biografia").value("Bio del candidato"))
                .andExpect(jsonPath("$.candidates[0].propuestas").value("Mis propuestas"))
                .andExpect(jsonPath("$.candidates[0].afiliacionPolitica").doesNotExist())
                .andExpect(jsonPath("$.candidates[1].fotoUrl").doesNotExist());
    }

    @Test
    @DisplayName("Should expose maxVotosPorElector=1 when election has default single-vote config")
    void portalBallot_shouldExposeSingleVoteConfig_whenMaxIsOne() throws Exception {
        // Given — triangulation: single vote election
        Election singleVoteElection = new Election(ELECCION_ID, "ELEC-SINGLE", "Single Vote",
                ElectionStatus.ACTIVA,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(30),
                false, 1);
        when(sessionPort.getFuncionarioIdFromSession(VALID_SESSION))
                .thenReturn(Optional.of(FUNCIONARIO_ID));
        when(votingTokenRepository.findIssuedByFuncionarioAndEleccion(FUNCIONARIO_ID, ELECCION_ID))
                .thenReturn(Optional.of(issuedToken()));
        when(electionRepository.findById(ELECCION_ID))
                .thenReturn(Optional.of(singleVoteElection));
        when(candidateRepository.findByEleccionIdOrderByNombre(ELECCION_ID))
                .thenReturn(List.of(
                        new Candidate(CANDIDATO_ID, ELECCION_ID, "Candidato Solo", false, false,
                                1, null, null, null)
                ));

        // When & Then
        mockMvc.perform(get("/api/v1/portal/ballot/{eleccionId}", ELECCION_ID)
                        .header("Authorization", "Bearer " + VALID_SESSION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxVotosPorElector").value(1))
                .andExpect(jsonPath("$.permiteVotoBlanco").value(false));
    }

    // ── POST /api/v1/portal/votar/{eleccionId} — JSON body with candidatoIds ─

    @Test
    @DisplayName("Should return 204 when multi-vote ballot is submitted via JSON body")
    void votarV2_shouldReturn204_whenMultiVoteBallotIsSubmitted() throws Exception {
        // Given
        UUID cand2 = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
        when(sessionPort.getFuncionarioIdFromSession(VALID_SESSION))
                .thenReturn(Optional.of(FUNCIONARIO_ID));
        when(votingTokenRepository.findIssuedByFuncionarioAndEleccion(FUNCIONARIO_ID, ELECCION_ID))
                .thenReturn(Optional.of(issuedToken()));

        String body = """
                { "candidatoIds": ["%s", "%s"] }
                """.formatted(CANDIDATO_ID, cand2);

        // When & Then
        mockMvc.perform(post("/api/v1/portal/votar/{eleccionId}", ELECCION_ID)
                        .header("Authorization", "Bearer " + VALID_SESSION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());

        // Verify list delegation
        verify(castVoteAppService).castVoteByTokenId(any(), any());
    }

    @Test
    @DisplayName("Should return 204 when single-vote ballot is submitted via JSON body")
    void votarV2_shouldReturn204_whenSingleCandidateBodyIsSubmitted() throws Exception {
        // Given — triangulation: single candidatoId in body
        when(sessionPort.getFuncionarioIdFromSession(VALID_SESSION))
                .thenReturn(Optional.of(FUNCIONARIO_ID));
        when(votingTokenRepository.findIssuedByFuncionarioAndEleccion(FUNCIONARIO_ID, ELECCION_ID))
                .thenReturn(Optional.of(issuedToken()));

        String body = """
                { "candidatoIds": ["%s"] }
                """.formatted(CANDIDATO_ID);

        // When & Then
        mockMvc.perform(post("/api/v1/portal/votar/{eleccionId}", ELECCION_ID)
                        .header("Authorization", "Bearer " + VALID_SESSION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should return 401 when no Authorization header for new votar endpoint")
    void votarV2_shouldReturn401_whenNoAuthHeader() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/portal/votar/{eleccionId}", ELECCION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"candidatoIds\": [] }"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 404 when funcionario has no token for the election (new votar endpoint)")
    void votarV2_shouldReturn404_whenNoTokenAssigned() throws Exception {
        // Given
        when(sessionPort.getFuncionarioIdFromSession(VALID_SESSION))
                .thenReturn(Optional.of(FUNCIONARIO_ID));
        when(votingTokenRepository.findIssuedByFuncionarioAndEleccion(FUNCIONARIO_ID, ELECCION_ID))
                .thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(post("/api/v1/portal/votar/{eleccionId}", ELECCION_ID)
                        .header("Authorization", "Bearer " + VALID_SESSION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"candidatoIds\": [\"" + CANDIDATO_ID + "\"] }"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 409 when domain throws DomainException (blank + other candidate)")
    void votarV2_shouldReturn409_whenBlankVoteExclusivityViolated() throws Exception {
        // Given
        UUID blankId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        when(sessionPort.getFuncionarioIdFromSession(VALID_SESSION))
                .thenReturn(Optional.of(FUNCIONARIO_ID));
        when(votingTokenRepository.findIssuedByFuncionarioAndEleccion(FUNCIONARIO_ID, ELECCION_ID))
                .thenReturn(Optional.of(issuedToken()));
        doThrow(new DomainException("blank vote candidate cannot be combined with other candidates"))
                .when(castVoteAppService).castVoteByTokenId(any(), any());

        String body = """
                { "candidatoIds": ["%s", "%s"] }
                """.formatted(blankId, CANDIDATO_ID);

        // When & Then
        mockMvc.perform(post("/api/v1/portal/votar/{eleccionId}", ELECCION_ID)
                        .header("Authorization", "Bearer " + VALID_SESSION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "blank vote candidate cannot be combined with other candidates"));
    }
}
