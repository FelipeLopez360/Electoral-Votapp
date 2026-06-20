package co.com.votapp.ws.electoral.domain.model;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Read-only aggregate value object representing the full results of a finalized election.
 *
 * <p>Returned by {@code GetElectionResultsUseCase}.
 * Consumed by the REST adapter for JSON serialization and by the report exporters for file generation.
 *
 * <p>Pure Java — ZERO framework imports.
 *
 * @param electionId         the election's UUID
 * @param electionName       the election's display name
 * @param candidateResults   per-candidate results (includes blank and null synthetic candidates)
 * @param blankVotes         total votes cast for the blank-vote synthetic candidate
 * @param nullVotes          total votes cast for the null-vote synthetic candidate
 * @param totalVotes         total valid votes cast (all candidates including blank and null)
 * @param eligibleCount      total eligible voters in the census
 * @param participationCount total voters who actually cast a vote
 */
public record ElectionResult(
        UUID electionId,
        String electionName,
        List<CandidateResult> candidateResults,
        long blankVotes,
        long nullVotes,
        long totalVotes,
        long eligibleCount,
        long participationCount
) {
    public ElectionResult {
        if (electionId == null) {
            throw new IllegalArgumentException("electionId must not be null");
        }
        if (electionName == null || electionName.isBlank()) {
            throw new IllegalArgumentException("electionName must not be blank");
        }
        if (candidateResults == null) {
            throw new IllegalArgumentException("candidateResults must not be null");
        }
        // Defensive copy to maintain immutability
        candidateResults = List.copyOf(candidateResults);
    }

    /**
     * Computes the abstention count as the difference between eligible voters and participants.
     */
    public long abstentions() {
        return eligibleCount - participationCount;
    }

    /**
     * Computes the participation rate as a percentage (0.0 to 100.0).
     * Returns 0.0 when there are no eligible voters to avoid division by zero.
     */
    public double participationRate() {
        if (eligibleCount == 0) {
            return 0.0;
        }
        return (participationCount * 100.0) / eligibleCount;
    }
}
