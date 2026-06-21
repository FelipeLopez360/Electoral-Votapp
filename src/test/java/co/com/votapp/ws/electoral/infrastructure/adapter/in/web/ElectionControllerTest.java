package co.com.votapp.ws.electoral.infrastructure.adapter.in.web;

import co.com.votapp.ws.electoral.application.command.AddCandidateCommand;
import co.com.votapp.ws.electoral.application.service.ElectionTransitionAppService;
import co.com.votapp.ws.electoral.domain.Candidate;
import co.com.votapp.ws.electoral.domain.Election;
import co.com.votapp.ws.electoral.domain.ElectionStatus;
import co.com.votapp.ws.electoral.domain.port.in.AddCandidateUseCase;
import co.com.votapp.ws.electoral.domain.port.in.CreateElectionUseCase;
import co.com.votapp.ws.electoral.domain.port.in.FinalizeElectionUseCase;
import co.com.votapp.ws.electoral.domain.port.out.CandidateRepositoryPort;
import co.com.votapp.ws.electoral.domain.port.out.ElectionRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ElectionController}.
 *
 * <p>No Spring context — all collaborators are mocked via Mockito.
 * Tests cover HTTP mapping, command assembly, and response projection.
 *
 * <p>Note: {@code activateElection} now delegates to {@link ElectionTransitionAppService}
 * (which orchestrates both activation and bulk token issuance) instead of calling
 * {@code ActivateElectionUseCase} directly.
 */
@DisplayName("ElectionController - Election lifecycle REST adapter")
@ExtendWith(MockitoExtension.class)
class ElectionControllerTest {

    @Mock private CreateElectionUseCase createElectionUseCase;
    @Mock private ElectionTransitionAppService electionTransitionAppService;
    @Mock private FinalizeElectionUseCase finalizeElectionUseCase;
    @Mock private AddCandidateUseCase addCandidateUseCase;
    @Mock private ElectionRepositoryPort electionRepository;
    @Mock private CandidateRepositoryPort candidateRepository;

    private ElectionController controller;

    private static final UUID ELECTION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CANDIDATE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final LocalDateTime START = LocalDateTime.now().plusDays(30);
    private static final LocalDateTime END = START.plusHours(10);

    @BeforeEach
    void setUp() {
        controller = new ElectionController(
                createElectionUseCase,
                null, // CreateElectionWithCandidatesAppService — not needed for these unit tests
                electionTransitionAppService,
                finalizeElectionUseCase,
                addCandidateUseCase,
                electionRepository,
                candidateRepository
        );
    }

    // ── createElection ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return 201 with election response when creation succeeds")
    void createElection_shouldReturn201_whenCreationSucceeds() {
        // Given
        Election created = new Election(ELECTION_ID, "ELEC-2025", "Elección General", ElectionStatus.PROGRAMADA, START, END, true, 1);
        when(createElectionUseCase.create(any())).thenReturn(created);

        String startStr = START.toString();
        String endStr = END.toString();
        ElectionController.CreateElectionRequest request = new ElectionController.CreateElectionRequest(
                "ELEC-2025", "Elección General",
                startStr, endStr,
                true, 1
        );

        // When
        ResponseEntity<?> response = controller.createElection(request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        var body = (ElectionController.ElectionResponse) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.id()).isEqualTo(ELECTION_ID.toString());
        assertThat(body.codigo()).isEqualTo("ELEC-2025");
        assertThat(body.estado()).isEqualTo("PROGRAMADA");
    }

    @Test
    @DisplayName("Should build CreateElectionCommand with parsed dates from request")
    void createElection_shouldBuildCommandWithParsedDates_whenRequestIsValid() {
        // Given
        Election created = new Election(ELECTION_ID, "ELEC-2025", "Test", ElectionStatus.PROGRAMADA, START, END, true, 1);
        when(createElectionUseCase.create(any())).thenReturn(created);

        String startStr = START.toString();
        String endStr = END.toString();
        ElectionController.CreateElectionRequest request = new ElectionController.CreateElectionRequest(
                "ELEC-2025", "Test",
                startStr, endStr,
                true, 1
        );

        // When
        controller.createElection(request);

        // Then
        ArgumentCaptor<co.com.votapp.ws.electoral.application.command.CreateElectionCommand> captor =
                ArgumentCaptor.forClass(co.com.votapp.ws.electoral.application.command.CreateElectionCommand.class);
        verify(createElectionUseCase).create(captor.capture());
        assertThat(captor.getValue().codigo()).isEqualTo("ELEC-2025");
        assertThat(captor.getValue().fechaInicio()).isEqualTo(START);
        assertThat(captor.getValue().fechaFin()).isEqualTo(END);
    }

    @Test
    @DisplayName("Should throw DateTimeParseException when fechaInicio is malformed")
    void createElection_shouldThrow_whenDateIsMalformed() {
        // Given
        ElectionController.CreateElectionRequest badRequest = new ElectionController.CreateElectionRequest(
                "ELEC-2025", "Test",
                "not-a-date", "2025-11-01T18:00:00",
                true, 1
        );

        // When & Then
        assertThatThrownBy(() -> controller.createElection(badRequest))
                .isInstanceOf(java.time.format.DateTimeParseException.class);
    }

    // ── createElection ballot config (legacy POST /api/v1/elections) ─────────

    @Test
    @DisplayName("Should forward permiteVotoBlanco=false and maxVotosPorElector=3 from legacy createElection request")
    void createElection_shouldForwardBallotConfig_whenNonDefaultValuesProvided() {
        // Given — non-default ballot config: no blank vote, 3 votes max
        Election created = new Election(ELECTION_ID, "ELEC-2025", "Elección General",
                ElectionStatus.PROGRAMADA, START, END, false, 3);
        when(createElectionUseCase.create(any())).thenReturn(created);

        ElectionController.CreateElectionRequest request = new ElectionController.CreateElectionRequest(
                "ELEC-2025", "Elección General",
                START.toString(), END.toString(),
                false, 3
        );

        // When
        ResponseEntity<?> response = controller.createElection(request);

        // Then — command must carry the non-default values (not hardcoded 1/true)
        ArgumentCaptor<co.com.votapp.ws.electoral.application.command.CreateElectionCommand> captor =
                ArgumentCaptor.forClass(co.com.votapp.ws.electoral.application.command.CreateElectionCommand.class);
        verify(createElectionUseCase).create(captor.capture());
        assertThat(captor.getValue().permiteVotoBlanco()).isFalse();
        assertThat(captor.getValue().maxVotosPorElector()).isEqualTo(3);

        // And the response reflects the actual election state
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        var body = (ElectionController.ElectionResponse) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.permiteVotoBlanco()).isFalse();
        assertThat(body.maxVotosPorElector()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should forward permiteVotoBlanco=true and maxVotosPorElector=2 from legacy createElection request")
    void createElection_shouldForwardBallotConfig_whenPermiteVotoBlancoTrueAndMaxTwo() {
        // Given — triangulation: blank vote allowed, but max 2
        Election created = new Election(ELECTION_ID, "ELEC-2026", "Segunda Elección",
                ElectionStatus.PROGRAMADA, START, END, true, 2);
        when(createElectionUseCase.create(any())).thenReturn(created);

        ElectionController.CreateElectionRequest request = new ElectionController.CreateElectionRequest(
                "ELEC-2026", "Segunda Elección",
                START.toString(), END.toString(),
                true, 2
        );

        // When
        controller.createElection(request);

        // Then
        ArgumentCaptor<co.com.votapp.ws.electoral.application.command.CreateElectionCommand> captor =
                ArgumentCaptor.forClass(co.com.votapp.ws.electoral.application.command.CreateElectionCommand.class);
        verify(createElectionUseCase).create(captor.capture());
        assertThat(captor.getValue().permiteVotoBlanco()).isTrue();
        assertThat(captor.getValue().maxVotosPorElector()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should return 201 with ballot config echoed back when legacy createElection request uses non-defaults")
    void createElection_shouldReturn201WithBallotConfigInResponse_whenNonDefaultConfig() {
        // Given
        Election created = new Election(ELECTION_ID, "ELEC-X", "Elección X",
                ElectionStatus.PROGRAMADA, START, END, false, 5);
        when(createElectionUseCase.create(any())).thenReturn(created);

        ElectionController.CreateElectionRequest request = new ElectionController.CreateElectionRequest(
                "ELEC-X", "Elección X",
                START.toString(), END.toString(),
                false, 5
        );

        // When
        ResponseEntity<?> response = controller.createElection(request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        var body = (ElectionController.ElectionResponse) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.permiteVotoBlanco()).isFalse();
        assertThat(body.maxVotosPorElector()).isEqualTo(5);
    }

    // ── activateElection ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return 204 when activation delegates to ElectionTransitionAppService")
    void activateElection_shouldReturn204_whenActivationSucceeds() {
        // When
        ResponseEntity<Void> response = controller.activateElection(ELECTION_ID.toString());

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(electionTransitionAppService).activate(ELECTION_ID);
    }

    @Test
    @DisplayName("Should pass UUID to ElectionTransitionAppService.activate from path variable")
    void activateElection_shouldPassUuidToAppService_fromPathVariable() {
        // Given
        ArgumentCaptor<UUID> captor = ArgumentCaptor.forClass(UUID.class);

        // When
        controller.activateElection(ELECTION_ID.toString());

        // Then
        verify(electionTransitionAppService).activate(captor.capture());
        assertThat(captor.getValue()).isEqualTo(ELECTION_ID);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when id is not a valid UUID")
    void activateElection_shouldThrow_whenIdIsNotUuid() {
        // When & Then
        assertThatThrownBy(() -> controller.activateElection("not-a-uuid"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── finalizeElection ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return 204 when finalization succeeds")
    void finalizeElection_shouldReturn204_whenFinalizationSucceeds() {
        // When
        ResponseEntity<Void> response = controller.finalizeElection(ELECTION_ID.toString());

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(finalizeElectionUseCase).finalize(ELECTION_ID);
    }

    // ── addCandidate ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return 201 with candidate response when creation succeeds")
    void addCandidate_shouldReturn201_whenCreationSucceeds() {
        // Given
        Candidate candidate = new Candidate(CANDIDATE_ID, ELECTION_ID, "Candidato A", false, false, 1, null, null, null, null);
        when(addCandidateUseCase.addCandidate(any())).thenReturn(candidate);

        ElectionController.AddCandidateRequest request = new ElectionController.AddCandidateRequest(
                "Candidato A", "Candidate description", 1, null, null, null, null);

        // When
        ResponseEntity<ElectionController.CandidateResponse> response =
                controller.addCandidate(ELECTION_ID, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(CANDIDATE_ID.toString());
        assertThat(response.getBody().nombre()).isEqualTo("Candidato A");
        assertThat(response.getBody().numeroOrden()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should build AddCandidateCommand with path variable and request body")
    void addCandidate_shouldBuildCommand_fromPathVariableAndBody() {
        // Given
        Candidate candidate = new Candidate(CANDIDATE_ID, ELECTION_ID, "Candidato B", false, false, 2, null, null, null, null);
        when(addCandidateUseCase.addCandidate(any())).thenReturn(candidate);

        ElectionController.AddCandidateRequest request = new ElectionController.AddCandidateRequest(
                "Candidato B", "Another candidate", 2, null, null, null, null);

        // When
        controller.addCandidate(ELECTION_ID, request);

        // Then
        ArgumentCaptor<AddCandidateCommand> captor = ArgumentCaptor.forClass(AddCandidateCommand.class);
        verify(addCandidateUseCase).addCandidate(captor.capture());
        AddCandidateCommand command = captor.getValue();
        assertThat(command.eleccionId()).isEqualTo(ELECTION_ID);
        assertThat(command.nombre()).isEqualTo("Candidato B");
        assertThat(command.descripcion()).isEqualTo("Another candidate");
        assertThat(command.numeroOrden()).isEqualTo(2);
    }

}
