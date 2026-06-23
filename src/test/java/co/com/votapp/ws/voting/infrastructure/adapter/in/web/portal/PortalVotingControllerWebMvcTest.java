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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * WebMvc slice tests for {@link PortalVotingController}.
 *
 * <p>Covers all three portal voting endpoints:
 * <ul>
 *   <li>GET /api/v1/portal/mis-elecciones (Req 2.1, 2.2, 2.3)</li>
 *   <li>GET /api/v1/portal/ballot/{eleccionId}</li>
 *   <li>POST /api/v1/portal/votar/{eleccionId}/{candidatoId} (Req 3.1, 3.3)</li>
 * </ul>
 */
@DisplayName("PortalVotingController - Portal voting endpoints")
@WebMvcTest(PortalVotingController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class PortalVotingControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CastVoteAppService castVoteAppService;

    @MockitoBean
    private CandidateRepositoryPort candidateRepository;

    @MockitoBean
    private VotingTokenRepository votingTokenRepository;

    @MockitoBean
    private ElectionRepositoryPort electionRepository;

    @MockitoBean
    private ParticipacionRepositoryPort participacionRepository;

    @MockitoBean
    private PortalSessionPort sessionPort;

    private static final UUID ELECCION_ID  = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID CANDIDATO_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final Integer FUNCIONARIO_ID = 42;
    private static final String VALID_SESSION = "valid-session-token";

    // ─── Helper factories ──────────────────────────────────────────────────

    private VotingToken issuedToken() {
        return new VotingToken(
                UUID.randomUUID(), ELECCION_ID, 42L, "hash",
                TokenStatus.ISSUED, Instant.now());
    }

    private Election activaElection() {
        return new Election(ELECCION_ID, "ELEC-TEST", "Elección de Prueba",
                ElectionStatus.ACTIVA,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(30),
                true, 1);
    }

    // ────────────────────────────────────────────────────────────────────────
    // GET /api/v1/portal/mis-elecciones
    // Req 2.1: dashboard shows assigned elections; Req 2.2: shows voted status
    // Req 2.3: never exposes rawToken
    // ────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return 200 with election list including yaVoto and estadoEleccion when session is valid")
    void misElecciones_shouldReturn200WithElectionList_whenSessionIsValid() throws Exception {
        // Given
        when(sessionPort.getFuncionarioIdFromSession(VALID_SESSION))
                .thenReturn(Optional.of(FUNCIONARIO_ID));
        when(votingTokenRepository.findAllByFuncionarioId(FUNCIONARIO_ID))
                .thenReturn(List.of(issuedToken()));
        when(electionRepository.findById(ELECCION_ID))
                .thenReturn(Optional.of(activaElection()));
        when(participacionRepository.hasParticipated(ELECCION_ID, 42L))
                .thenReturn(false);

        // When & Then
        mockMvc.perform(get("/api/v1/portal/mis-elecciones")
                        .header("Authorization", "Bearer " + VALID_SESSION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.elecciones").isArray())
                .andExpect(jsonPath("$.elecciones.length()").value(1))
                .andExpect(jsonPath("$.elecciones[0].eleccionId").value(ELECCION_ID.toString()))
                .andExpect(jsonPath("$.elecciones[0].nombre").value("Elección de Prueba"))
                .andExpect(jsonPath("$.elecciones[0].estadoEleccion").value("ACTIVA"))
                .andExpect(jsonPath("$.elecciones[0].estadoToken").value("ISSUED"))
                .andExpect(jsonPath("$.elecciones[0].yaVoto").value(false));
    }

    @Test
    @DisplayName("Should return yaVoto=true when funcionario already participated in election")
    void misElecciones_shouldReturnYaVotoTrue_whenFuncionarioAlreadyParticipated() throws Exception {
        // Given — funcionario has already voted
        when(sessionPort.getFuncionarioIdFromSession(VALID_SESSION))
                .thenReturn(Optional.of(FUNCIONARIO_ID));
        when(votingTokenRepository.findAllByFuncionarioId(FUNCIONARIO_ID))
                .thenReturn(List.of(new VotingToken(
                        UUID.randomUUID(), ELECCION_ID, 42L, "hash",
                        TokenStatus.USED, Instant.now())));
        when(electionRepository.findById(ELECCION_ID))
                .thenReturn(Optional.of(activaElection()));
        when(participacionRepository.hasParticipated(ELECCION_ID, 42L))
                .thenReturn(true);

        // When & Then
        mockMvc.perform(get("/api/v1/portal/mis-elecciones")
                        .header("Authorization", "Bearer " + VALID_SESSION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.elecciones[0].yaVoto").value(true))
                .andExpect(jsonPath("$.elecciones[0].estadoToken").value("USED"));
    }

    @Test
    @DisplayName("Should return empty elecciones list when funcionario has no tokens")
    void misElecciones_shouldReturnEmptyList_whenNoTokensAssigned() throws Exception {
        // Given
        when(sessionPort.getFuncionarioIdFromSession(VALID_SESSION))
                .thenReturn(Optional.of(FUNCIONARIO_ID));
        when(votingTokenRepository.findAllByFuncionarioId(FUNCIONARIO_ID))
                .thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/v1/portal/mis-elecciones")
                        .header("Authorization", "Bearer " + VALID_SESSION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.elecciones").isEmpty());
    }

    @Test
    @DisplayName("Should return 401 when no Authorization header is present for mis-elecciones")
    void misElecciones_shouldReturn401_whenNoAuthHeader() throws Exception {
        // When & Then — no Bearer token, filter returns 401
        mockMvc.perform(get("/api/v1/portal/mis-elecciones"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 401 when session token is expired or invalid for mis-elecciones")
    void misElecciones_shouldReturn401_whenSessionExpired() throws Exception {
        // Given
        when(sessionPort.getFuncionarioIdFromSession("expired-token"))
                .thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/v1/portal/mis-elecciones")
                        .header("Authorization", "Bearer expired-token"))
                .andExpect(status().isUnauthorized());
    }

    // Req 2.3: mis-elecciones response must NEVER contain rawToken or tokenHash fields
    @Test
    @DisplayName("Should NOT expose rawToken or tokenHash in mis-elecciones response (anonymity preserved)")
    void misElecciones_shouldNotExpose_rawTokenOrHash() throws Exception {
        // Given
        when(sessionPort.getFuncionarioIdFromSession(VALID_SESSION))
                .thenReturn(Optional.of(FUNCIONARIO_ID));
        when(votingTokenRepository.findAllByFuncionarioId(FUNCIONARIO_ID))
                .thenReturn(List.of(issuedToken()));
        when(electionRepository.findById(ELECCION_ID))
                .thenReturn(Optional.of(activaElection()));
        when(participacionRepository.hasParticipated(ELECCION_ID, 42L))
                .thenReturn(false);

        // When & Then — response JSON must not contain rawToken or tokenHash fields
        mockMvc.perform(get("/api/v1/portal/mis-elecciones")
                        .header("Authorization", "Bearer " + VALID_SESSION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.elecciones[0].rawToken").doesNotExist())
                .andExpect(jsonPath("$.elecciones[0].tokenHash").doesNotExist());
    }

    // ────────────────────────────────────────────────────────────────────────
    // GET /api/v1/portal/ballot/{eleccionId}
    // ────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return 200 with ordered candidates when ISSUED token exists")
    void portalBallot_shouldReturn200WithCandidates_whenIssuedTokenExists() throws Exception {
        // Given
        UUID candId1 = UUID.randomUUID();
        UUID blankId = UUID.randomUUID();
        when(sessionPort.getFuncionarioIdFromSession(VALID_SESSION))
                .thenReturn(Optional.of(FUNCIONARIO_ID));
        when(votingTokenRepository.findIssuedByFuncionarioAndEleccion(FUNCIONARIO_ID, ELECCION_ID))
                .thenReturn(Optional.of(issuedToken()));
        when(electionRepository.findById(ELECCION_ID))
                .thenReturn(Optional.of(activaElection()));
        when(candidateRepository.findByEleccionIdOrderByNombre(ELECCION_ID))
                .thenReturn(List.of(
                        new Candidate(candId1, ELECCION_ID, "Candidato A", false, false, 1, null, null, null),
                        new Candidate(blankId, ELECCION_ID, "Voto en Blanco", true, false, null, null, null, null)
                ));

        // When & Then
        mockMvc.perform(get("/api/v1/portal/ballot/{eleccionId}", ELECCION_ID)
                        .header("Authorization", "Bearer " + VALID_SESSION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eleccionId").value(ELECCION_ID.toString()))
                .andExpect(jsonPath("$.nombre").value("Elección de Prueba"))
                .andExpect(jsonPath("$.candidates").isArray())
                .andExpect(jsonPath("$.candidates.length()").value(2))
                .andExpect(jsonPath("$.candidates[0].nombre").value("Candidato A"))
                .andExpect(jsonPath("$.candidates[0].esVotoEnBlanco").value(false))
                .andExpect(jsonPath("$.candidates[1].nombre").value("Voto en Blanco"))
                .andExpect(jsonPath("$.candidates[1].esVotoEnBlanco").value(true));
    }

    @Test
    @DisplayName("Should return 404 when funcionario has no ISSUED token for ballot election")
    void portalBallot_shouldReturn404_whenNoIssuedTokenForElection() throws Exception {
        // Given
        when(sessionPort.getFuncionarioIdFromSession(VALID_SESSION))
                .thenReturn(Optional.of(FUNCIONARIO_ID));
        when(votingTokenRepository.findIssuedByFuncionarioAndEleccion(FUNCIONARIO_ID, ELECCION_ID))
                .thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/v1/portal/ballot/{eleccionId}", ELECCION_ID)
                        .header("Authorization", "Bearer " + VALID_SESSION))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No tenés un token asignado para esta elección"));
    }

    @Test
    @DisplayName("Should return 401 when no session header present for ballot")
    void portalBallot_shouldReturn401_whenNoAuthHeader() throws Exception {
        mockMvc.perform(get("/api/v1/portal/ballot/{eleccionId}", ELECCION_ID))
                .andExpect(status().isUnauthorized());
    }

    // ────────────────────────────────────────────────────────────────────────
    // POST /api/v1/portal/votar/{eleccionId}   (Phase 2: JSON body with candidatoIds)
    // Req 3.1: successful vote cast; 3.3: error cases
    // ────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return 204 when vote is cast successfully via portal (JSON body)")
    void votar_shouldReturn204_whenVoteCastSuccessfully() throws Exception {
        // Given
        when(sessionPort.getFuncionarioIdFromSession(VALID_SESSION))
                .thenReturn(Optional.of(FUNCIONARIO_ID));
        when(votingTokenRepository.findIssuedByFuncionarioAndEleccion(FUNCIONARIO_ID, ELECCION_ID))
                .thenReturn(Optional.of(issuedToken()));

        // When & Then — Phase 2: body with candidatoIds
        mockMvc.perform(post("/api/v1/portal/votar/{eleccionId}", ELECCION_ID)
                        .header("Authorization", "Bearer " + VALID_SESSION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"candidatoIds\": [\"" + CANDIDATO_ID + "\"] }"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should return 401 when no Authorization header is present for votar")
    void votar_shouldReturn401_whenNoAuthHeader() throws Exception {
        mockMvc.perform(post("/api/v1/portal/votar/{eleccionId}", ELECCION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"candidatoIds\": [\"" + CANDIDATO_ID + "\"] }"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 401 when session token is invalid or expired for votar")
    void votar_shouldReturn401_whenSessionExpired() throws Exception {
        // Given
        when(sessionPort.getFuncionarioIdFromSession("expired-token"))
                .thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(post("/api/v1/portal/votar/{eleccionId}", ELECCION_ID)
                        .header("Authorization", "Bearer expired-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"candidatoIds\": [\"" + CANDIDATO_ID + "\"] }"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 404 when funcionario has no assigned token for the election")
    void votar_shouldReturn404_whenNoTokenAssigned() throws Exception {
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
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No tenés un token asignado para esta elección"));
    }

    @Test
    @DisplayName("Should return 409 when domain throws DomainException (already voted, election closed, etc)")
    void votar_shouldReturn409_whenDomainRuleViolated() throws Exception {
        // Given
        when(sessionPort.getFuncionarioIdFromSession(VALID_SESSION))
                .thenReturn(Optional.of(FUNCIONARIO_ID));
        when(votingTokenRepository.findIssuedByFuncionarioAndEleccion(FUNCIONARIO_ID, ELECCION_ID))
                .thenReturn(Optional.of(issuedToken()));
        doThrow(new DomainException("Ya hiciste efectivo tu derecho al voto"))
                .when(castVoteAppService).castVoteByTokenId(any(), any());

        // When & Then
        mockMvc.perform(post("/api/v1/portal/votar/{eleccionId}", ELECCION_ID)
                        .header("Authorization", "Bearer " + VALID_SESSION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"candidatoIds\": [\"" + CANDIDATO_ID + "\"] }"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ya hiciste efectivo tu derecho al voto"));
    }
}
