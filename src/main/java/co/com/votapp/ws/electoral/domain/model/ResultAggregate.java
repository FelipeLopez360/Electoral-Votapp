package co.com.votapp.ws.electoral.domain.model;

import java.util.List;
import java.util.UUID;

/**
 * Raw aggregation result returned by {@code ResultsRepositoryPort}.
 *
 * <p>Contains per-candidate vote counts and participation/census totals
 * from a single grouped SQL query. The use case maps this into the richer
 * {@link ElectionResult} domain record with computed percentages and winner flags.
 *
 * <p>Pure Java — ZERO framework imports.
 *
 * @param candidateCounts    per-candidate vote counts (one entry per candidato)
 * @param blankVotes         count of votes for the blank-vote synthetic candidate
 * @param nullVotes          count of votes for the null-vote synthetic candidate
 * @param totalVotes         total votes cast across all candidates
 * @param eligibleCount      total eligible voters in the census
 * @param participationCount total voters who cast a vote
 */
public record ResultAggregate(
        List<CandidateCount> candidateCounts,
        long blankVotes,
        long nullVotes,
        long totalVotes,
        long eligibleCount,
        long participationCount
) {
    /**
     * A single candidate's raw vote count from the aggregation query.
     *
     * @param candidateId    the candidate UUID
     * @param nombre         the candidate's display name
     * @param votes          votes received
     * @param esVotoEnBlanco true if this is the blank-vote synthetic candidate
     * @param esVotoNulo     true if this is the null-vote synthetic candidate
     */
    public record CandidateCount(
            UUID candidateId,
            String nombre,
            long votes,
            boolean esVotoEnBlanco,
            boolean esVotoNulo
    ) {}
}
