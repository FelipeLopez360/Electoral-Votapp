package co.com.votapp.ws.electoral.domain.usecase;

import co.com.votapp.ws.common.exception.NotFoundException;
import co.com.votapp.ws.electoral.domain.Election;
import co.com.votapp.ws.electoral.domain.ElectionStatus;
import co.com.votapp.ws.electoral.domain.exception.ElectionNotFinalizedException;
import co.com.votapp.ws.electoral.domain.model.CandidateResult;
import co.com.votapp.ws.electoral.domain.model.ElectionResult;
import co.com.votapp.ws.electoral.domain.model.ResultAggregate;
import co.com.votapp.ws.electoral.domain.port.out.ElectionRepositoryPort;
import co.com.votapp.ws.electoral.domain.port.out.ResultsRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@DisplayName("GetElectionResultsUseCaseImpl - Domain aggregation logic")
@ExtendWith(MockitoExtension.class)
class GetElectionResultsUseCaseImplTest {

    @Mock
    private ElectionRepositoryPort electionRepository;

    @Mock
    private ResultsRepositoryPort resultsRepository;

    private GetElectionResultsUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetElectionResultsUseCaseImpl(electionRepository, resultsRepository);
    }

    // ─── Finalized guard ──────────────────────────────────────────────────────

    @Test
    @DisplayName("Should throw ElectionNotFinalizedException when election is PROGRAMADA")
    void getResults_shouldThrowElectionNotFinalized_whenElectionIsProgramada() {
        // Given
        UUID electionId = UUID.randomUUID();
        when(electionRepository.findById(electionId))
                .thenReturn(Optional.of(electionWith(electionId, ElectionStatus.PROGRAMADA)));

        // When & Then
        assertThatThrownBy(() -> useCase.getResults(electionId))
                .isInstanceOf(ElectionNotFinalizedException.class)
                .hasMessageContaining(electionId.toString());
    }

    @Test
    @DisplayName("Should throw ElectionNotFinalizedException when election is ACTIVA")
    void getResults_shouldThrowElectionNotFinalized_whenElectionIsActiva() {
        // Given
        UUID electionId = UUID.randomUUID();
        when(electionRepository.findById(electionId))
                .thenReturn(Optional.of(electionWith(electionId, ElectionStatus.ACTIVA)));

        // When & Then
        assertThatThrownBy(() -> useCase.getResults(electionId))
                .isInstanceOf(ElectionNotFinalizedException.class);
    }

    @Test
    @DisplayName("Should throw NotFoundException when election does not exist")
    void getResults_shouldThrowNotFoundException_whenElectionNotFound() {
        // Given
        UUID electionId = UUID.randomUUID();
        when(electionRepository.findById(electionId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> useCase.getResults(electionId))
                .isInstanceOf(NotFoundException.class);
    }

    // ─── Percentage and winner calculation ────────────────────────────────────

    @Test
    @DisplayName("Should compute correct percentages for each candidate")
    void getResults_shouldComputePercentages_whenElectionIsFinalized() {
        // Given
        UUID electionId = UUID.randomUUID();
        UUID candidateA = UUID.randomUUID();
        UUID candidateB = UUID.randomUUID();
        UUID blankId    = UUID.randomUUID();

        when(electionRepository.findById(electionId))
                .thenReturn(Optional.of(electionWith(electionId, ElectionStatus.FINALIZADA)));

        // 60 votes to A, 30 to B, 10 blank = 100 total
        ResultAggregate aggregate = new ResultAggregate(
                List.of(
                        new ResultAggregate.CandidateCount(candidateA, "Candidate A", 60, false, false),
                        new ResultAggregate.CandidateCount(candidateB, "Candidate B", 30, false, false),
                        new ResultAggregate.CandidateCount(blankId, "Voto en Blanco", 10, true, false)
                ),
                10L,  // blankVotes
                0L,   // nullVotes
                100L, // totalVotes
                200L, // eligibleCount
                100L  // participationCount
        );
        when(resultsRepository.aggregate(electionId)).thenReturn(aggregate);

        // When
        ElectionResult result = useCase.getResults(electionId);

        // Then
        assertThat(result.totalVotes()).isEqualTo(100L);
        assertThat(result.blankVotes()).isEqualTo(10L);
        assertThat(result.nullVotes()).isEqualTo(0L);

        CandidateResult candidateAResult = result.candidateResults().stream()
                .filter(cr -> cr.candidateId().equals(candidateA))
                .findFirst()
                .orElseThrow();
        assertThat(candidateAResult.percentage()).isEqualTo(60.0);
        assertThat(candidateAResult.votes()).isEqualTo(60L);

        CandidateResult candidateBResult = result.candidateResults().stream()
                .filter(cr -> cr.candidateId().equals(candidateB))
                .findFirst()
                .orElseThrow();
        assertThat(candidateBResult.percentage()).isEqualTo(30.0);
    }

    @Test
    @DisplayName("Should flag candidate with most votes as winner")
    void getResults_shouldFlagWinner_whenClearLeaderExists() {
        // Given
        UUID electionId = UUID.randomUUID();
        UUID candidateA = UUID.randomUUID();
        UUID candidateB = UUID.randomUUID();

        when(electionRepository.findById(electionId))
                .thenReturn(Optional.of(electionWith(electionId, ElectionStatus.FINALIZADA)));

        ResultAggregate aggregate = new ResultAggregate(
                List.of(
                        new ResultAggregate.CandidateCount(candidateA, "Winner A", 70, false, false),
                        new ResultAggregate.CandidateCount(candidateB, "Loser B",  30, false, false)
                ),
                0L, 0L, 100L, 100L, 100L
        );
        when(resultsRepository.aggregate(electionId)).thenReturn(aggregate);

        // When
        ElectionResult result = useCase.getResults(electionId);

        // Then
        assertThat(result.candidateResults())
                .anySatisfy(cr -> {
                    assertThat(cr.candidateId()).isEqualTo(candidateA);
                    assertThat(cr.isWinner()).isTrue();
                });
        assertThat(result.candidateResults())
                .anySatisfy(cr -> {
                    assertThat(cr.candidateId()).isEqualTo(candidateB);
                    assertThat(cr.isWinner()).isFalse();
                });
    }

    @Test
    @DisplayName("Should flag multiple winners on exact tie")
    void getResults_shouldFlagMultipleWinners_whenTieExists() {
        // Given
        UUID electionId = UUID.randomUUID();
        UUID candidateA = UUID.randomUUID();
        UUID candidateB = UUID.randomUUID();

        when(electionRepository.findById(electionId))
                .thenReturn(Optional.of(electionWith(electionId, ElectionStatus.FINALIZADA)));

        ResultAggregate aggregate = new ResultAggregate(
                List.of(
                        new ResultAggregate.CandidateCount(candidateA, "Tie A", 50, false, false),
                        new ResultAggregate.CandidateCount(candidateB, "Tie B", 50, false, false)
                ),
                0L, 0L, 100L, 100L, 100L
        );
        when(resultsRepository.aggregate(electionId)).thenReturn(aggregate);

        // When
        ElectionResult result = useCase.getResults(electionId);

        // Then — both are winners
        assertThat(result.candidateResults())
                .extracting(CandidateResult::isWinner)
                .containsOnly(true);
    }

    @Test
    @DisplayName("Should NOT flag blank/null synthetic candidates as winners")
    void getResults_shouldNotFlagSyntheticCandidatesAsWinners_whenTheyHaveMostVotes() {
        // Given
        UUID electionId = UUID.randomUUID();
        UUID realCandidate = UUID.randomUUID();
        UUID blankId      = UUID.randomUUID();
        UUID nullId       = UUID.randomUUID();

        when(electionRepository.findById(electionId))
                .thenReturn(Optional.of(electionWith(electionId, ElectionStatus.FINALIZADA)));

        // Blank + null have more votes, but real candidate should win
        ResultAggregate aggregate = new ResultAggregate(
                List.of(
                        new ResultAggregate.CandidateCount(realCandidate, "Real A",       10, false, false),
                        new ResultAggregate.CandidateCount(blankId, "Voto en Blanco", 40, true,  false),
                        new ResultAggregate.CandidateCount(nullId,  "Voto Nulo",      50, false, true)
                ),
                40L, 50L, 100L, 200L, 100L
        );
        when(resultsRepository.aggregate(electionId)).thenReturn(aggregate);

        // When
        ElectionResult result = useCase.getResults(electionId);

        // Then — real candidate is winner even with fewer votes; blank/null never win
        CandidateResult realResult = result.candidateResults().stream()
                .filter(cr -> cr.candidateId().equals(realCandidate))
                .findFirst().orElseThrow();
        assertThat(realResult.isWinner()).isTrue();

        result.candidateResults().stream()
                .filter(cr -> cr.esVotoEnBlanco() || cr.esVotoNulo())
                .forEach(cr -> assertThat(cr.isWinner()).isFalse());
    }

    @Test
    @DisplayName("Should compute abstentions as eligibleCount minus participationCount")
    void getResults_shouldComputeAbstentions_fromEligibleMinusParticipation() {
        // Given
        UUID electionId = UUID.randomUUID();
        UUID candidateA = UUID.randomUUID();

        when(electionRepository.findById(electionId))
                .thenReturn(Optional.of(electionWith(electionId, ElectionStatus.FINALIZADA)));

        ResultAggregate aggregate = new ResultAggregate(
                List.of(new ResultAggregate.CandidateCount(candidateA, "A", 50, false, false)),
                0L, 0L, 50L, 200L, 150L
        );
        when(resultsRepository.aggregate(electionId)).thenReturn(aggregate);

        // When
        ElectionResult result = useCase.getResults(electionId);

        // Then
        assertThat(result.abstentions()).isEqualTo(50L); // 200 eligible - 150 participated
        assertThat(result.participationRate()).isEqualTo(75.0); // 150/200 * 100
    }

    @Test
    @DisplayName("Should return zero participation rate when there are no eligible voters")
    void getResults_shouldReturnZeroParticipationRate_whenNoEligibleVoters() {
        // Given
        UUID electionId = UUID.randomUUID();
        UUID candidateA = UUID.randomUUID();

        when(electionRepository.findById(electionId))
                .thenReturn(Optional.of(electionWith(electionId, ElectionStatus.FINALIZADA)));

        ResultAggregate aggregate = new ResultAggregate(
                List.of(new ResultAggregate.CandidateCount(candidateA, "A", 0, false, false)),
                0L, 0L, 0L, 0L, 0L
        );
        when(resultsRepository.aggregate(electionId)).thenReturn(aggregate);

        // When
        ElectionResult result = useCase.getResults(electionId);

        // Then — no division by zero
        assertThat(result.participationRate()).isEqualTo(0.0);
        assertThat(result.abstentions()).isEqualTo(0L);
    }

    @Test
    @DisplayName("Should return correct election name in result")
    void getResults_shouldReturnElectionName_fromElection() {
        // Given
        UUID electionId = UUID.randomUUID();
        String electionName = "Eleccion de Prueba 2026";

        Election election = new Election(
                electionId, "ELEC-001", electionName, ElectionStatus.FINALIZADA,
                LocalDateTime.now().minusDays(30), LocalDateTime.now().minusDays(1)
        );
        when(electionRepository.findById(electionId)).thenReturn(Optional.of(election));

        ResultAggregate aggregate = new ResultAggregate(
                List.of(), 0L, 0L, 0L, 100L, 0L
        );
        when(resultsRepository.aggregate(electionId)).thenReturn(aggregate);

        // When
        ElectionResult result = useCase.getResults(electionId);

        // Then
        assertThat(result.electionId()).isEqualTo(electionId);
        assertThat(result.electionName()).isEqualTo(electionName);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Election electionWith(UUID id, ElectionStatus status) {
        return new Election(
                id,
                "ELEC-" + id.toString().substring(0, 8),
                "Eleccion Test",
                status,
                LocalDateTime.now().minusDays(30),
                LocalDateTime.now().minusDays(1)
        );
    }
}
