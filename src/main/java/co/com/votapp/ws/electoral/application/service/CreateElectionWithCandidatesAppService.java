package co.com.votapp.ws.electoral.application.service;

import co.com.votapp.ws.electoral.application.command.AddCandidateCommand;
import co.com.votapp.ws.electoral.application.command.CreateElectionCommand;
import co.com.votapp.ws.electoral.domain.Election;
import co.com.votapp.ws.electoral.domain.port.in.AddCandidateUseCase;
import co.com.votapp.ws.electoral.domain.port.in.CreateElectionUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Application service that creates an election together with its candidates in a single
 * {@code @Transactional} operation — the "comprehensive submit" endpoint backing the admin wizard.
 *
 * <h3>Why this exists</h3>
 * <p>The admin wizard collects Basic Info, Candidates, and Ballot Config locally (no DB round-trips)
 * and submits everything atomically on the final Review step. A single transaction guarantees that
 * either the election AND all candidates are persisted, or nothing is — no orphan elections.
 *
 * <h3>V5 changes</h3>
 * <p>{@link CandidateCreationData} now uses {@code funcionarioId} instead of {@code numeroOrden}.
 * {@code afiliacionPolitica} is removed. If the same funcionario appears twice in the candidate list,
 * {@link AddCandidateUseCase} will throw a {@link co.com.votapp.ws.common.exception.DomainException}
 * (409) rolling back the entire transaction.
 */
@Service
public class CreateElectionWithCandidatesAppService {

    private final CreateElectionUseCase createElectionUseCase;
    private final AddCandidateUseCase addCandidateUseCase;

    public CreateElectionWithCandidatesAppService(CreateElectionUseCase createElectionUseCase,
                                                   AddCandidateUseCase addCandidateUseCase) {
        this.createElectionUseCase = createElectionUseCase;
        this.addCandidateUseCase = addCandidateUseCase;
    }

    /**
     * Create an election with all its candidates in one atomic transaction.
     *
     * @param electionCommand  the command carrying election metadata and ballot config
     * @param candidatesData   the unrooted candidate data records (no election ID yet)
     * @return the persisted {@link Election} in {@code PROGRAMADA} state
     */
    @Transactional
    public Election createWithCandidates(CreateElectionCommand electionCommand,
                                          List<CandidateCreationData> candidatesData) {
        Election election = createElectionUseCase.create(electionCommand);

        for (CandidateCreationData data : candidatesData) {
            AddCandidateCommand candidateCommand = new AddCandidateCommand(
                    election.id(),
                    data.nombre(),
                    "",
                    data.funcionarioId(),
                    data.fotoUrl(),
                    data.biografia(),
                    data.propuestas()
            );
            addCandidateUseCase.addCandidate(candidateCommand);
        }

        return election;
    }

    /**
     * Unrooted candidate data for the comprehensive election creation flow.
     *
     * <p>Does NOT include {@code eleccionId} — the service injects it after the election is created.
     * All rich profile fields are optional (nullable).
     *
     * <p>V5: {@code funcionarioId} replaces {@code numeroOrden}; {@code afiliacionPolitica} removed.
     *
     * @param nombre            the candidate's display name (required)
     * @param funcionarioId     optional FK to funcionarios (nullable; synthetics pass null)
     * @param fotoUrl           optional photo URL
     * @param biografia         optional biography text
     * @param propuestas        optional proposals text
     */
    public record CandidateCreationData(
            String nombre,
            Integer funcionarioId,
            String fotoUrl,
            String biografia,
            String propuestas
    ) {}
}
