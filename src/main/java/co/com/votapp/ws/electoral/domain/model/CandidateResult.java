package co.com.votapp.ws.electoral.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Read-only value object representing a single candidate's aggregated results.
 *
 * <p>Produced by {@code GetElectionResultsUseCase} after live aggregation.
 * Used in both JSON API response and report generation.
 *
 * <p>Pure Java — ZERO framework imports.
 *
 * @param candidateId    the candidate's UUID
 * @param nombre         the candidate's display name
 * @param votes          total votes received by this candidate
 * @param percentage     percentage of total valid votes (server-computed)
 * @param esVotoEnBlanco true if this is the synthetic blank-vote candidate
 * @param esVotoNulo     true if this is the synthetic null-vote candidate
 * @param isWinner       true if this candidate has the highest valid vote count
 *                       (multiple may be true on exact tie)
 */
public record CandidateResult(
        UUID candidateId,
        String nombre,
        long votes,
        double percentage,
        boolean esVotoEnBlanco,
        boolean esVotoNulo,
        boolean isWinner
) {
    public CandidateResult {
        if (candidateId == null) {
            throw new IllegalArgumentException("candidateId must not be null");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("nombre must not be blank");
        }
    }
}
