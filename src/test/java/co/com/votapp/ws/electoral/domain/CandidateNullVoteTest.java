package co.com.votapp.ws.electoral.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for the esVotoNulo field added to the Candidate domain record.
 *
 * <p>Strict TDD — RED written before adding esVotoNulo to Candidate record.
 */
@DisplayName("Candidate - Null vote domain model extension")
class CandidateNullVoteTest {

    @Test
    @DisplayName("Should create null-vote candidate with esVotoNulo=true and name 'Voto Nulo'")
    void candidate_shouldCreateNullVoteCandidate_whenEsVotoNuloIsTrue() {
        // Given
        UUID id = UUID.randomUUID();
        UUID eleccionId = UUID.randomUUID();

        // When
        var nullVoteCandidate = new Candidate(id, eleccionId, "Voto Nulo", false, true, 1, null, null, null, null);

        // Then
        assertThat(nullVoteCandidate.esVotoNulo()).isTrue();
        assertThat(nullVoteCandidate.esVotoEnBlanco()).isFalse();
        assertThat(nullVoteCandidate.nombre()).isEqualTo("Voto Nulo");
    }

    @Test
    @DisplayName("Should reject null-vote candidate that is also blank-vote")
    void candidate_shouldThrow_whenBothBlankAndNullAreTrue() {
        // Given
        UUID id = UUID.randomUUID();
        UUID eleccionId = UUID.randomUUID();

        // When & Then
        assertThatThrownBy(() -> new Candidate(id, eleccionId, "Voto Nulo", true, true, 1, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should reject null-vote candidate not named 'Voto Nulo'")
    void candidate_shouldThrow_whenNullVoteHasWrongName() {
        // Given
        UUID id = UUID.randomUUID();
        UUID eleccionId = UUID.randomUUID();

        // When & Then
        assertThatThrownBy(() -> new Candidate(id, eleccionId, "Some Other Name", false, true, 1, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should create regular candidate with esVotoNulo=false (backward compatibility)")
    void candidate_shouldCreateNormalCandidate_withEsVotoNuloFalse() {
        // Given
        UUID id = UUID.randomUUID();
        UUID eleccionId = UUID.randomUUID();

        // When
        var regular = new Candidate(id, eleccionId, "Candidato A", false, false, 1, null, null, null, null);

        // Then
        assertThat(regular.esVotoNulo()).isFalse();
        assertThat(regular.esVotoEnBlanco()).isFalse();
    }
}
