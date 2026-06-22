package co.com.votapp.ws.electoral.application.service;

import co.com.votapp.ws.auth.domain.port.out.FuncionarioRepositoryPort;
import co.com.votapp.ws.electoral.application.dto.DashboardMetricsResponse;
import co.com.votapp.ws.electoral.domain.ElectionStatus;
import co.com.votapp.ws.electoral.domain.port.out.ElectionRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Application service that composes aggregate counts for the admin dashboard.
 *
 * <h3>Responsibilities</h3>
 * <ol>
 *   <li>Retrieve election counts grouped by status from {@link ElectionRepositoryPort}.</li>
 *   <li>Retrieve funcionario counts grouped by labor status from {@link FuncionarioRepositoryPort}.</li>
 *   <li>Retrieve the global active-eligible-voter count from {@link FuncionarioRepositoryPort}.</li>
 *   <li>Zero-fill all 5 {@link ElectionStatus} keys and the minimum funcionario status keys
 *       (ACTIVO, INACTIVO) so the response is always complete regardless of DB state.</li>
 *   <li>Compute totals as the sum of status-map values.</li>
 * </ol>
 *
 * <h3>Why here and not in a domain use case</h3>
 * <p>This is a pure read composition with zero business rules — no state transition,
 * no invariant to enforce. Per the design decision recorded in {@code design.md}, this
 * mirrors the existing {@link CreateElectionWithCandidatesAppService} / {@link ResultsAppService}
 * pattern: an {@code @Service} in {@code electoral/application/service/} that orchestrates
 * output-port calls and assembles a DTO for the web adapter.
 *
 * <p>No {@code DomainConfig} wiring needed — Spring component-scans this as an {@code @Service}.
 */
@Service
public class DashboardMetricsService {

    /** Known funcionario labor statuses guaranteed to appear in the response (zero-filled if absent). */
    private static final String[] KNOWN_LABOR_STATUSES = {"ACTIVO", "INACTIVO"};

    private final FuncionarioRepositoryPort funcionarioRepository;
    private final ElectionRepositoryPort electionRepository;

    public DashboardMetricsService(FuncionarioRepositoryPort funcionarioRepository,
                                    ElectionRepositoryPort electionRepository) {
        this.funcionarioRepository = funcionarioRepository;
        this.electionRepository = electionRepository;
    }

    /**
     * Retrieve and assemble aggregated dashboard metrics.
     *
     * @return {@link DashboardMetricsResponse} with all status keys present and zero-filled
     */
    public DashboardMetricsResponse getMetrics() {
        // ── Elections ──────────────────────────────────────────────────────────
        // Port already zero-fills all 5 ElectionStatus values (ElectionRepositoryAdapter contract)
        Map<String, Long> electionCounts = electionRepository.countByStatus();
        // Guard: ensure all enum values are present even if adapter misses one
        Map<String, Long> electionsByStatus = zeroFillElectionStatuses(electionCounts);
        long electionsTotal = electionsByStatus.values().stream().mapToLong(Long::longValue).sum();

        // ── Funcionarios ───────────────────────────────────────────────────────
        Map<String, Long> laborCounts = funcionarioRepository.countByEstadoLaboral();
        Map<String, Long> funcionariosByStatus = zeroFillLaborStatuses(laborCounts);
        long funcionariosTotal = funcionariosByStatus.values().stream().mapToLong(Long::longValue).sum();

        // ── Active voters ──────────────────────────────────────────────────────
        long activeVoters = funcionarioRepository.countActiveEligibleVoters();

        return new DashboardMetricsResponse(
                new DashboardMetricsResponse.FuncionarioBlock(funcionariosTotal, funcionariosByStatus),
                activeVoters,
                new DashboardMetricsResponse.ElectionBlock(electionsTotal, electionsByStatus)
        );
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    /**
     * Returns a copy of {@code counts} with all five {@link ElectionStatus} keys guaranteed present.
     * Missing keys are inserted with value {@code 0}.
     */
    private static Map<String, Long> zeroFillElectionStatuses(Map<String, Long> counts) {
        Map<String, Long> result = new HashMap<>(counts);
        for (ElectionStatus status : EnumSet.allOf(ElectionStatus.class)) {
            result.putIfAbsent(status.name(), 0L);
        }
        return Map.copyOf(result);
    }

    /**
     * Returns a copy of {@code counts} with the known labor status keys guaranteed present.
     * Missing keys are inserted with value {@code 0}.
     */
    private static Map<String, Long> zeroFillLaborStatuses(Map<String, Long> counts) {
        Map<String, Long> result = new HashMap<>(counts);
        for (String status : KNOWN_LABOR_STATUSES) {
            result.putIfAbsent(status, 0L);
        }
        return Map.copyOf(result);
    }
}
