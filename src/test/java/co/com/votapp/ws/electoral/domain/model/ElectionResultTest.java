package co.com.votapp.ws.electoral.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for ElectionResult and CandidateResult domain records.
 *
 * <p>Strict TDD — RED written before production code.
 * Tests verify: construction, null guards, and calculated field consistency.
 */
@DisplayName("ElectionResult - Domain model construction and invariants")
class ElectionResultTest {

    // ─── CandidateResult tests ────────────────────────────────────────────────

    @Test
    @DisplayName("Should create CandidateResult with all fields populated")
    void candidateResult_shouldCreateSuccessfully_whenAllFieldsProvided() {
        // Given
        UUID candidateId = UUID.randomUUID();

        // When
        var result = new CandidateResult(candidateId, "Candidato A", 42L, 60.0, false, false, true);

        // Then
        assertThat(result.candidateId()).isEqualTo(candidateId);
        assertThat(result.nombre()).isEqualTo("Candidato A");
        assertThat(result.votes()).isEqualTo(42L);
        assertThat(result.percentage()).isEqualTo(60.0);
        assertThat(result.esVotoEnBlanco()).isFalse();
        assertThat(result.esVotoNulo()).isFalse();
        assertThat(result.isWinner()).isTrue();
    }

    @Test
    @DisplayName("Should create CandidateResult for blank vote candidate")
    void candidateResult_shouldMarkBlankVote_whenEsVotoEnBlancoIsTrue() {
        // Given
        UUID candidateId = UUID.randomUUID();

        // When
        var blankVote = new CandidateResult(candidateId, "Voto en Blanco", 5L, 10.0, true, false, false);

        // Then
        assertThat(blankVote.esVotoEnBlanco()).isTrue();
        assertThat(blankVote.esVotoNulo()).isFalse();
        assertThat(blankVote.isWinner()).isFalse();
    }

    @Test
    @DisplayName("Should create CandidateResult for null vote candidate")
    void candidateResult_shouldMarkNullVote_whenEsVotoNuloIsTrue() {
        // Given
        UUID candidateId = UUID.randomUUID();

        // When
        var nullVote = new CandidateResult(candidateId, "Voto Nulo", 3L, 6.0, false, true, false);

        // Then
        assertThat(nullVote.esVotoEnBlanco()).isFalse();
        assertThat(nullVote.esVotoNulo()).isTrue();
        assertThat(nullVote.isWinner()).isFalse();
    }

    @Test
    @DisplayName("Should reject null candidateId on CandidateResult construction")
    void candidateResult_shouldThrow_whenCandidateIdIsNull() {
        assertThatThrownBy(() -> new CandidateResult(null, "Candidato A", 10L, 50.0, false, false, true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should reject null or blank nombre on CandidateResult construction")
    void candidateResult_shouldThrow_whenNombreIsBlank() {
        assertThatThrownBy(() -> new CandidateResult(UUID.randomUUID(), "  ", 10L, 50.0, false, false, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ─── ElectionResult tests ─────────────────────────────────────────────────

    @Test
    @DisplayName("Should create ElectionResult with correct aggregated totals")
    void electionResult_shouldCreateSuccessfully_withValidData() {
        // Given
        UUID electionId = UUID.randomUUID();
        var winner = new CandidateResult(UUID.randomUUID(), "Candidato A", 70L, 70.0, false, false, true);
        var loser = new CandidateResult(UUID.randomUUID(), "Candidato B", 20L, 20.0, false, false, false);
        var blank = new CandidateResult(UUID.randomUUID(), "Voto en Blanco", 5L, 5.0, true, false, false);
        var nullVote = new CandidateResult(UUID.randomUUID(), "Voto Nulo", 5L, 5.0, false, true, false);

        // When
        var electionResult = new ElectionResult(
                electionId, "Eleccion Prueba",
                List.of(winner, loser, blank, nullVote),
                5L, 5L, 100L, 80L, 20L
        );

        // Then
        assertThat(electionResult.electionId()).isEqualTo(electionId);
        assertThat(electionResult.electionName()).isEqualTo("Eleccion Prueba");
        assertThat(electionResult.candidateResults()).hasSize(4);
        assertThat(electionResult.blankVotes()).isEqualTo(5L);
        assertThat(electionResult.nullVotes()).isEqualTo(5L);
        assertThat(electionResult.totalVotes()).isEqualTo(100L);
        assertThat(electionResult.eligibleCount()).isEqualTo(80L);
        assertThat(electionResult.participationCount()).isEqualTo(20L);
    }

    @Test
    @DisplayName("Should reject null electionId on ElectionResult construction")
    void electionResult_shouldThrow_whenElectionIdIsNull() {
        assertThatThrownBy(() -> new ElectionResult(
                null, "Eleccion Prueba", List.of(), 0L, 0L, 0L, 100L, 0L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should reject null or blank electionName on ElectionResult construction")
    void electionResult_shouldThrow_whenElectionNameIsBlank() {
        assertThatThrownBy(() -> new ElectionResult(
                UUID.randomUUID(), "  ", List.of(), 0L, 0L, 0L, 100L, 0L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should return winners — candidates marked isWinner=true")
    void electionResult_shouldReturnWinners_fromCandidateResultList() {
        // Given
        UUID electionId = UUID.randomUUID();
        var winner = new CandidateResult(UUID.randomUUID(), "Candidato A", 70L, 70.0, false, false, true);
        var loser = new CandidateResult(UUID.randomUUID(), "Candidato B", 30L, 30.0, false, false, false);
        var result = new ElectionResult(electionId, "Eleccion", List.of(winner, loser), 0L, 0L, 100L, 50L, 10L);

        // When
        var winners = result.candidateResults().stream()
                .filter(CandidateResult::isWinner)
                .toList();

        // Then
        assertThat(winners).hasSize(1);
        assertThat(winners.getFirst().nombre()).isEqualTo("Candidato A");
    }
}
