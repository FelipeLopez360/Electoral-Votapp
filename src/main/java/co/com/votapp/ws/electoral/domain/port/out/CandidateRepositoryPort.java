package co.com.votapp.ws.electoral.domain.port.out;

import co.com.votapp.ws.electoral.domain.Candidate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port for Candidate persistence within the electoral bounded context.
 *
 * <p>Distinct from CandidatoRepositoryPort in the candidates context,
 * which manages the legacy Candidato domain object.
 *
 * <p>V5 changes:
 * - {@code existsByEleccionIdAndNumeroOrden} → {@code existsByEleccionIdAndFuncionarioId}
 * - {@code findByEleccionIdOrderByNumeroOrden} → {@code findByEleccionIdOrderByNombre}
 */
public interface CandidateRepositoryPort {

    /**
     * Persist a new Candidate.
     *
     * @return the saved candidate (with generated id if applicable)
     */
    Candidate save(Candidate candidate);

    /**
     * Find all candidates for an election, ordered alphabetically by nombre.
     * Synthetic candidates (blank vote, null vote) always appear last.
     */
    List<Candidate> findByEleccionIdOrderByNombre(UUID eleccionId);

    /**
     * Check if a candidate linked to a given funcionarioId already exists for the election.
     * Used to enforce uniqueness of funcionarioId per election (409 Conflict).
     */
    boolean existsByEleccionIdAndFuncionarioId(UUID eleccionId, Integer funcionarioId);

    /**
     * Find a candidate by its id within a specific election.
     */
    Optional<Candidate> findByIdAndEleccionId(UUID candidateId, UUID eleccionId);

    /**
     * Delete a candidate by id.
     */
    void deleteById(UUID candidateId);
}
