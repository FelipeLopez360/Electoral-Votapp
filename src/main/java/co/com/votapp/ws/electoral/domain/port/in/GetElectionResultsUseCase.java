package co.com.votapp.ws.electoral.domain.port.in;

import co.com.votapp.ws.electoral.domain.exception.ElectionNotFinalizedException;
import co.com.votapp.ws.electoral.domain.model.ElectionResult;

import java.util.UUID;

/**
 * Input port: retrieve aggregated results for a finalized election.
 *
 * <p>The system MUST ONLY provide results for elections in the {@code FINALIZADA} state.
 * Requesting results for any other state throws {@link ElectionNotFinalizedException}.
 *
 * <p>Per design: live aggregation (no snapshot). Cost: O(votes) single scan using
 * {@code idx_votos_candidato}. Acceptable for low admin read volume on finalized elections.
 *
 * <p>Pure Java — ZERO framework imports.
 */
public interface GetElectionResultsUseCase {

    /**
     * Retrieve the aggregated results for the given election.
     *
     * @param eleccionId the election's UUID
     * @return a fully-computed {@link ElectionResult} with percentages, winner flags,
     *         participation rate, and abstentions
     * @throws co.com.votapp.ws.common.exception.NotFoundException if the election does not exist
     * @throws ElectionNotFinalizedException                        if the election is not FINALIZADA
     */
    ElectionResult getResults(UUID eleccionId);
}
