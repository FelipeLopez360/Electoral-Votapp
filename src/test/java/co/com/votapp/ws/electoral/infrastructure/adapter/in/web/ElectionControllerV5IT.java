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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * V5 slice tests for {@link ElectionController}.
 *
 * <p>Validates updated DTOs: no {@code numeroOrden} or {@code afiliacionPolitica},
 * {@code funcionarioId} present in requests and responses.
 *
 * <p>RED cycle: written first. Will fail until controller DTOs are updated.
 */
@DisplayName("ElectionController - V5 DTOs (funcionarioId, no numeroOrden/afiliacion)")
@WebMvcTest(ElectionController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class ElectionControllerV5IT {

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

    private static final UUID ELECTION_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID CANDIDATE_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final LocalDateTime START = LocalDateTime.now().plusDays(5);
    private static final LocalDateTime END = START.plusDays(1);

    // ── POST /api/v1/elections/{id}/candidates ───────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("Should return 201 with funcionarioId in response when candidate added")
    void addCandidate_shouldReturn201WithFuncionarioId() throws Exception {
        // Given
        Candidate candidate = new Candidate(
                CANDIDATE_ID, ELECTION_ID, "Juan Pérez", false, false,
                7, "https://example.com/photo.jpg", "Bio", "Propuestas");
        when(addCandidateUseCase.addCandidate(any())).thenReturn(candidate);

        String body = """
                {
                  "nombre": "Juan Pérez",
                  "funcionarioId": 7,
                  "fotoUrl": "https://example.com/photo.jpg",
                  "biografia": "Bio",
                  "propuestas": "Propuestas"
                }
                """;

        // When & Then
        mockMvc.perform(post("/api/v1/elections/{id}/candidates", ELECTION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(CANDIDATE_ID.toString()))
                .andExpect(jsonPath("$.nombre").value("Juan Pérez"))
                .andExpect(jsonPath("$.funcionarioId").value(7));
    }

    @Test
    @WithMockUser
    @DisplayName("CandidateResponse should NOT contain numeroOrden field")
    void addCandidate_candidateResponse_shouldNotHaveNumeroOrden() throws Exception {
        // Given
        Candidate candidate = new Candidate(
                CANDIDATE_ID, ELECTION_ID, "María García", false, false,
                3, null, null, null);
        when(addCandidateUseCase.addCandidate(any())).thenReturn(candidate);

        String body = """
                { "nombre": "María García", "funcionarioId": 3 }
                """;

        // When & Then
        mockMvc.perform(post("/api/v1/elections/{id}/candidates", ELECTION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroOrden").doesNotExist())
                .andExpect(jsonPath("$.afiliacionPolitica").doesNotExist());
    }

    // ── GET /api/v1/elections/{id}/candidates ────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("listCandidates should return candidates with funcionarioId, no numeroOrden")
    void listCandidates_shouldReturnFuncionarioId_noNumeroOrden() throws Exception {
        // Given
        List<Candidate> candidates = List.of(
                new Candidate(CANDIDATE_ID, ELECTION_ID, "Ana González", false, false,
                        1, null, null, null),
                new Candidate(UUID.randomUUID(), ELECTION_ID, "Voto en Blanco", true, false,
                        null, null, null, null)
        );
        when(candidateRepository.findByEleccionIdOrderByNombre(ELECTION_ID)).thenReturn(candidates);

        // When & Then
        mockMvc.perform(get("/api/v1/elections/{id}/candidates", ELECTION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Ana González"))
                .andExpect(jsonPath("$[0].funcionarioId").value(1))
                .andExpect(jsonPath("$[0].numeroOrden").doesNotExist())
                .andExpect(jsonPath("$[0].afiliacionPolitica").doesNotExist());
    }

    // ── POST /api/v1/elections/full ──────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("Full election creation should accept funcionarioId, not numeroOrden")
    void postElectionFull_shouldAcceptFuncionarioId() throws Exception {
        // Given
        Election election = new Election(ELECTION_ID, "ELEC-V5", "V5 Election",
                ElectionStatus.PROGRAMADA, START, END, true, 1);
        when(createElectionWithCandidatesAppService.createWithCandidates(any(), any()))
                .thenReturn(election);

        String body = """
                {
                  "codigo": "ELEC-V5",
                  "nombre": "V5 Election",
                  "fechaInicio": "%s",
                  "fechaFin": "%s",
                  "permiteVotoBlanco": true,
                  "maxVotosPorElector": 1,
                  "candidatos": [
                    { "nombre": "Juan Pérez", "funcionarioId": 5,
                      "fotoUrl": "http://foto.png", "biografia": "Bio",
                      "propuestas": "Propuesta" }
                  ]
                }
                """.formatted(START, END);

        // When & Then
        mockMvc.perform(post("/api/v1/elections/full")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(ELECTION_ID.toString()))
                .andExpect(jsonPath("$.codigo").value("ELEC-V5"));
    }
}
