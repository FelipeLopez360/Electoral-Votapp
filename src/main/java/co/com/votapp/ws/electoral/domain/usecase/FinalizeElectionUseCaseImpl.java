package co.com.votapp.ws.electoral.domain.usecase;

import co.com.votapp.ws.common.exception.DomainException;
import co.com.votapp.ws.electoral.domain.Election;
import co.com.votapp.ws.electoral.domain.ElectionStatus;
import co.com.votapp.ws.electoral.domain.port.in.FinalizeElectionUseCase;
import co.com.votapp.ws.electoral.domain.port.out.ElectionRepositoryPort;

import java.util.UUID;

/**
 * Use case implementation: transition election from ACTIVA to FINALIZADA.
 *
 * <p>Only elections in ACTIVA status can be finalized.
 * CANCELADA and already FINALIZADA elections are rejected.
 * No Spring annotations — wired manually via DomainConfig.
 */
public class FinalizeElectionUseCaseImpl implements FinalizeElectionUseCase {

    private final ElectionRepositoryPort electionRepository;

    public FinalizeElectionUseCaseImpl(ElectionRepositoryPort electionRepository) {
        this.electionRepository = electionRepository;
    }

    @Override
    public void finalize(UUID electionId) {
        Election election = electionRepository.findById(electionId)
                .orElseThrow(() -> new DomainException("Election not found: " + electionId));

        if (election.status() != ElectionStatus.ACTIVA) {
            throw new DomainException(
                    "Only ACTIVA elections can be finalized. Current status: " + election.status());
        }

        Election finalized = new Election(
                election.id(),
                election.codigo(),
                election.nombre(),
                ElectionStatus.FINALIZADA,
                election.fechaInicio(),
                election.fechaFin()
        );

        electionRepository.save(finalized);
    }
}
