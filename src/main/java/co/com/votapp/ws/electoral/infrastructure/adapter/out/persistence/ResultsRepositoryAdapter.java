package co.com.votapp.ws.electoral.infrastructure.adapter.out.persistence;

import co.com.votapp.ws.electoral.domain.model.ResultAggregate;
import co.com.votapp.ws.electoral.domain.port.out.ResultsRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Persistence adapter implementing {@link ResultsRepositoryPort} for read-only results aggregation.
 *
 * <p>Executes a single {@code GROUP BY candidato_id} native query via {@link ResultsJpaRepository}
 * and maps the raw result rows to the domain {@link ResultAggregate} record.
 *
 * <p>Separate participation and census count queries supply the aggregate totals.
 * All queries are read-only — this adapter never mutates the database.
 */
@Component
public class ResultsRepositoryAdapter implements ResultsRepositoryPort {

    private final ResultsJpaRepository resultsJpaRepository;

    public ResultsRepositoryAdapter(ResultsJpaRepository resultsJpaRepository) {
        this.resultsJpaRepository = resultsJpaRepository;
    }

    @Override
    public ResultAggregate aggregate(UUID eleccionId) {
        List<Object[]> rows = resultsJpaRepository.findVoteCountsByElection(eleccionId);
        long participationCount = resultsJpaRepository.countParticipation(eleccionId);
        long eligibleCount = resultsJpaRepository.countEligible(eleccionId);

        List<ResultAggregate.CandidateCount> candidateCounts = rows.stream()
                .map(this::toCount)
                .toList();

        long totalVotes = candidateCounts.stream()
                .mapToLong(ResultAggregate.CandidateCount::votes)
                .sum();

        long blankVotes = candidateCounts.stream()
                .filter(ResultAggregate.CandidateCount::esVotoEnBlanco)
                .mapToLong(ResultAggregate.CandidateCount::votes)
                .sum();

        long nullVotes = candidateCounts.stream()
                .filter(ResultAggregate.CandidateCount::esVotoNulo)
                .mapToLong(ResultAggregate.CandidateCount::votes)
                .sum();

        return new ResultAggregate(
                candidateCounts,
                blankVotes,
                nullVotes,
                totalVotes,
                eligibleCount,
                participationCount
        );
    }

    // ─── Mapping ─────────────────────────────────────────────────────────────

    /**
     * Map a raw native-query result row to a {@link ResultAggregate.CandidateCount} record.
     *
     * <p>Row layout (0-indexed):
     * <ul>
     *   <li>0 — candidateId (UUID)</li>
     *   <li>1 — nombre (String)</li>
     *   <li>2 — votes (Long / BigInteger from JDBC — cast to Long)</li>
     *   <li>3 — esVotoEnBlanco (Boolean)</li>
     *   <li>4 — esVotoNulo (Boolean)</li>
     * </ul>
     */
    private ResultAggregate.CandidateCount toCount(Object[] row) {
        UUID candidateId = (UUID) row[0];
        String nombre = (String) row[1];
        long votes = toLong(row[2]);
        boolean esVotoEnBlanco = toBoolean(row[3]);
        boolean esVotoNulo = toBoolean(row[4]);

        return new ResultAggregate.CandidateCount(
                candidateId, nombre, votes, esVotoEnBlanco, esVotoNulo
        );
    }

    /** JDBC may return BigInteger or Long depending on driver; normalise to long. */
    private long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Long l) return l;
        if (value instanceof Number n) return n.longValue();
        return 0L;
    }

    /** JDBC may return Boolean directly or as a numeric 0/1. */
    private boolean toBoolean(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean b) return b;
        if (value instanceof Number n) return n.intValue() != 0;
        return false;
    }
}
