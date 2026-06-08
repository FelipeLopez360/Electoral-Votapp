package co.com.votapp.ws.electoral.domain.usecase;

import co.com.votapp.ws.common.exception.DomainException;
import co.com.votapp.ws.electoral.application.command.CreateElectionCommand;
import co.com.votapp.ws.electoral.domain.Election;
import co.com.votapp.ws.electoral.domain.ElectionStatus;
import co.com.votapp.ws.electoral.domain.port.in.CreateElectionUseCase;
import co.com.votapp.ws.electoral.domain.port.out.ElectionRepositoryPort;

/**
 * Use case implementation: create a new election in PROGRAMADA state.
 *
 * <p>Enforces codigo uniqueness before persisting.
 * No Spring annotations — wired manually via DomainConfig.
 */
public class CreateElectionUseCaseImpl implements CreateElectionUseCase {

    private final ElectionRepositoryPort electionRepository;

    public CreateElectionUseCaseImpl(ElectionRepositoryPort electionRepository) {
        this.electionRepository = electionRepository;
    }

    @Override
    public Election create(CreateElectionCommand command) {
        electionRepository.findByCodigo(command.codigo()).ifPresent(existing -> {
            throw new DomainException("Election with codigo '" + command.codigo() + "' already exists");
        });

        Election election = new Election(
                null,
                command.codigo(),
                command.nombre(),
                ElectionStatus.PROGRAMADA,
                command.fechaInicio(),
                command.fechaFin()
        );

        return electionRepository.save(election);
    }
}
