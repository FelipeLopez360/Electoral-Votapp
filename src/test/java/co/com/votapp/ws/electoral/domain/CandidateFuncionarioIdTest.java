package co.com.votapp.ws.electoral.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link Candidate} record after V5 migration:
 * - {@code numeroOrden} and {@code afiliacionPolitica} fields removed
 * - {@code funcionarioId} (Integer, nullable) added
 */
@DisplayName("Candidate - funcionarioId field and removed fields")
class CandidateFuncionarioIdTest {

    private static final UUID ELECTION_ID = UUID.randomUUID();

    @Test
    @DisplayName("Should create candidate with non-null funcionarioId")
    void candidate_shouldCreateWithFuncionarioId() {
        // Given / When
        Candidate candidate = new Candidate(
                UUID.randomUUID(), ELECTION_ID,
                "Juan Pérez", false, false,
                42, // funcionarioId
                "https://example.com/photo.jpg",
                "Experienced leader",
                "Improve services"
        );

        // Then
        assertThat(candidate.funcionarioId()).isEqualTo(42);
        assertThat(candidate.fotoUrl()).isEqualTo("https://example.com/photo.jpg");
    }

    @Test
    @DisplayName("Should create candidate with null funcionarioId (synthetic candidates)")
    void candidate_shouldCreateWithNullFuncionarioId() {
        // Given / When
        Candidate blankVote = new Candidate(
                UUID.randomUUID(), ELECTION_ID,
                "Voto en Blanco", true, false,
                null, // funcionarioId = null for synthetic
                null, null, null
        );

        // Then
        assertThat(blankVote.funcionarioId()).isNull();
        assertThat(blankVote.esVotoEnBlanco()).isTrue();
    }

    @Test
    @DisplayName("Should create null vote with null funcionarioId")
    void candidate_nullVote_shouldHaveNullFuncionarioId() {
        // Given / When
        Candidate nullVote = new Candidate(
                UUID.randomUUID(), ELECTION_ID,
                "Voto Nulo", false, true,
                null, null, null, null
        );

        // Then
        assertThat(nullVote.funcionarioId()).isNull();
        assertThat(nullVote.esVotoNulo()).isTrue();
    }

    @Test
    @DisplayName("Candidate does not have numeroOrden field")
    void candidate_shouldNotHaveNumeroOrdenField() {
        // The record must NOT have a 'numeroOrden' accessor method
        boolean hasNumeroOrden;
        try {
            Candidate.class.getMethod("numeroOrden");
            hasNumeroOrden = true;
        } catch (NoSuchMethodException e) {
            hasNumeroOrden = false;
        }
        assertThat(hasNumeroOrden)
                .as("Candidate record must NOT have a 'numeroOrden' method after V5")
                .isFalse();
    }

    @Test
    @DisplayName("Candidate does not have afiliacionPolitica field")
    void candidate_shouldNotHaveAfiliacionPoliticaField() {
        boolean hasAfiliacion;
        try {
            Candidate.class.getMethod("afiliacionPolitica");
            hasAfiliacion = true;
        } catch (NoSuchMethodException e) {
            hasAfiliacion = false;
        }
        assertThat(hasAfiliacion)
                .as("Candidate record must NOT have an 'afiliacionPolitica' method after V5")
                .isFalse();
    }
}
