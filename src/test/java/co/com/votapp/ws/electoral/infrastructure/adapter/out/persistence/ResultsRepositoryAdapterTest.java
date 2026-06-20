package co.com.votapp.ws.electoral.infrastructure.adapter.out.persistence;

import co.com.votapp.ws.electoral.domain.model.ResultAggregate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ResultsRepositoryAdapter.
 *
 * <p>Mocks the underlying JPA repositories to verify the adapter's mapping logic.
 * Integration-level SQL accuracy is covered in ResultsRepositoryAdapterIT (Slice 3).
 */
@DisplayName("ResultsRepositoryAdapter - Mapping and aggregation delegation")
@ExtendWith(MockitoExtension.class)
class ResultsRepositoryAdapterTest {

    @Mock
    private ResultsJpaRepository resultsJpaRepository;

    private ResultsRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ResultsRepositoryAdapter(resultsJpaRepository);
    }

    @Test
    @DisplayName("Should return empty aggregate when no votes exist for the election")
    void aggregate_shouldReturnEmptyAggregate_whenNoVotesExist() {
        // Given
        UUID electionId = UUID.randomUUID();
        List<Object[]> emptyRows = new ArrayList<>();
        doReturn(emptyRows).when(resultsJpaRepository).findVoteCountsByElection(eq(electionId));
        when(resultsJpaRepository.countParticipation(eq(electionId))).thenReturn(0L);
        when(resultsJpaRepository.countEligible(eq(electionId))).thenReturn(0L);

        // When
        ResultAggregate result = adapter.aggregate(electionId);

        // Then
        assertThat(result.candidateCounts()).isEmpty();
        assertThat(result.totalVotes()).isEqualTo(0L);
        assertThat(result.blankVotes()).isEqualTo(0L);
        assertThat(result.nullVotes()).isEqualTo(0L);
        assertThat(result.eligibleCount()).isEqualTo(0L);
        assertThat(result.participationCount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("Should map vote counts rows to CandidateCount records correctly")
    void aggregate_shouldMapRows_toCandidateCounts() {
        // Given
        UUID electionId  = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();

        // The JPA projection row: [candidateId, nombre, votes, esVotoEnBlanco, esVotoNulo]
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{candidateId, "Candidate A", 42L, false, false});
        doReturn(rows).when(resultsJpaRepository).findVoteCountsByElection(electionId);
        when(resultsJpaRepository.countParticipation(electionId)).thenReturn(42L);
        when(resultsJpaRepository.countEligible(electionId)).thenReturn(100L);

        // When
        ResultAggregate result = adapter.aggregate(electionId);

        // Then
        assertThat(result.candidateCounts()).hasSize(1);
        ResultAggregate.CandidateCount count = result.candidateCounts().getFirst();
        assertThat(count.candidateId()).isEqualTo(candidateId);
        assertThat(count.nombre()).isEqualTo("Candidate A");
        assertThat(count.votes()).isEqualTo(42L);
        assertThat(count.esVotoEnBlanco()).isFalse();
        assertThat(count.esVotoNulo()).isFalse();
    }

    @Test
    @DisplayName("Should correctly identify and sum blank-vote candidate")
    void aggregate_shouldSumBlankVotes_fromBlankVoteCandidate() {
        // Given
        UUID electionId = UUID.randomUUID();
        UUID blankId    = UUID.randomUUID();

        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{blankId, "Voto en Blanco", 15L, true, false});
        doReturn(rows).when(resultsJpaRepository).findVoteCountsByElection(electionId);
        when(resultsJpaRepository.countParticipation(electionId)).thenReturn(15L);
        when(resultsJpaRepository.countEligible(electionId)).thenReturn(100L);

        // When
        ResultAggregate result = adapter.aggregate(electionId);

        // Then
        assertThat(result.blankVotes()).isEqualTo(15L);
        assertThat(result.nullVotes()).isEqualTo(0L);
        assertThat(result.totalVotes()).isEqualTo(15L);
        assertThat(result.candidateCounts()).hasSize(1);
        assertThat(result.candidateCounts().getFirst().esVotoEnBlanco()).isTrue();
    }

    @Test
    @DisplayName("Should correctly identify and sum null-vote candidate")
    void aggregate_shouldSumNullVotes_fromNullVoteCandidate() {
        // Given
        UUID electionId = UUID.randomUUID();
        UUID nullId     = UUID.randomUUID();

        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{nullId, "Voto Nulo", 8L, false, true});
        doReturn(rows).when(resultsJpaRepository).findVoteCountsByElection(electionId);
        when(resultsJpaRepository.countParticipation(electionId)).thenReturn(8L);
        when(resultsJpaRepository.countEligible(electionId)).thenReturn(100L);

        // When
        ResultAggregate result = adapter.aggregate(electionId);

        // Then
        assertThat(result.nullVotes()).isEqualTo(8L);
        assertThat(result.blankVotes()).isEqualTo(0L);
        assertThat(result.totalVotes()).isEqualTo(8L);
    }

    @Test
    @DisplayName("Should compute totalVotes as sum of all candidate vote counts")
    void aggregate_shouldComputeTotalVotes_asSumOfAllCandidates() {
        // Given
        UUID electionId = UUID.randomUUID();
        UUID candA = UUID.randomUUID();
        UUID candB = UUID.randomUUID();
        UUID blank = UUID.randomUUID();

        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{candA, "A",              60L, false, false});
        rows.add(new Object[]{candB, "B",              30L, false, false});
        rows.add(new Object[]{blank, "Voto en Blanco", 10L, true,  false});
        doReturn(rows).when(resultsJpaRepository).findVoteCountsByElection(electionId);
        when(resultsJpaRepository.countParticipation(electionId)).thenReturn(100L);
        when(resultsJpaRepository.countEligible(electionId)).thenReturn(200L);

        // When
        ResultAggregate result = adapter.aggregate(electionId);

        // Then
        assertThat(result.totalVotes()).isEqualTo(100L);
        assertThat(result.blankVotes()).isEqualTo(10L);
        assertThat(result.participationCount()).isEqualTo(100L);
        assertThat(result.eligibleCount()).isEqualTo(200L);
    }
}
