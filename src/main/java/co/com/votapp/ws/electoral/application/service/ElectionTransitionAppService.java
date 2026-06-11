package co.com.votapp.ws.electoral.application.service;

import co.com.votapp.ws.electoral.domain.port.in.ActivateElectionUseCase;
import co.com.votapp.ws.electoral.domain.port.in.FinalizeElectionUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Application service that adds {@code @Transactional} semantics to election state transitions.
 *
 * <p>This is a thin wrapper in the application layer. Its ONLY responsibility is to provide
 * a Spring-managed transaction boundary around the domain use case calls.
 * It contains NO business logic — all rules live in the domain use cases.
 *
 * <h3>Why this exists</h3>
 * <p>Domain use cases ({@link ActivateElectionUseCaseImpl}, {@link FinalizeElectionUseCaseImpl})
 * are pure Java with no Spring annotations. {@code @Transactional} requires a Spring proxy.
 * This application service provides the proxy boundary while keeping the domain clean.
 * Mirrors the {@code CastVoteAppService} pattern in the voting bounded context.
 */
@Service
public class ElectionTransitionAppService {

    private final ActivateElectionUseCase activateElectionUseCase;
    private final FinalizeElectionUseCase finalizeElectionUseCase;

    public ElectionTransitionAppService(ActivateElectionUseCase activateElectionUseCase,
                                        FinalizeElectionUseCase finalizeElectionUseCase) {
        this.activateElectionUseCase = activateElectionUseCase;
        this.finalizeElectionUseCase = finalizeElectionUseCase;
    }

    /**
     * Activate an election (PROGRAMADA → ACTIVA) within a transaction.
     *
     * <p>Delegates to {@link ActivateElectionUseCase#activate(UUID)}.
     * Also creates the synthetic "Voto en Blanco" candidate atomically.
     *
     * @param electionId the UUID of the election to activate
     */
    @Transactional
    public void activate(UUID electionId) {
        activateElectionUseCase.activate(electionId);
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
