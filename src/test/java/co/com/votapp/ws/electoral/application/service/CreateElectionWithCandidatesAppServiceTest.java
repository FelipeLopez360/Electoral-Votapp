package co.com.votapp.ws.electoral.application.service;

import co.com.votapp.ws.electoral.application.command.AddCandidateCommand;
import co.com.votapp.ws.electoral.application.command.CreateElectionCommand;
import co.com.votapp.ws.electoral.application.service.CreateElectionWithCandidatesAppService.CandidateCreationData;
import co.com.votapp.ws.electoral.domain.Candidate;
import co.com.votapp.ws.electoral.domain.Election;
import co.com.votapp.ws.electoral.domain.ElectionStatus;
import co.com.votapp.ws.electoral.domain.port.in.AddCandidateUseCase;
import co.com.votapp.ws.electoral.domain.port.in.CreateElectionUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CreateElectionWithCandidatesAppService}.
 *
 * <p>Verifies the orchestration contract: CreateElectionUseCase executes first,
 * then AddCandidateUseCase is called once per candidate in the provided list,
 * with the created election's ID injected into each candidate command.
 * The entire operation runs within a single @Transactional boundary.
 */
@DisplayName("CreateElectionWithCandidatesAppService - Comprehensive election creation orchestration")
@ExtendWith(MockitoExtension.class)
class CreateElectionWithCandidatesAppServiceTest {

    @Mock private CreateElectionUseCase createElectionUseCase;
    @Mock private AddCandidateUseCase addCandidateUseCase;

    private CreateElectionWithCandidatesAppService service;

    private static final UUID ELECTION_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final LocalDateTime START = LocalDateTime.now().plusDays(10);
    private static final LocalDateTime END = START.plusDays(1);

    @BeforeEach
    void setUp() {
        service = new CreateElectionWithCandidatesAppService(createElectionUseCase, addCandidateUseCase);
    }

    // ── createElectionWithCandidates — happy path ─────────────────────────────

    @Test
    @DisplayName("Should call CreateElectionUseCase then AddCandidateUseCase for each candidate in order")
    void createWithCandidates_shouldCallUseCasesInOrder_forEachCandidate() {
        // Given
        Election createdElection = new Election(ELECTION_ID, "ELEC-01", "Test Election",
                ElectionStatus.PROGRAMADA, START, END, true, 1);
        when(createElectionUseCase.create(any())).thenReturn(createdElection);

        Candidate cand1 = new Candidate(UUID.randomUUID(), ELECTION_ID, "Candidato A", false, false, 1,
                null, null, null, null);
        Candidate cand2 = new Candidate(UUID.randomUUID(), ELECTION_ID, "Candidato B", false, false, 2,
                null, null, null, null);
        when(addCandidateUseCase.addCandidate(any())).thenReturn(cand1, cand2);

        CreateElectionCommand electionCmd = new CreateElectionCommand(
                "ELEC-01", "Test Election", null, START, END, true, 1);

        List<CandidateCreationData> candidates = List.of(
                new CandidateCreationData("Candidato A", 1, null, null, null, null),
                new CandidateCreationData("Candidato B", 2, null, null, null, null)
        );

        // When
        Election result = service.createWithCandidates(electionCmd, candidates);

        // Then — result is the created election
        assertThat(result).isEqualTo(createdElection);

        // Verify order: create first, then each candidate (2 times total)
        InOrder inOrder = inOrder(createElectionUseCase, addCandidateUseCase);
        inOrder.verify(createElectionUseCase).create(electionCmd);
        inOrder.verify(addCandidateUseCase, times(2)).addCandidate(any());
    }

    @Test
    @DisplayName("Should call AddCandidateUseCase exactly N times for N candidates provided")
    void createWithCandidates_shouldCallAddCandidate_exactlyNTimes() {
        // Given — triangulation: 3 candidates
        UUID elecId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        Election createdElection = new Election(elecId, "ELEC-02", "Multi Candidate Election",
                ElectionStatus.PROGRAMADA, START, END, true, 3);
        when(createElectionUseCase.create(any())).thenReturn(createdElection);

        Candidate stub = new Candidate(UUID.randomUUID(), elecId, "X", false, false, 1,
                null, null, null, null);
        when(addCandidateUseCase.addCandidate(any())).thenReturn(stub);

        CreateElectionCommand electionCmd = new CreateElectionCommand(
                "ELEC-02", "Multi Candidate Election", null, START, END, true, 3);

        List<CandidateCreationData> candidates = List.of(
                new CandidateCreationData("Candidato X", 1, null, null, null, null),
                new CandidateCreationData("Candidato Y", 2, null, null, null, null),
                new CandidateCreationData("Candidato Z", 3, null, null, null, null)
        );

        // When
        service.createWithCandidates(electionCmd, candidates);

        // Then
        verify(createElectionUseCase, times(1)).create(any());
        verify(addCandidateUseCase, times(3)).addCandidate(any());
    }

    @Test
    @DisplayName("Should not call AddCandidateUseCase when candidate list is empty")
    void createWithCandidates_shouldNotCallAddCandidate_whenNoCandidates() {
        // Given
        Election createdElection = new Election(ELECTION_ID, "ELEC-03", "Empty Election",
                ElectionStatus.PROGRAMADA, START, END, false, 1);
        when(createElectionUseCase.create(any())).thenReturn(createdElection);

        CreateElectionCommand electionCmd = new CreateElectionCommand(
                "ELEC-03", "Empty Election", null, START, END, false, 1);

        // When
        Election result = service.createWithCandidates(electionCmd, List.of());

        // Then
        assertThat(result).isEqualTo(createdElection);
        verify(createElectionUseCase).create(electionCmd);
        verifyNoInteractions(addCandidateUseCase);
    }

    @Test
    @DisplayName("Should propagate exception from AddCandidateUseCase (transaction rollback)")
    void createWithCandidates_shouldPropagateException_whenAddCandidateFails() {
        // Given
        Election createdElection = new Election(ELECTION_ID, "ELEC-04", "Failing Election",
                ElectionStatus.PROGRAMADA, START, END, true, 1);
        when(createElectionUseCase.create(any())).thenReturn(createdElection);
        doThrow(new RuntimeException("Duplicate numero_orden"))
                .when(addCandidateUseCase).addCandidate(any());

        CreateElectionCommand electionCmd = new CreateElectionCommand(
                "ELEC-04", "Failing Election", null, START, END, true, 1);

        List<CandidateCreationData> candidates = List.of(
                new CandidateCreationData("Candidato A", 1, null, null, null, null)
        );

        // When & Then
        assertThatThrownBy(() -> service.createWithCandidates(electionCmd, candidates))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Duplicate numero_orden");

        verify(createElectionUseCase).create(electionCmd);
    }

    @Test
    @DisplayName("Should NOT call AddCandidateUseCase when CreateElectionUseCase fails")
    void createWithCandidates_shouldNotCallAddCandidate_whenCreateFails() {
        // Given
        doThrow(new RuntimeException("Election creation failed"))
                .when(createElectionUseCase).create(any());

        CreateElectionCommand electionCmd = new CreateElectionCommand(
                "ELEC-05", "Bad Election", null, START, END, true, 1);

        List<CandidateCreationData> candidates = List.of(
                new CandidateCreationData("Candidato A", 1, null, null, null, null)
        );

        // When & Then
        assertThatThrownBy(() -> service.createWithCandidates(electionCmd, candidates))
                .isInstanceOf(RuntimeException.class);

        verify(createElectionUseCase).create(any());
        verifyNoInteractions(addCandidateUseCase);
    }

    @Test
    @DisplayName("Should inject created election ID and rich profile into each AddCandidateCommand")
    void createWithCandidates_shouldInjectElectionIdAndRichFields_intoEachCommand() {
        // Given
        Election createdElection = new Election(ELECTION_ID, "ELEC-06", "Rich Election",
                ElectionStatus.PROGRAMADA, START, END, true, 2);
        when(createElectionUseCase.create(any())).thenReturn(createdElection);

        Candidate stub = new Candidate(UUID.randomUUID(), ELECTION_ID, "A", false, false, 1,
                "http://foto.png", "Bio", "Propuestas", "Partido X");
        when(addCandidateUseCase.addCandidate(any())).thenReturn(stub);

        CreateElectionCommand electionCmd = new CreateElectionCommand(
                "ELEC-06", "Rich Election", null, START, END, true, 2);

        List<CandidateCreationData> candidates = List.of(
                new CandidateCreationData("Candidato A", 1,
                        "http://foto.png", "Bio", "Propuestas", "Partido X")
        );

        // When
        service.createWithCandidates(electionCmd, candidates);

        // Then: the AddCandidateCommand passed to the use case must have the created election's id
        ArgumentCaptor<AddCandidateCommand> captor = ArgumentCaptor.forClass(AddCandidateCommand.class);
        verify(addCandidateUseCase).addCandidate(captor.capture());
        AddCandidateCommand captured = captor.getValue();
        assertThat(captured.eleccionId()).isEqualTo(ELECTION_ID);
        assertThat(captured.nombre()).isEqualTo("Candidato A");
        assertThat(captured.numeroOrden()).isEqualTo(1);
        assertThat(captured.fotoUrl()).isEqualTo("http://foto.png");
        assertThat(captured.biografia()).isEqualTo("Bio");
        assertThat(captured.propuestas()).isEqualTo("Propuestas");
        assertThat(captured.afiliacionPolitica()).isEqualTo("Partido X");
    }

    // ── @Transactional contract ───────────────────────────────────────────────

    @Test
    @DisplayName("Should have @Transactional on createWithCandidates() method")
    void createWithCandidates_shouldHaveTransactionalAnnotation() throws NoSuchMethodException {
        // Given
        Method method = CreateElectionWithCandidatesAppService.class
                .getMethod("createWithCandidates", CreateElectionCommand.class, List.class);

        // When / Then
        assertThat(method.isAnnotationPresent(Transactional.class))
                .as("createWithCandidates() must be annotated with @Transactional")
                .isTrue();
    }
}
