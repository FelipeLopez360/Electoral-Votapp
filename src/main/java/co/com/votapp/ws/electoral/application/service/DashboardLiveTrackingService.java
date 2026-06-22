package co.com.votapp.ws.electoral.application.service;

import co.com.votapp.ws.auth.domain.port.out.FuncionarioRepositoryPort;
import co.com.votapp.ws.electoral.application.dto.LiveTrackingResponse;
import co.com.votapp.ws.electoral.domain.Election;
import co.com.votapp.ws.electoral.domain.ElectionStatus;
import co.com.votapp.ws.electoral.domain.port.out.CensoRepositoryPort;
import co.com.votapp.ws.electoral.domain.port.out.ElectionRepositoryPort;
import co.com.votapp.ws.voting.domain.port.out.ParticipacionRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Application service that composes live participation tracking data for the admin dashboard.
 *
 * <h3>Responsibilities</h3>
 * <ol>
 *   <li>Retrieve all currently ACTIVA elections via {@link ElectionRepositoryPort}.</li>
 *   <li>For each election, count cast votes via {@link ParticipacionRepositoryPort}.</li>
 *   <li>For each election, resolve the eligible voter count with the census-first fallback:
 *       <ul>
 *         <li>If {@code censoRepository.countByEleccionId(id) > 0}, use the census count.</li>
 *         <li>Otherwise, fall back to {@code funcionarioRepository.countActiveEligibleVoters()}.</li>
 *       </ul>
 *   </li>
 *   <li>Assemble and return a {@link LiveTrackingResponse} DTO.</li>
 * </ol>
 *
 * <h3>Why here and not in a domain use case</h3>
 * <p>This is a pure read composition with zero business rules — no state transition,
 * no invariant to enforce. Mirrors the existing {@link DashboardMetricsService} pattern.
 * No {@code DomainConfig} wiring needed — Spring component-scans this as an {@code @Service}.
 *
 * <h3>Cross-context wiring</h3>
 * <p>{@link ParticipacionRepositoryPort} is from the voting context;
 * {@link FuncionarioRepositoryPort} is from the auth context.
 * Ports are the sanctioned seam for cross-context reads per the project architecture decisions.
 */
@Service
public class DashboardLiveTrackingService {

    private final ElectionRepositoryPort electionRepository;
    private final ParticipacionRepositoryPort participacionRepository;
    private final CensoRepositoryPort censoRepository;
    private final FuncionarioRepositoryPort funcionarioRepository;

    public DashboardLiveTrackingService(
            ElectionRepositoryPort electionRepository,
            ParticipacionRepositoryPort participacionRepository,
            CensoRepositoryPort censoRepository,
            FuncionarioRepositoryPort funcionarioRepository) {
        this.electionRepository = electionRepository;
        this.participacionRepository = participacionRepository;
        this.censoRepository = censoRepository;
        this.funcionarioRepository = funcionarioRepository;
    }

    /**
     * Retrieve per-election participation progress for all active elections.
     *
     * @return {@link LiveTrackingResponse} with an item per ACTIVA election
     *         (empty list if no elections are active)
     */
    public LiveTrackingResponse getLiveTracking() {
        List<Election> activeElections = electionRepository.findByStatus(ElectionStatus.ACTIVA);

        List<LiveTrackingResponse.LiveTrackingItem> items = activeElections.stream()
                .map(this::buildTrackingItem)
                .toList();

        return new LiveTrackingResponse(items);
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    /**
     * Build a {@link LiveTrackingResponse.LiveTrackingItem} for a single active election.
     *
     * <p>Eligible voters: census-first fallback.
     * If the election has a custom census (count > 0), use it.
     * Otherwise fall back to the global active voters count.
     */
    private LiveTrackingResponse.LiveTrackingItem buildTrackingItem(Election election) {
        long totalCastVotes = participacionRepository.countByEleccionId(election.id());

        long censusCount = censoRepository.countByEleccionId(election.id());
        long totalEligibleVoters = censusCount > 0
                ? censusCount
                : funcionarioRepository.countActiveEligibleVoters();

        return new LiveTrackingResponse.LiveTrackingItem(
                election.id(),
                election.nombre(),
                totalCastVotes,
                totalEligibleVoters
        );
    }
}
