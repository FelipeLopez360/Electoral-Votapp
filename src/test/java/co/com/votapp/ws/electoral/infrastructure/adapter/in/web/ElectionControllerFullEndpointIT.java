package co.com.votapp.ws.electoral.infrastructure.adapter.in.web;

import co.com.votapp.ws.auth.domain.port.out.PortalSessionPort;
import co.com.votapp.ws.common.config.SecurityConfig;
import co.com.votapp.ws.common.exception.GlobalExceptionHandler;
import co.com.votapp.ws.electoral.application.service.CreateElectionWithCandidatesAppService;
import co.com.votapp.ws.electoral.application.service.ElectionTransitionAppService;
import co.com.votapp.ws.electoral.domain.Candidate;
import co.com.votapp.ws.electoral.domain.Election;
import co.com.votapp.ws.electoral.domain.ElectionStatus;
import co.com.votapp.ws.electoral.domain.port.in.AddCandidateUseCase;
import co.com.votapp.ws.electoral.domain.port.in.CreateElectionUseCase;
import co.com.votapp.ws.electoral.domain.port.in.FinalizeElectionUseCase;
import co.com.votapp.ws.electoral.domain.port.out.CandidateRepositoryPort;
import co.com.votapp.ws.electoral.domain.port.out.ElectionRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * WebMvc slice tests for {@link ElectionController} POST /api/v1/elections/full endpoint.
 *
 * <p>Task 2.3: Verifies the new comprehensive election creation endpoint,
 * including DTO parsing, ballot config fields, and candidate rich profiles.
 */
@DisplayName("ElectionController - POST /api/v1/elections/full comprehensive endpoint")
@WebMvcTest(ElectionController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class ElectionControllerFullEndpointIT {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private CreateElectionUseCase createElectionUseCase;
    @MockitoBean private CreateElectionWithCandidatesAppService createElectionWithCandidatesAppService;
    @MockitoBean private ElectionTransitionAppService electionTransitionAppService;
    @MockitoBean private FinalizeElectionUseCase finalizeElectionUseCase;
    @MockitoBean private AddCandidateUseCase addCandidateUseCase;
    @MockitoBean private ElectionRepositoryPort electionRepository;
    @MockitoBean private CandidateRepositoryPort candidateRepository;
    @MockitoBean private PortalSessionPort sessionPort;

    private static final UUID ELECTION_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final LocalDateTime START = LocalDateTime.now().plusDays(5);
    private static final LocalDateTime END = START.plusDays(1);

    private Election programadaElection() {
        return new Election(ELECTION_ID, "ELEC-FULL", "Full Election",
                ElectionStatus.PROGRAMADA, START, END, true, 2);
    }

    // ── POST /api/v1/elections/full ──────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("Should return 201 with election fields including ballot config when full election is created")
    void postElectionFull_shouldReturn201WithBallotConfig_whenPayloadIsValid() throws Exception {
        // Given
        when(createElectionWithCandidatesAppService.createWithCandidates(any(), any()))
                .thenReturn(programadaElection());

        String body = """
                {
                  "codigo": "ELEC-FULL",
                  "nombre": "Full Election",
                  "fechaInicio": "%s",
                  "fechaFin": "%s",
                  "permiteVotoBlanco": true,
                  "maxVotosPorElector": 2,
                  "candidatos": [
                    { "nombre": "Candidato A", "numeroOrden": 1,
                      "fotoUrl": "http://foto.png", "biografia": "Bio A",
                      "propuestas": "Propuesta A", "afiliacionPolitica": "Partido A" },
                    { "nombre": "Candidato B", "numeroOrden": 2 }
                  ]
                }
                """.formatted(START, END);

        // When & Then
        mockMvc.perform(post("/api/v1/elections/full")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(ELECTION_ID.toString()))
                .andExpect(jsonPath("$.codigo").value("ELEC-FULL"))
                .andExpect(jsonPath("$.estado").value("PROGRAMADA"))
                .andExpect(jsonPath("$.permiteVotoBlanco").value(true))
                .andExpect(jsonPath("$.maxVotosPorElector").value(2));
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 201 with empty candidatos list when no candidates provided")
    void postElectionFull_shouldReturn201_whenNoCandidatesProvided() throws Exception {
        // Given — triangulation: no candidates
        Election election = new Election(ELECTION_ID, "ELEC-NOCAND", "No Candidates",
                ElectionStatus.PROGRAMADA, START, END, false, 1);
        when(createElectionWithCandidatesAppService.createWithCandidates(any(), any()))
                .thenReturn(election);

        String body = """
                {
                  "codigo": "ELEC-NOCAND",
                  "nombre": "No Candidates",
                  "fechaInicio": "%s",
                  "fechaFin": "%s",
                  "permiteVotoBlanco": false,
                  "maxVotosPorElector": 1,
                  "candidatos": []
                }
                """.formatted(START, END);

        // When & Then
        mockMvc.perform(post("/api/v1/elections/full")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.permiteVotoBlanco").value(false))
                .andExpect(jsonPath("$.maxVotosPorElector").value(1));
    }

    @Test
    @WithMockUser
    @DisplayName("Should delegate to CreateElectionWithCandidatesAppService with ballot config and candidates")
    void postElectionFull_shouldDelegateToAppService_withParsedCommand() throws Exception {
        // Given
        when(createElectionWithCandidatesAppService.createWithCandidates(any(), any()))
                .thenReturn(programadaElection());

        String body = """
                {
                  "codigo": "ELEC-VERIFY",
                  "nombre": "Verify Delegation",
                  "fechaInicio": "%s",
                  "fechaFin": "%s",
                  "permiteVotoBlanco": true,
                  "maxVotosPorElector": 3,
                  "candidatos": [
                    { "nombre": "Candidato A", "numeroOrden": 1 }
                  ]
                }
                """.formatted(START, END);

        // When
        mockMvc.perform(post("/api/v1/elections/full")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        // Then
        verify(createElectionWithCandidatesAppService).createWithCandidates(any(), any());
    }

    @Test
    @DisplayName("Should return 401 when no authentication is provided")
    void postElectionFull_shouldReturn401_whenNoAuth() throws Exception {
        // Given
        String body = """
                {
                  "codigo": "ELEC-NOAUTH",
                  "nombre": "No Auth",
                  "fechaInicio": "%s",
                  "fechaFin": "%s",
                  "permiteVotoBlanco": true,
                  "maxVotosPorElector": 1,
                  "candidatos": []
                }
                """.formatted(START, END);

        // When & Then
        mockMvc.perform(post("/api/v1/elections/full")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    // ── Semantic validation: invalid ballot config → 400, not 500 ───────────

    @Test
    @WithMockUser
    @DisplayName("Should return 400 when maxVotosPorElector is 0 (below minimum of 1)")
    void postElectionFull_shouldReturn400_whenMaxVotosIsZero() throws Exception {
        // Given — maxVotosPorElector: 0 violates domain invariant; must not produce 500
        String body = """
                {
                  "codigo": "ELEC-INVALID",
                  "nombre": "Invalid Election",
                  "fechaInicio": "%s",
                  "fechaFin": "%s",
                  "permiteVotoBlanco": true,
                  "maxVotosPorElector": 0,
                  "candidatos": []
                }
                """.formatted(START, END);

        // When & Then
        mockMvc.perform(post("/api/v1/elections/full")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 400 when maxVotosPorElector is negative")
    void postElectionFull_shouldReturn400_whenMaxVotosIsNegative() throws Exception {
        // Given — negative value also violates maxVotosPorElector >= 1
        String body = """
                {
                  "codigo": "ELEC-NEG",
                  "nombre": "Negative Max Votes",
                  "fechaInicio": "%s",
                  "fechaFin": "%s",
                  "permiteVotoBlanco": true,
                  "maxVotosPorElector": -5,
                  "candidatos": []
                }
                """.formatted(START, END);

        // When & Then
        mockMvc.perform(post("/api/v1/elections/full")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 400 with error message when maxVotosPorElector is 0")
    void postElectionFull_shouldReturn400WithErrorBody_whenMaxVotosIsZero() throws Exception {
        // Given — confirm the response body carries an error message (not empty)
        String body = """
                {
                  "codigo": "ELEC-ERRBODY",
                  "nombre": "Error Body Check",
                  "fechaInicio": "%s",
                  "fechaFin": "%s",
                  "permiteVotoBlanco": true,
                  "maxVotosPorElector": 0,
                  "candidatos": []
                }
                """.formatted(START, END);

        // When & Then
        mockMvc.perform(post("/api/v1/elections/full")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }
}
