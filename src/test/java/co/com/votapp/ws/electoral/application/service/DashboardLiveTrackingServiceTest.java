package co.com.votapp.ws.electoral.application.service;

import co.com.votapp.ws.auth.domain.port.out.FuncionarioRepositoryPort;
import co.com.votapp.ws.electoral.application.dto.LiveTrackingResponse;
import co.com.votapp.ws.electoral.domain.Election;
import co.com.votapp.ws.electoral.domain.ElectionStatus;
import co.com.votapp.ws.electoral.domain.port.out.CensoRepositoryPort;
import co.com.votapp.ws.electoral.domain.port.out.ElectionRepositoryPort;
import co.com.votapp.ws.voting.domain.port.out.ParticipacionRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DashboardLiveTrackingService}.
 *
 * <p>TDD: RED phase written before production code (tasks 2.2–2.5).
 *
 * <p>Spec scenarios covered:
 * <ul>
 *   <li>2.2 — Census > 0: uses census count, NEVER calls global voters</li>
 *   <li>2.3 — Census == 0: falls back to global active voters</li>
 *   <li>2.4 — No active elections: returns empty list</li>
 *   <li>2.5 — Zero participation: reports totalCastVotes = 0</li>
 * </ul>
 */
@DisplayName("DashboardLiveTrackingService - Live tracking orchestration and fallback logic")
@ExtendWith(MockitoExtension.class)
class DashboardLiveTrackingServiceTest {

    @Mock
    private ElectionRepositoryPort electionRepository;

    @Mock
    private ParticipacionRepositoryPort participacionRepository;

    @Mock
    private CensoRepositoryPort censoRepository;

    @Mock
    private FuncionarioRepositoryPort funcionarioRepository;

    private DashboardLiveTrackingService service;

    @BeforeEach
    void setUp() {
        service = new DashboardLiveTrackingService(
                electionRepository, participacionRepository, censoRepository, funcionarioRepository);
    }

    // ─── Task 2.4: empty list when no active elections ────────────────────────

    @Test
    @DisplayName("Should return empty list when no elections are ACTIVA")
    void getLiveTracking_shouldReturnEmptyList_whenNoActiveElections() {
        // Given
        when(electionRepository.findByStatus(ElectionStatus.ACTIVA)).thenReturn(List.of());

        // When
        LiveTrackingResponse response = service.getLiveTracking();

        // Then
        assertThat(response.elections()).isEmpty();
        verify(electionRepository).findByStatus(ElectionStatus.ACTIVA);
    }

    // ─── Task 2.2: census > 0 path — must NOT call global voters ─────────────

    @Test
    @DisplayName("Should use census count when census > 0 and MUST NOT query global voters")
    void getLiveTracking_shouldUseCensusCount_whenCensusIsGreaterThanZero() {
        // Given
        UUID eleccionId = UUID.randomUUID();
        Election activeElection = buildElection(eleccionId, "ELEC-001", "Election One");
        when(electionRepository.findByStatus(ElectionStatus.ACTIVA))
                .thenReturn(List.of(activeElection));
        when(participacionRepository.countByEleccionId(eleccionId)).thenReturn(150L);
        when(censoRepository.countByEleccionId(eleccionId)).thenReturn(200L); // census > 0

        // When
        LiveTrackingResponse response = service.getLiveTracking();

        // Then
        assertThat(response.elections()).hasSize(1);
        LiveTrackingResponse.LiveTrackingItem item = response.elections().get(0);
        assertThat(item.id()).isEqualTo(eleccionId);
        assertThat(item.title()).isEqualTo("Election One");
        assertThat(item.totalCastVotes()).isEqualTo(150L);
        assertThat(item.totalEligibleVoters()).isEqualTo(200L);

        // MUST NOT call global voters when census is populated
        verify(funcionarioRepository, never()).countActiveEligibleVoters();
    }

    // ─── Task 2.3: census == 0 path — must fall back to global voters ─────────

    @Test
    @DisplayName("Should fall back to global active voters when census count is 0")
    void getLiveTracking_shouldFallBackToGlobalVoters_whenCensusIsZero() {
        // Given
        UUID eleccionId = UUID.randomUUID();
        Election activeElection = buildElection(eleccionId, "ELEC-002", "Election Two");
        when(electionRepository.findByStatus(ElectionStatus.ACTIVA))
                .thenReturn(List.of(activeElection));
        when(participacionRepository.countByEleccionId(eleccionId)).thenReturn(50L);
        when(censoRepository.countByEleccionId(eleccionId)).thenReturn(0L); // census == 0
        when(funcionarioRepository.countActiveEligibleVoters()).thenReturn(500L);

        // When
        LiveTrackingResponse response = service.getLiveTracking();

        // Then
        assertThat(response.elections()).hasSize(1);
        LiveTrackingResponse.LiveTrackingItem item = response.elections().get(0);
        assertThat(item.totalEligibleVoters()).isEqualTo(500L);
        assertThat(item.totalCastVotes()).isEqualTo(50L);

        // Global voters MUST be queried when census is empty
        verify(funcionarioRepository).countActiveEligibleVoters();
    }

    // ─── Task 2.5: zero cast votes ────────────────────────────────────────────

    @Test
    @DisplayName("Should report totalCastVotes = 0 when there are no participation records")
    void getLiveTracking_shouldReportZeroCastVotes_whenNoParticipationExists() {
        // Given
        UUID eleccionId = UUID.randomUUID();
        Election activeElection = buildElection(eleccionId, "ELEC-003", "Election Three");
        when(electionRepository.findByStatus(ElectionStatus.ACTIVA))
                .thenReturn(List.of(activeElection));
        when(participacionRepository.countByEleccionId(eleccionId)).thenReturn(0L);
        when(censoRepository.countByEleccionId(eleccionId)).thenReturn(300L);

        // When
        LiveTrackingResponse response = service.getLiveTracking();

        // Then
        assertThat(response.elections()).hasSize(1);
        assertThat(response.elections().get(0).totalCastVotes()).isEqualTo(0L);
    }

    // ─── Multiple elections ───────────────────────────────────────────────────

    @Test
    @DisplayName("Should handle multiple active elections independently applying fallback per election")
    void getLiveTracking_shouldHandleMultipleElections_withIndependentFallbackPerElection() {
        // Given
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        Election elec1 = buildElection(id1, "ELEC-A", "Election A");  // census > 0
        Election elec2 = buildElection(id2, "ELEC-B", "Election B");  // census == 0
        when(electionRepository.findByStatus(ElectionStatus.ACTIVA))
                .thenReturn(List.of(elec1, elec2));

        when(participacionRepository.countByEleccionId(id1)).thenReturn(10L);
        when(censoRepository.countByEleccionId(id1)).thenReturn(100L); // census > 0

        when(participacionRepository.countByEleccionId(id2)).thenReturn(5L);
        when(censoRepository.countByEleccionId(id2)).thenReturn(0L);   // census == 0
        when(funcionarioRepository.countActiveEligibleVoters()).thenReturn(250L);

        // When
        LiveTrackingResponse response = service.getLiveTracking();

        // Then
        assertThat(response.elections()).hasSize(2);
        // election 1: census path
        LiveTrackingResponse.LiveTrackingItem item1 = response.elections().stream()
                .filter(i -> i.id().equals(id1)).findFirst().orElseThrow();
        assertThat(item1.totalEligibleVoters()).isEqualTo(100L);

        // election 2: global fallback path
        LiveTrackingResponse.LiveTrackingItem item2 = response.elections().stream()
                .filter(i -> i.id().equals(id2)).findFirst().orElseThrow();
        assertThat(item2.totalEligibleVoters()).isEqualTo(250L);

        // Global was called ONCE (per election that needs it) — but in this test only once
        verify(funcionarioRepository).countActiveEligibleVoters();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private Election buildElection(UUID id, String codigo, String nombre) {
        return new Election(
                id, codigo, nombre, ElectionStatus.ACTIVA,
                LocalDateTime.of(2026, 6, 1, 0, 0),
                LocalDateTime.of(2026, 6, 30, 0, 0),
                true, 1
        );
    }
}
