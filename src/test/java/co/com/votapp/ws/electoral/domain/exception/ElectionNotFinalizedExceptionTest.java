package co.com.votapp.ws.electoral.domain.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ElectionNotFinalizedException.
 *
 * <p>Strict TDD — RED written before production code.
 */
@DisplayName("ElectionNotFinalizedException - Domain exception construction")
class ElectionNotFinalizedExceptionTest {

    @Test
    @DisplayName("Should carry the electionId and produce a meaningful message")
    void exception_shouldExposeElectionId_andHaveDescriptiveMessage() {
        // Given
        UUID electionId = UUID.randomUUID();

        // When
        var ex = new ElectionNotFinalizedException(electionId);

        // Then
        assertThat(ex.getElectionId()).isEqualTo(electionId);
        assertThat(ex.getMessage()).contains(electionId.toString());
    }

    @Test
    @DisplayName("Should produce a message that references FINALIZADA state")
    void exception_shouldMentionFinalizada_inMessage() {
        // Given
        UUID electionId = UUID.randomUUID();

        // When
        var ex = new ElectionNotFinalizedException(electionId);

        // Then
        assertThat(ex.getMessage()).containsIgnoringCase("FINALIZADA");
    }
}
