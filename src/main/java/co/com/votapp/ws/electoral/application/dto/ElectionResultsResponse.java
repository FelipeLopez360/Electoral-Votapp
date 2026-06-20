package co.com.votapp.ws.electoral.application.dto;

import co.com.votapp.ws.electoral.domain.model.CandidateResult;
import co.com.votapp.ws.electoral.domain.model.ElectionResult;

import java.util.List;
import java.util.Objects;

/**
 * Chart-ready JSON projection of an {@link ElectionResult}.
 *
 * <p>Designed for frontend consumption: all UUIDs are serialized as strings,
 * computed fields (abstentions, participationRate) are pre-calculated and included,
 * so the frontend can render charts without additional math.
 *
 * <p>Spring {@code @Service} layer populates this from the domain model.
 * Lives in {@code application/dto/} — Spring annotations are permitted here.
 *
 * @param electionId         the election's UUID as a string
 * @param electionName       the election's display name
 * @param candidates         per-candidate results, ordered by votes descending
 * @param blankVotes         total votes for the blank-vote synthetic candidate
 * @param nullVotes          total votes for the null-vote synthetic candidate
 * @param totalVotes         total votes cast (all candidates)
 * @param eligibleCount      total eligible voters in the census
 * @param participationCount total voters who cast a vote
 * @param abstentions        eligible - participated (pre-computed for convenience)
 * @param participationRate  participation as a percentage (0.0–100.0)
 */
public record ElectionResultsResponse(
        String electionId,
        String electionName,
        List<CandidateResultEntry> candidates,
        long blankVotes,
        long nullVotes,
        long totalVotes,
        long eligibleCount,
        long participationCount,
        long abstentions,
        double participationRate
) {
    public ElectionResultsResponse {
        Objects.requireNonNull(electionId, "electionId must not be null");
        Objects.requireNonNull(electionName, "electionName must not be blank");
        candidates = candidates != null ? List.copyOf(candidates) : List.of();
    }

    /**
     * Factory: build a response from the domain {@link ElectionResult}.
     */
    public static ElectionResultsResponse from(ElectionResult result) {
        List<CandidateResultEntry> entries = result.candidateResults().stream()
                .map(CandidateResultEntry::from)
                .toList();

        return new ElectionResultsResponse(
                result.electionId().toString(),
                result.electionName(),
                entries,
                result.blankVotes(),
                result.nullVotes(),
                result.totalVotes(),
                result.eligibleCount(),
                result.participationCount(),
                result.abstentions(),
                result.participationRate()
        );
    }

    /**
     * Per-candidate result entry in the JSON response.
     *
     * @param candidateId    the candidate's UUID as a string
     * @param nombre         the candidate's display name
     * @param votes          votes received
     * @param percentage     percentage of total valid votes (server-computed)
     * @param esVotoEnBlanco true if this is the blank-vote synthetic candidate
     * @param esVotoNulo     true if this is the null-vote synthetic candidate
     * @param isWinner       true if this candidate has the highest valid vote count
     */
    public record CandidateResultEntry(
            String candidateId,
            String nombre,
            long votes,
            double percentage,
            boolean esVotoEnBlanco,
            boolean esVotoNulo,
            boolean isWinner
    ) {
        public static CandidateResultEntry from(CandidateResult cr) {
            return new CandidateResultEntry(
                    cr.candidateId().toString(),
                    cr.nombre(),
                    cr.votes(),
                    cr.percentage(),
                    cr.esVotoEnBlanco(),
                    cr.esVotoNulo(),
                    cr.isWinner()
            );
        }
    }
}
