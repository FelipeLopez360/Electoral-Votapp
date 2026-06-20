package co.com.votapp.ws.electoral.domain.port.out;

import co.com.votapp.ws.electoral.domain.model.ResultAggregate;

import java.util.UUID;

/**
 * Output port for read-only election results aggregation.
 *
 * <p>Implemented by the persistence adapter using a single GROUP BY candidato_id query
 * over the {@code votos} table and separate participation/census counts.
 *
 * <p>Deliberately separate from VoteRepositoryPort to keep the voting context write-only
 * and this results aggregation as a pure read concern of the electoral context.
 *
 * <p>Pure Java — ZERO framework imports.
 */
public interface ResultsRepositoryPort {

    /**
     * Aggregate per-candidate vote counts and participation/census totals
     * for the given election.
     *
     * @param eleccionId the election's UUID
     * @return a {@link ResultAggregate} with all counts needed to build an {@code ElectionResult}
     */
    ResultAggregate aggregate(UUID eleccionId);
}
