package co.com.votapp.ws.electoral.application.service;

import co.com.votapp.ws.auth.domain.port.out.FuncionarioRepositoryPort;
import co.com.votapp.ws.electoral.application.dto.DashboardMetricsResponse;
import co.com.votapp.ws.electoral.domain.ElectionStatus;
import co.com.votapp.ws.electoral.domain.port.out.ElectionRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DashboardMetricsService}.
 *
 * <p>TDD: RED phase written before production code (task 2.1).
 *
 * <p>Verifies:
 * <ul>
 *   <li>Service composes both output ports and returns a valid {@link DashboardMetricsResponse}.</li>
 *   <li>Zero-fills all 5 {@link ElectionStatus} keys when the DB returns a partial map.</li>
 *   <li>Zero-fills missing funcionario status keys (ACTIVO, INACTIVO) when map is partial.</li>
 *   <li>Totals equal the sum of map values.</li>
 *   <li>activeVoters is forwarded as-is from the port.</li>
 * </ul>
 */
@DisplayName("DashboardMetricsService - Aggregate metrics composition")
@ExtendWith(MockitoExtension.class)
class DashboardMetricsServiceTest {

    @Mock
    private FuncionarioRepositoryPort funcionarioRepository;

    @Mock
    private ElectionRepositoryPort electionRepository;

    private DashboardMetricsService service;

    @BeforeEach
    void setUp() {
        service = new DashboardMetricsService(funcionarioRepository, electionRepository);
    }

    // ─── Core composition tests ────────────────────────────────────────────────

    @Test
    @DisplayName("Should return all 5 ElectionStatus keys in elections block, zero-filled")
    void getMetrics_shouldReturnAllElectionStatusKeys_withZeroFillForMissing() {
        // Given — DB only has PROGRAMADA and ACTIVA elections (partial map)
        when(electionRepository.countByStatus()).thenReturn(Map.of(
                "PROGRAMADA", 3L,
                "ACTIVA", 1L
                // FINALIZADA, CANCELADA, SUSPENDIDA missing → should be zero-filled
        ));
        when(funcionarioRepository.countByEstadoLaboral()).thenReturn(Map.of(
                "ACTIVO", 100L,
                "INACTIVO", 20L
        ));
        when(funcionarioRepository.countActiveEligibleVoters()).thenReturn(95L);

        // When
        DashboardMetricsResponse response = service.getMetrics();

        // Then — elections block has all 5 statuses
        assertThat(response.elections().byStatus()).containsKey("PROGRAMADA");
        assertThat(response.elections().byStatus()).containsKey("ACTIVA");
        assertThat(response.elections().byStatus()).containsKey("FINALIZADA");
        assertThat(response.elections().byStatus()).containsKey("CANCELADA");
        assertThat(response.elections().byStatus()).containsKey("SUSPENDIDA");

        // Returned values match port output
        assertThat(response.elections().byStatus().get("PROGRAMADA")).isEqualTo(3L);
        assertThat(response.elections().byStatus().get("ACTIVA")).isEqualTo(1L);
        // Zero-filled missing statuses
        assertThat(response.elections().byStatus().get("FINALIZADA")).isEqualTo(0L);
        assertThat(response.elections().byStatus().get("CANCELADA")).isEqualTo(0L);
        assertThat(response.elections().byStatus().get("SUSPENDIDA")).isEqualTo(0L);
    }

    @Test
    @DisplayName("Should compute elections total as sum of all status counts")
    void getMetrics_shouldComputeElectionsTotal_asSumOfStatusCounts() {
        // Given
        when(electionRepository.countByStatus()).thenReturn(Map.of(
                "PROGRAMADA", 2L,
                "ACTIVA", 1L,
                "FINALIZADA", 4L,
                "CANCELADA", 1L,
                "SUSPENDIDA", 0L
        ));
        when(funcionarioRepository.countByEstadoLaboral()).thenReturn(Map.of("ACTIVO", 50L));
        when(funcionarioRepository.countActiveEligibleVoters()).thenReturn(50L);

        // When
        DashboardMetricsResponse response = service.getMetrics();

        // Then — total must equal 2 + 1 + 4 + 1 + 0 = 8
        assertThat(response.elections().total()).isEqualTo(8L);
    }

    @Test
    @DisplayName("Should zero-fill ACTIVO and INACTIVO when funcionario map is empty")
    void getMetrics_shouldZeroFillFuncionarioStatuses_whenMapIsEmpty() {
        // Given — no funcionarios in DB (empty map from port)
        when(funcionarioRepository.countByEstadoLaboral()).thenReturn(Map.of());
        when(funcionarioRepository.countActiveEligibleVoters()).thenReturn(0L);
        when(electionRepository.countByStatus()).thenReturn(Map.of());

        // When
        DashboardMetricsResponse response = service.getMetrics();

        // Then — funcionarios block has ACTIVO and INACTIVO keys, both zero
        assertThat(response.funcionarios().byStatus()).containsKey("ACTIVO");
        assertThat(response.funcionarios().byStatus()).containsKey("INACTIVO");
        assertThat(response.funcionarios().byStatus().get("ACTIVO")).isEqualTo(0L);
        assertThat(response.funcionarios().byStatus().get("INACTIVO")).isEqualTo(0L);
    }

    @Test
    @DisplayName("Should compute funcionarios total as sum of all status counts")
    void getMetrics_shouldComputeFuncionariosTotal_asSumOfStatusCounts() {
        // Given
        when(funcionarioRepository.countByEstadoLaboral()).thenReturn(Map.of(
                "ACTIVO", 100L,
                "INACTIVO", 20L
        ));
        when(funcionarioRepository.countActiveEligibleVoters()).thenReturn(95L);
        when(electionRepository.countByStatus()).thenReturn(Map.of());

        // When
        DashboardMetricsResponse response = service.getMetrics();

        // Then — total = 100 + 20 = 120
        assertThat(response.funcionarios().total()).isEqualTo(120L);
    }

    @Test
    @DisplayName("Should forward activeVoters count from port as-is")
    void getMetrics_shouldForwardActiveVoters_fromPort() {
        // Given
        when(funcionarioRepository.countByEstadoLaboral()).thenReturn(Map.of("ACTIVO", 100L));
        when(funcionarioRepository.countActiveEligibleVoters()).thenReturn(87L);
        when(electionRepository.countByStatus()).thenReturn(Map.of());

        // When
        DashboardMetricsResponse response = service.getMetrics();

        // Then
        assertThat(response.activeVoters()).isEqualTo(87L);
    }

    @Test
    @DisplayName("Should call all three port methods exactly once")
    void getMetrics_shouldCallAllThreePortMethods_exactlyOnce() {
        // Given
        when(funcionarioRepository.countByEstadoLaboral()).thenReturn(Map.of());
        when(funcionarioRepository.countActiveEligibleVoters()).thenReturn(0L);
        when(electionRepository.countByStatus()).thenReturn(Map.of());

        // When
        service.getMetrics();

        // Then — verify all three port calls
        verify(funcionarioRepository).countByEstadoLaboral();
        verify(funcionarioRepository).countActiveEligibleVoters();
        verify(electionRepository).countByStatus();
    }

    @Test
    @DisplayName("Should return all zeros when both ports return empty maps and zero activeVoters")
    void getMetrics_shouldReturnAllZeros_whenDatabaseIsEmpty() {
        // Given
        when(funcionarioRepository.countByEstadoLaboral()).thenReturn(Map.of());
        when(funcionarioRepository.countActiveEligibleVoters()).thenReturn(0L);
        when(electionRepository.countByStatus()).thenReturn(Map.of());

        // When
        DashboardMetricsResponse response = service.getMetrics();

        // Then
        assertThat(response.activeVoters()).isEqualTo(0L);
        assertThat(response.funcionarios().total()).isEqualTo(0L);
        assertThat(response.elections().total()).isEqualTo(0L);
        response.elections().byStatus().values().forEach(v -> assertThat(v).isEqualTo(0L));
        assertThat(response.funcionarios().byStatus().get("ACTIVO")).isEqualTo(0L);
        assertThat(response.funcionarios().byStatus().get("INACTIVO")).isEqualTo(0L);
    }
}
