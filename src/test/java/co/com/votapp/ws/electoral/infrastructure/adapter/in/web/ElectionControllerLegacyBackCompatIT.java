package co.com.votapp.ws.electoral.infrastructure.adapter.in.web;

import co.com.votapp.ws.auth.domain.port.out.PortalSessionPort;
import co.com.votapp.ws.common.config.SecurityConfig;
import co.com.votapp.ws.common.exception.GlobalExceptionHandler;
import co.com.votapp.ws.electoral.application.command.CreateElectionCommand;
import co.com.votapp.ws.electoral.application.service.CreateElectionWithCandidatesAppService;
import co.com.votapp.ws.electoral.application.service.ElectionTransitionAppService;
import co.com.votapp.ws.electoral.domain.Election;
import co.com.votapp.ws.electoral.domain.ElectionStatus;
import co.com.votapp.ws.electoral.domain.port.in.AddCandidateUseCase;
import co.com.votapp.ws.electoral.domain.port.in.CreateElectionUseCase;
import co.com.votapp.ws.electoral.domain.port.in.FinalizeElectionUseCase;
import co.com.votapp.ws.electoral.domain.port.out.CandidateRepositoryPort;
import co.com.votapp.ws.electoral.domain.port.out.ElectionRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * WebMvc backward-compatibility tests for legacy {@code POST /api/v1/elections}.
 *
 * <p>Task 2.8 corrective (gate retry): Verifies that legacy callers omitting
 * {@code permiteVotoBlanco} and {@code maxVotosPorElector} from the JSON payload
 * receive the historical defaults ({@code true} / {@code 1}) instead of the
 * Java primitive defaults ({@code false} / {@code 0}).
 *
 * <p>TDD cycle: RED tests written before fixing {@code CreateElectionRequest}.
 */
@DisplayName("ElectionController - Legacy POST /api/v1/elections backward compatibility")
@WebMvcTest(ElectionController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class ElectionControllerLegacyBackCompatIT {

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

    private static final UUID ELECTION_ID = UUID.fromString("aaaabbbb-cccc-dddd-eeee-ffffffffffff");
    private static final LocalDateTime START = LocalDateTime.now().plusDays(5);
    private static final LocalDateTime END = START.plusDays(1);

    private Election programadaElection(boolean permiteVotoBlanco, int maxVotos) {
        return new Election(ELECTION_ID, "ELEC-LEGACY", "Legacy Election",
                ElectionStatus.PROGRAMADA, START, END, permiteVotoBlanco, maxVotos);
    }

    // ── Scenario 1: omitted ballot-config fields → default to true / 1 ──────

    @Test
    @WithMockUser
    @DisplayName("Should succeed with 201 when legacy caller omits ballot-config fields (default true/1 applied)")
    void createElection_shouldReturn201WithDefaults_whenBallotConfigFieldsAreOmitted() throws Exception {
        // Given — legacy caller only sends required fields, no ballot config
        when(createElectionUseCase.create(any())).thenReturn(programadaElection(true, 1));

        String body = """
                {
                  "codigo": "ELEC-LEGACY",
                  "nombre": "Legacy Election",
                  "fechaInicio": "%s",
                  "fechaFin": "%s"
                }
                """.formatted(START, END);

        // When & Then — must succeed, not fail with 500 (domain invariant maxVotos < 1)
        mockMvc.perform(post("/api/v1/elections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.permiteVotoBlanco").value(true))
                .andExpect(jsonPath("$.maxVotosPorElector").value(1));
    }

    @Test
    @WithMockUser
    @DisplayName("Should pass permiteVotoBlanco=true and maxVotosPorElector=1 to command when ballot-config omitted")
    void createElection_shouldBuildCommandWithDefaults_whenBallotConfigFieldsAreOmitted() throws Exception {
        // Given — omit ballot config fields entirely
        when(createElectionUseCase.create(any())).thenReturn(programadaElection(true, 1));

        String body = """
                {
                  "codigo": "ELEC-DFLT",
                  "nombre": "Default Test",
                  "fechaInicio": "%s",
                  "fechaFin": "%s"
                }
                """.formatted(START, END);

        // When
        mockMvc.perform(post("/api/v1/elections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        // Then — the command forwarded to the use case must carry the defaults
        ArgumentCaptor<CreateElectionCommand> captor =
                ArgumentCaptor.forClass(CreateElectionCommand.class);
        verify(createElectionUseCase).create(captor.capture());
        assertThat(captor.getValue().permiteVotoBlanco()).isTrue();
        assertThat(captor.getValue().maxVotosPorElector()).isEqualTo(1);
    }

    // ── Scenario 2: explicit non-default values must still flow through ───────

    @Test
    @WithMockUser
    @DisplayName("Should pass explicit permiteVotoBlanco=false and maxVotosPorElector=3 when provided in request")
    void createElection_shouldForwardExplicitNonDefaultBallotConfig_whenProvided() throws Exception {
        // Given — caller explicitly sets non-defaults
        when(createElectionUseCase.create(any())).thenReturn(programadaElection(false, 3));

        String body = """
                {
                  "codigo": "ELEC-EXPLICIT",
                  "nombre": "Explicit Config Test",
                  "fechaInicio": "%s",
                  "fechaFin": "%s",
                  "permiteVotoBlanco": false,
                  "maxVotosPorElector": 3
                }
                """.formatted(START, END);

        // When
        mockMvc.perform(post("/api/v1/elections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        // Then — the command must carry the explicit values, not the defaults
        ArgumentCaptor<CreateElectionCommand> captor =
                ArgumentCaptor.forClass(CreateElectionCommand.class);
        verify(createElectionUseCase).create(captor.capture());
        assertThat(captor.getValue().permiteVotoBlanco()).isFalse();
        assertThat(captor.getValue().maxVotosPorElector()).isEqualTo(3);
    }

    // ── Scenario 3: explicit maxVotosPorElector=0 → 400 (domain invariant) ──

    @Test
    @WithMockUser
    @DisplayName("Should return 400 when legacy caller explicitly sends maxVotosPorElector=0")
    void createElection_shouldReturn400_whenMaxVotosExplicitlyZero() throws Exception {
        // Given — caller explicitly sends 0 (invalid per domain invariant)
        when(createElectionUseCase.create(any()))
                .thenThrow(new IllegalArgumentException("maxVotosPorElector must be >= 1"));

        String body = """
                {
                  "codigo": "ELEC-ZERO",
                  "nombre": "Zero Max Votes",
                  "fechaInicio": "%s",
                  "fechaFin": "%s",
                  "permiteVotoBlanco": true,
                  "maxVotosPorElector": 0
                }
                """.formatted(START, END);

        // When & Then — domain invariant violation must produce 400, not 500
        mockMvc.perform(post("/api/v1/elections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // ── Scenario 4 (triangulation): partial omission — only permiteVotoBlanco omitted ──

    @Test
    @WithMockUser
    @DisplayName("Should apply default permiteVotoBlanco=true when only that field is omitted, keeping explicit maxVotosPorElector=2")
    void createElection_shouldApplyDefaultPermiteVotoBlanco_whenOnlyThatFieldIsOmitted() throws Exception {
        // Given — omit only permiteVotoBlanco; maxVotosPorElector is explicit
        when(createElectionUseCase.create(any())).thenReturn(programadaElection(true, 2));

        String body = """
                {
                  "codigo": "ELEC-PARTIAL",
                  "nombre": "Partial Omit",
                  "fechaInicio": "%s",
                  "fechaFin": "%s",
                  "maxVotosPorElector": 2
                }
                """.formatted(START, END);

        // When
        mockMvc.perform(post("/api/v1/elections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        // Then — permiteVotoBlanco must default to true; maxVotosPorElector must be 2 (explicit)
        ArgumentCaptor<CreateElectionCommand> captor =
                ArgumentCaptor.forClass(CreateElectionCommand.class);
        verify(createElectionUseCase).create(captor.capture());
        assertThat(captor.getValue().permiteVotoBlanco()).isTrue();
        assertThat(captor.getValue().maxVotosPorElector()).isEqualTo(2);
    }
}
