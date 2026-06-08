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
 */
public interface CandidateRepositoryPort {

    /**
     * Persist a new Candidate.
     *
     * @return the saved candidate (with generated id if applicable)
     */
    Candidate save(Candidate candidate);

    /**
     * Find all candidates for an election, ordered by numero_orden ascending.
     */
    List<Candidate> findByEleccionIdOrderByNumeroOrden(UUID eleccionId);

    /**
     * Check if a candidate with a given numero_orden already exists for the election.
     * Used to enforce uniqueness of numero_orden per election.
     */
    boolean existsByEleccionIdAndNumeroOrden(UUID eleccionId, int numeroOrden);

    /**
     * Find a candidate by its id within a specific election.
     */
    Optional<Candidate> findByIdAndEleccionId(UUID candidateId, UUID eleccionId);

    /**
     * Delete a candidate by id.
     */
    void deleteById(UUID candidateId);
}
