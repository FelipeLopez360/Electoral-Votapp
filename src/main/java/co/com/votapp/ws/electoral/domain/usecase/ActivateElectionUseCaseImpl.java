package co.com.votapp.ws.electoral.domain.usecase;

import co.com.votapp.ws.common.exception.DomainException;
import co.com.votapp.ws.electoral.domain.Candidate;
import co.com.votapp.ws.electoral.domain.Election;
import co.com.votapp.ws.electoral.domain.ElectionStatus;
import co.com.votapp.ws.electoral.domain.port.in.ActivateElectionUseCase;
import co.com.votapp.ws.electoral.domain.port.out.CandidateRepositoryPort;
import co.com.votapp.ws.electoral.domain.port.out.ElectionRepositoryPort;

import java.util.UUID;

/**
 * Use case implementation: transition election from PROGRAMADA to ACTIVA.
 *
 * <p>Side effect: auto-creates the synthetic "Voto en Blanco" candidate
 * with numero_orden=0 (always first in the ballot).
 * No Spring annotations — wired manually via DomainConfig.
 */
public class ActivateElectionUseCaseImpl implements ActivateElectionUseCase {

    private final ElectionRepositoryPort electionRepository;
    private final CandidateRepositoryPort candidateRepository;

    public ActivateElectionUseCaseImpl(ElectionRepositoryPort electionRepository,
                                       CandidateRepositoryPort candidateRepository) {
        this.electionRepository = electionRepository;
        this.candidateRepository = candidateRepository;
    }

    @Override
    public void activate(UUID electionId) {
        Election election = electionRepository.findById(electionId)
                .orElseThrow(() -> new DomainException("Election not found: " + electionId));

        if (election.status() != ElectionStatus.PROGRAMADA) {
            throw new DomainException(
                    "Only PROGRAMADA elections can be activated. Current status: " + election.status());
        }

        // Transition to ACTIVA — records are immutable, create a new instance
        Election activated = new Election(
                election.id(),
                election.codigo(),
                election.nombre(),
                ElectionStatus.ACTIVA,
                election.fechaInicio(),
                election.fechaFin()
        );

        electionRepository.save(activated);

        // Auto-create the synthetic blank vote candidate (numero_orden=0)
        Candidate blankVote = new Candidate(
                UUID.randomUUID(),
                electionId,
                "Voto en Blanco",
                true,
                0
        );

        candidateRepository.save(blankVote);
    }
}
