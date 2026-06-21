package co.com.votapp.ws.electoral.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link Election} ballot configuration fields.
 *
 * <p>RED phase: these tests validate the new permiteVotoBlanco and maxVotosPorElector
 * fields added as part of task 1.3. Tests will fail until Election record is updated.
 */
@DisplayName("Election - Ballot configuration fields validation")
class ElectionBallotConfigTest {

    private static final LocalDateTime INICIO = LocalDateTime.now().plusDays(1);
    private static final LocalDateTime FIN = LocalDateTime.now().plusDays(30);

    @Test
    @DisplayName("Should create election with permiteVotoBlanco=true and maxVotosPorElector=1")
    void election_shouldCreateWithDefaultBallotConfig() {
        // Given / When
        Election election = new Election(
                UUID.randomUUID(), "ELEC-001", "Test Election",
                ElectionStatus.PROGRAMADA, INICIO, FIN,
                true, 1
        );

        // Then
        assertThat(election.permiteVotoBlanco()).isTrue();
        assertThat(election.maxVotosPorElector()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should create election with permiteVotoBlanco=false and maxVotosPorElector=3")
    void election_shouldCreateWithCustomBallotConfig() {
        // Given / When
        Election election = new Election(
                UUID.randomUUID(), "ELEC-002", "Multi Vote Election",
                ElectionStatus.PROGRAMADA, INICIO, FIN,
                false, 3
        );

        // Then
        assertThat(election.permiteVotoBlanco()).isFalse();
        assertThat(election.maxVotosPorElector()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when maxVotosPorElector is 0")
    void election_shouldThrowIllegalArgument_whenMaxVotosIsZero() {
        // When & Then
        assertThatThrownBy(() -> new Election(
                UUID.randomUUID(), "ELEC-003", "Bad Election",
                ElectionStatus.PROGRAMADA, INICIO, FIN,
                true, 0
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxVotosPorElector");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when maxVotosPorElector is negative")
    void election_shouldThrowIllegalArgument_whenMaxVotosIsNegative() {
        // When & Then
        assertThatThrownBy(() -> new Election(
                UUID.randomUUID(), "ELEC-004", "Bad Election",
                ElectionStatus.PROGRAMADA, INICIO, FIN,
                true, -1
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxVotosPorElector");
    }

    @Test
    @DisplayName("Should allow maxVotosPorElector=1 (minimum valid value)")
    void election_shouldAllowMinValidMaxVotos() {
        // Given / When
        Election election = new Election(
                UUID.randomUUID(), "ELEC-005", "Single Vote",
                ElectionStatus.PROGRAMADA, INICIO, FIN,
                true, 1
        );

        // Then
        assertThat(election.maxVotosPorElector()).isEqualTo(1);
    }
}
