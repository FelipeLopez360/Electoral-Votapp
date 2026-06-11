package co.com.votapp.ws.electoral.infrastructure.adapter.in.scheduler;

import co.com.votapp.ws.electoral.application.service.ElectionTransitionAppService;
import co.com.votapp.ws.electoral.domain.Election;
import co.com.votapp.ws.electoral.domain.ElectionStatus;
import co.com.votapp.ws.electoral.domain.port.out.ElectionRepositoryPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Driving adapter (input side): scheduled poller that auto-transitions elections
 * based on their configured {@code fechaInicio} and {@code fechaFin}.
 *
 * <p>Runs every 60 seconds. Each election is processed independently inside its own
 * {@code try/catch} block — one failure never aborts the rest of the batch (Fault Isolation
 * requirement from spec).
 *
 * <p>Transitions delegated to {@link ElectionTransitionAppService}, which provides the
 * {@code @Transactional} boundary and delegates to the pure domain use cases.
 */
@Component
@Slf4j
public class ElectionScheduler {

    private final ElectionRepositoryPort electionRepository;
    private final ElectionTransitionAppService electionTransitionAppService;

    public ElectionScheduler(ElectionRepositoryPort electionRepository,
                              ElectionTransitionAppService electionTransitionAppService) {
        this.electionRepository = electionRepository;
        this.electionTransitionAppService = electionTransitionAppService;
    }

    /**
     * Scheduled tick: activate elections past their {@code fechaInicio} and finalize
     * elections past their {@code fechaFin}.
     *
     * <p>Uses {@code LocalDateTime.now(ZoneOffset.UTC)} so comparisons stay consistent
     * with the UTC {@code Instant} values stored in the database.
     */
    @Scheduled(fixedRate = 60_000)
    public void processElections() {
        LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);

        List<Election> toActivate = electionRepository
                .findByStatusAndFechaInicioLessThanEqual(ElectionStatus.PROGRAMADA, nowUtc);

        List<Election> toFinalize = electionRepository
                .findByStatusAndFechaFinLessThanEqual(ElectionStatus.ACTIVA, nowUtc);

        log.info("Election scheduler: processing {} due elections ({} to activate, {} to finalize)",
                toActivate.size() + toFinalize.size(), toActivate.size(), toFinalize.size());

        for (Election election : toActivate) {
            try {
                electionTransitionAppService.activate(election.id());
            } catch (Exception e) {
                log.error("Failed to activate election {}: {}", election.id(), e.getMessage(), e);
            }
        }

        for (Election election : toFinalize) {
            try {
                electionTransitionAppService.finalize(election.id());
            } catch (Exception e) {
                log.error("Failed to finalize election {}: {}", election.id(), e.getMessage(), e);
            }
        }
    }
}
