package co.com.votapp.ws.electoral.infrastructure.adapter.in.scheduler;

import co.com.votapp.ws.electoral.application.service.ElectionTransitionAppService;
import co.com.votapp.ws.electoral.domain.Election;
import co.com.votapp.ws.electoral.domain.ElectionStatus;
import co.com.votapp.ws.electoral.domain.port.out.ElectionRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("ElectionScheduler - Scheduled poller for election state transitions")
@ExtendWith(MockitoExtension.class)
class ElectionSchedulerTest {

    @Mock
    private ElectionRepositoryPort electionRepository;

    @Mock
    private ElectionTransitionAppService electionTransitionAppService;

    private ElectionScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new ElectionScheduler(electionRepository, electionTransitionAppService);
    }

    // ─── Happy path: activate due elections ─────────────────────────────────

    @Test
    @DisplayName("Should call activate for each PROGRAMADA election with past fechaInicio")
    void processElections_shouldActivateDueElections_whenProgramadaElectionsAreDue() {
        // Given
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        Election e1 = electionWith(id1, ElectionStatus.PROGRAMADA);
        Election e2 = electionWith(id2, ElectionStatus.PROGRAMADA);

        when(electionRepository.findByStatusAndFechaInicioLessThanEqual(eq(ElectionStatus.PROGRAMADA), any(LocalDateTime.class)))
                .thenReturn(List.of(e1, e2));
        when(electionRepository.findByStatusAndFechaFinLessThanEqual(eq(ElectionStatus.ACTIVA), any(LocalDateTime.class)))
                .thenReturn(List.of());

        // When
        scheduler.processElections();

        // Then
        verify(electionTransitionAppService).activate(id1);
        verify(electionTransitionAppService).activate(id2);
    }

    @Test
    @DisplayName("Should call finalize for each ACTIVA election with past fechaFin")
    void processElections_shouldFinalizeExpiredElections_whenActivaElectionsAreExpired() {
        // Given
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        Election e1 = electionWith(id1, ElectionStatus.ACTIVA);
        Election e2 = electionWith(id2, ElectionStatus.ACTIVA);

        when(electionRepository.findByStatusAndFechaInicioLessThanEqual(eq(ElectionStatus.PROGRAMADA), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(electionRepository.findByStatusAndFechaFinLessThanEqual(eq(ElectionStatus.ACTIVA), any(LocalDateTime.class)))
                .thenReturn(List.of(e1, e2));

        // When
        scheduler.processElections();

        // Then
        verify(electionTransitionAppService).finalize(id1);
        verify(electionTransitionAppService).finalize(id2);
    }

    @Test
    @DisplayName("Should query PROGRAMADA then ACTIVA elections in order")
    void processElections_shouldQueryProgramadaBeforeActiva_inOrder() {
        // Given
        when(electionRepository.findByStatusAndFechaInicioLessThanEqual(eq(ElectionStatus.PROGRAMADA), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(electionRepository.findByStatusAndFechaFinLessThanEqual(eq(ElectionStatus.ACTIVA), any(LocalDateTime.class)))
                .thenReturn(List.of());

        // When
        scheduler.processElections();

        // Then
        InOrder inOrder = inOrder(electionRepository);
        inOrder.verify(electionRepository).findByStatusAndFechaInicioLessThanEqual(eq(ElectionStatus.PROGRAMADA), any(LocalDateTime.class));
        inOrder.verify(electionRepository).findByStatusAndFechaFinLessThanEqual(eq(ElectionStatus.ACTIVA), any(LocalDateTime.class));
    }

    // ─── Fault isolation ────────────────────────────────────────────────────

    @Test
    @DisplayName("Should continue activating remaining elections when one throws an exception")
    void processElections_shouldNotStopOnFailure_whenOneActivationThrows() {
        // Given
        UUID failingId = UUID.randomUUID();
        UUID successId = UUID.randomUUID();
        Election failing = electionWith(failingId, ElectionStatus.PROGRAMADA);
        Election success = electionWith(successId, ElectionStatus.PROGRAMADA);

        when(electionRepository.findByStatusAndFechaInicioLessThanEqual(eq(ElectionStatus.PROGRAMADA), any(LocalDateTime.class)))
                .thenReturn(List.of(failing, success));
        when(electionRepository.findByStatusAndFechaFinLessThanEqual(eq(ElectionStatus.ACTIVA), any(LocalDateTime.class)))
                .thenReturn(List.of());

        doThrow(new RuntimeException("DB constraint violation"))
                .when(electionTransitionAppService).activate(failingId);

        // When — must NOT throw
        scheduler.processElections();

        // Then — the second election is still processed
        verify(electionTransitionAppService).activate(failingId);
        verify(electionTransitionAppService).activate(successId);
    }

    @Test
    @DisplayName("Should continue finalizing remaining elections when one throws an exception")
    void processElections_shouldNotStopOnFailure_whenOneFinalizationThrows() {
        // Given
        UUID failingId = UUID.randomUUID();
        UUID successId = UUID.randomUUID();
        Election failing = electionWith(failingId, ElectionStatus.ACTIVA);
        Election success = electionWith(successId, ElectionStatus.ACTIVA);

        when(electionRepository.findByStatusAndFechaInicioLessThanEqual(eq(ElectionStatus.PROGRAMADA), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(electionRepository.findByStatusAndFechaFinLessThanEqual(eq(ElectionStatus.ACTIVA), any(LocalDateTime.class)))
                .thenReturn(List.of(failing, success));

        doThrow(new RuntimeException("State machine violation"))
                .when(electionTransitionAppService).finalize(failingId);

        // When — must NOT throw
        scheduler.processElections();

        // Then — the second election is still processed
        verify(electionTransitionAppService).finalize(failingId);
        verify(electionTransitionAppService).finalize(successId);
    }

    @Test
    @DisplayName("Should not call app service when no elections are due")
    void processElections_shouldNotCallAppService_whenNoElectionsDue() {
        // Given
        when(electionRepository.findByStatusAndFechaInicioLessThanEqual(eq(ElectionStatus.PROGRAMADA), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(electionRepository.findByStatusAndFechaFinLessThanEqual(eq(ElectionStatus.ACTIVA), any(LocalDateTime.class)))
                .thenReturn(List.of());

        // When
        scheduler.processElections();

        // Then
        verifyNoInteractions(electionTransitionAppService);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private Election electionWith(UUID id, ElectionStatus status) {
        return new Election(
                id,
                "CODE-" + id.toString().substring(0, 8),
                "Election " + id,
                status,
                LocalDateTime.now().minusDays(2),
                LocalDateTime.now().plusDays(5)
        );
    }
}
