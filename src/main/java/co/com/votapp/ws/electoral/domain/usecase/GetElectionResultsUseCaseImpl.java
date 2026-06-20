package co.com.votapp.ws.electoral.domain.usecase;

import co.com.votapp.ws.common.exception.NotFoundException;
import co.com.votapp.ws.electoral.domain.ElectionStatus;
import co.com.votapp.ws.electoral.domain.exception.ElectionNotFinalizedException;
import co.com.votapp.ws.electoral.domain.model.CandidateResult;
import co.com.votapp.ws.electoral.domain.model.ElectionResult;
import co.com.votapp.ws.electoral.domain.model.ResultAggregate;
import co.com.votapp.ws.electoral.domain.port.in.GetElectionResultsUseCase;
import co.com.votapp.ws.electoral.domain.port.out.ElectionRepositoryPort;
import co.com.votapp.ws.electoral.domain.port.out.ResultsRepositoryPort;

import java.util.List;
import java.util.UUID;

/**
 * Use case implementation: aggregate and return results for a finalized election.
 *
 * <p>Guards against non-FINALIZADA elections, delegates aggregation to {@link ResultsRepositoryPort},
 * and computes percentages and winner flags from the raw counts.
 *
 * <p>Winner determination: the candidate(s) with the highest valid vote count among
 * NON-synthetic candidates (blank and null are excluded from winner calculation).
 * On exact tie, all tied candidates are marked as winners.
 *
 * <p>Pure Java — ZERO Spring or framework imports. Wired manually in DomainConfig.
 */
public class GetElectionResultsUseCaseImpl implements GetElectionResultsUseCase {

    private final ElectionRepositoryPort electionRepository;
    private final ResultsRepositoryPort resultsRepository;

    public GetElectionResultsUseCaseImpl(ElectionRepositoryPort electionRepository,
                                         ResultsRepositoryPort resultsRepository) {
        this.electionRepository = electionRepository;
        this.resultsRepository = resultsRepository;
    }

    @Override
    public ElectionResult getResults(UUID eleccionId) {
        var election = electionRepository.findById(eleccionId)
                .orElseThrow(() -> new NotFoundException("Election not found: " + eleccionId));

        if (election.status() != ElectionStatus.FINALIZADA) {
            throw new ElectionNotFinalizedException(eleccionId);
        }

        ResultAggregate aggregate = resultsRepository.aggregate(eleccionId);

        long totalVotes = aggregate.totalVotes();

        // Determine maximum votes among real (non-synthetic) candidates only
        long maxRealVotes = aggregate.candidateCounts().stream()
                .filter(cc -> !cc.esVotoEnBlanco() && !cc.esVotoNulo())
                .mapToLong(ResultAggregate.CandidateCount::votes)
                .max()
                .orElse(0L);

        List<CandidateResult> candidateResults = aggregate.candidateCounts().stream()
                .map(cc -> {
                    double percentage = totalVotes > 0
                            ? (cc.votes() * 100.0) / totalVotes
                            : 0.0;

                    // Only real candidates may be winners
                    boolean isWinner = !cc.esVotoEnBlanco()
                            && !cc.esVotoNulo()
                            && cc.votes() == maxRealVotes
                            && maxRealVotes > 0;

                    return new CandidateResult(
                            cc.candidateId(),
                            cc.nombre(),
                            cc.votes(),
                            percentage,
                            cc.esVotoEnBlanco(),
                            cc.esVotoNulo(),
                            isWinner
                    );
                })
                .toList();

        return new ElectionResult(
                election.id(),
                election.nombre(),
                candidateResults,
                aggregate.blankVotes(),
                aggregate.nullVotes(),
                totalVotes,
                aggregate.eligibleCount(),
                aggregate.participationCount()
        );
    }
}
