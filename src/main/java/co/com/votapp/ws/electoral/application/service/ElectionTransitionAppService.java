package co.com.votapp.ws.electoral.application.service;

import co.com.votapp.ws.electoral.domain.port.in.ActivateElectionUseCase;
import co.com.votapp.ws.electoral.domain.port.in.FinalizeElectionUseCase;
import co.com.votapp.ws.voting.domain.port.in.BulkIssueTokensUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Application service that adds {@code @Transactional} semantics to election state transitions.
 *
 * <p>This is a thin wrapper in the application layer. It provides a Spring-managed transaction
 * boundary around the domain use case calls. It contains NO business logic — all rules live
 * in the domain use cases.
 *
 * <h3>Activation flow</h3>
 * <p>{@code activate()} orchestrates two domain operations inside a single transaction:
 * <ol>
 *   <li>{@link ActivateElectionUseCase#activate(UUID)} — transitions the election to ACTIVA
 *       and creates the synthetic "Voto en Blanco" candidate.</li>
 *   <li>{@link BulkIssueTokensUseCase#issueForElection(UUID)} — issues voting tokens
 *       for all eligible census members (or globally eligible funcionarios if census is empty).</li>
 * </ol>
 * <p>If issuance fails, the whole transaction rolls back — the election stays PROGRAMADA.
 *
 * <h3>Why this exists</h3>
 * <p>Domain use cases are pure Java with no Spring annotations. {@code @Transactional} requires
 * a Spring proxy. This application service provides the proxy boundary while keeping the domain
 * clean. Mirrors the {@code CastVoteAppService} pattern in the voting bounded context.
 *
 * <h3>Scheduler inheritance</h3>
 * <p>{@link co.com.votapp.ws.electoral.infrastructure.adapter.in.scheduler.ElectionScheduler}
 * already calls {@code ElectionTransitionAppService.activate()} — it inherits bulk issuance
 * automatically without any changes.
 */
@Service
public class ElectionTransitionAppService {

    private final ActivateElectionUseCase activateElectionUseCase;
    private final FinalizeElectionUseCase finalizeElectionUseCase;
    private final BulkIssueTokensUseCase bulkIssueTokensUseCase;

    public ElectionTransitionAppService(ActivateElectionUseCase activateElectionUseCase,
                                        FinalizeElectionUseCase finalizeElectionUseCase,
                                        BulkIssueTokensUseCase bulkIssueTokensUseCase) {
        this.activateElectionUseCase = activateElectionUseCase;
        this.finalizeElectionUseCase = finalizeElectionUseCase;
        this.bulkIssueTokensUseCase = bulkIssueTokensUseCase;
    }

    /**
     * Activate an election (PROGRAMADA → ACTIVA) and issue bulk voting tokens — all in one transaction.
     *
     * <p>Calls {@link ActivateElectionUseCase#activate(UUID)} first, then
     * {@link BulkIssueTokensUseCase#issueForElection(UUID)}. If issuance fails,
     * the whole transaction rolls back — the election remains PROGRAMADA.
     *
     * @param electionId the UUID of the election to activate
     */
    @Transactional
    public void activate(UUID electionId) {
        activateElectionUseCase.activate(electionId);
        bulkIssueTokensUseCase.issueForElection(electionId);
    }

    /**
     * Finalize an election (ACTIVA → FINALIZADA) within a transaction.
     *
     * <p>Delegates to {@link FinalizeElectionUseCase#finalize(UUID)}.
     *
     * @param electionId the UUID of the election to finalize
     */
    @Transactional
    public void finalize(UUID electionId) {
        finalizeElectionUseCase.finalize(electionId);
    }
}
