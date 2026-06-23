package co.com.votapp.ws.electoral.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link Candidate} rich profile fields.
 *
 * <p>V5: Updated — {@code afiliacionPolitica} removed; {@code funcionarioId} added.
 * {@code fotoUrl}, {@code biografia}, {@code propuestas} remain.
 */
@DisplayName("Candidate - Rich profile fields")
class CandidateRichProfileTest {

    private static final UUID ELECTION_ID = UUID.randomUUID();

    @Test
    @DisplayName("Should create candidate with all rich profile fields populated")
    void candidate_shouldCreateWithAllRichProfileFields() {
        // Given / When
        Candidate candidate = new Candidate(
                UUID.randomUUID(), ELECTION_ID,
                "Juan Pérez", false, false,
                5, // funcionarioId
                "https://example.com/photo.jpg",
                "Experienced leader with 10 years of public service",
                "Improve education and healthcare access"
        );

        // Then
        assertThat(candidate.funcionarioId()).isEqualTo(5);
        assertThat(candidate.fotoUrl()).isEqualTo("https://example.com/photo.jpg");
        assertThat(candidate.biografia()).isEqualTo("Experienced leader with 10 years of public service");
        assertThat(candidate.propuestas()).isEqualTo("Improve education and healthcare access");
    }

    @Test
    @DisplayName("Should create candidate with null rich profile fields (backward-compatible)")
    void candidate_shouldCreateWithNullRichProfileFields() {
        // Given / When
        Candidate candidate = new Candidate(
                UUID.randomUUID(), ELECTION_ID,
                "María García", false, false,
                2, // funcionarioId
                null, null, null
        );

        // Then
        assertThat(candidate.fotoUrl()).isNull();
        assertThat(candidate.biografia()).isNull();
        assertThat(candidate.propuestas()).isNull();
    }

    @Test
    @DisplayName("Should create synthetic blank vote candidate with null rich profile fields")
    void candidate_shouldCreateBlankVoteWithNullRichProfileFields() {
        // Given / When
        Candidate blankVote = new Candidate(
                UUID.randomUUID(), ELECTION_ID,
                "Voto en Blanco", true, false,
                null, null, null, null
        );

        // Then
        assertThat(blankVote.esVotoEnBlanco()).isTrue();
        assertThat(blankVote.funcionarioId()).isNull();
        assertThat(blankVote.fotoUrl()).isNull();
        assertThat(blankVote.biografia()).isNull();
    }

    @Test
    @DisplayName("Should create synthetic null vote candidate with null rich profile fields")
    void candidate_shouldCreateNullVoteWithNullRichProfileFields() {
        // Given / When
        Candidate nullVote = new Candidate(
                UUID.randomUUID(), ELECTION_ID,
                "Voto Nulo", false, true,
                null, null, null, null
        );

        // Then
        assertThat(nullVote.esVotoNulo()).isTrue();
        assertThat(nullVote.funcionarioId()).isNull();
        assertThat(nullVote.fotoUrl()).isNull();
    }
}
