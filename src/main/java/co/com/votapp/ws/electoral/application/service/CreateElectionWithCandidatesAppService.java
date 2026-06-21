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
 * <h3>Responsibilities</h3>
 * <ol>
 *   <li>Call {@link CreateElectionUseCase#create(CreateElectionCommand)} to persist the election
 *       in {@code PROGRAMADA} state.</li>
 *   <li>For each {@link CandidateCreationData}, build an {@link AddCandidateCommand} injecting the
 *       newly created election's ID and rich profile fields, then delegate to
 *       {@link AddCandidateUseCase#addCandidate(AddCandidateCommand)}.</li>
 * </ol>
 *
 * <h3>Transaction semantics</h3>
 * <p>If any {@code AddCandidateUseCase} call fails (e.g., duplicate {@code numero_orden}),
 * the whole transaction rolls back — the election and any previously inserted candidates are removed.
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
                    data.numeroOrden(),
                    data.fotoUrl(),
                    data.biografia(),
                    data.propuestas(),
                    data.afiliacionPolitica()
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
     * @param nombre            the candidate's display name (required)
     * @param numeroOrden       display order within the ballot (≥ 1)
     * @param fotoUrl           optional photo URL
     * @param biografia         optional biography text
     * @param propuestas        optional proposals text (MVP: single text field)
     * @param afiliacionPolitica optional political affiliation
     */
    public record CandidateCreationData(
            String nombre,
            int numeroOrden,
            String fotoUrl,
            String biografia,
            String propuestas,
            String afiliacionPolitica
    ) {}
}
