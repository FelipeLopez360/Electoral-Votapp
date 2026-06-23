package co.com.votapp.ws.electoral.domain.usecase;

import co.com.votapp.ws.common.exception.DomainException;
import co.com.votapp.ws.electoral.application.command.AddCandidateCommand;
import co.com.votapp.ws.electoral.domain.Candidate;
import co.com.votapp.ws.electoral.domain.Election;
import co.com.votapp.ws.electoral.domain.ElectionStatus;
import co.com.votapp.ws.electoral.domain.port.in.AddCandidateUseCase;
import co.com.votapp.ws.electoral.domain.port.out.CandidateRepositoryPort;
import co.com.votapp.ws.electoral.domain.port.out.ElectionRepositoryPort;

import java.util.UUID;

/**
 * Use case implementation: add a candidate to a valid (non-closed) election.
 *
 * <p>Validates that the election exists and is not FINALIZADA or CANCELADA.
 * V5: Enforces unique funcionarioId per election (returns 409 via DomainException).
 * If funcionarioId is null (no funcionario link), uniqueness check is skipped.
 * No Spring annotations — wired manually via DomainConfig.
 */
public class AddCandidateUseCaseImpl implements AddCandidateUseCase {

    private final ElectionRepositoryPort electionRepository;
    private final CandidateRepositoryPort candidateRepository;

    public AddCandidateUseCaseImpl(ElectionRepositoryPort electionRepository,
                                   CandidateRepositoryPort candidateRepository) {
        this.electionRepository = electionRepository;
        this.candidateRepository = candidateRepository;
    }

    @Override
    public Candidate addCandidate(AddCandidateCommand command) {
        Election election = electionRepository.findById(command.eleccionId())
                .orElseThrow(() -> new DomainException(
                        "Election not found: " + command.eleccionId()));

        if (election.status() == ElectionStatus.FINALIZADA) {
            throw new DomainException("Cannot add candidates to a FINALIZADA election");
        }
        if (election.status() == ElectionStatus.CANCELADA) {
            throw new DomainException("Cannot add candidates to a CANCELADA election");
        }

        // Uniqueness check: if funcionarioId provided, ensure no duplicate in this election
        if (command.funcionarioId() != null &&
                candidateRepository.existsByEleccionIdAndFuncionarioId(
                        command.eleccionId(), command.funcionarioId())) {
            throw new DomainException(
                    "A candidate linked to funcionario=" + command.funcionarioId() +
                    " already exists for election " + command.eleccionId());
        }

        Candidate candidate = new Candidate(
                UUID.randomUUID(),
                command.eleccionId(),
                command.nombre(),
                false,
                false,
                command.funcionarioId(),
                command.fotoUrl(),
                command.biografia(),
                command.propuestas()
        );

        return candidateRepository.save(candidate);
    }
}
