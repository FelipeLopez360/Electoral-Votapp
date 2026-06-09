package co.com.votapp.ws.auth.domain.port;

import co.com.votapp.ws.auth.domain.port.out.PasswordEncoderPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract tests for {@link PasswordEncoderPort} — verifies the {@code matches} method exists (Task 1.2).
 *
 * <p>RED phase: fails until {@code matches()} is added to the interface.
 */
@DisplayName("PasswordEncoderPort - Contract test for matches() method")
class PasswordEncoderPortContractTest {

    @Test
    @DisplayName("Should expose matches(rawPassword, encodedPassword) method on the port interface")
    void passwordEncoderPort_shouldHaveMatchesMethod() throws NoSuchMethodException {
        // Then — the interface must declare matches(String, String)
        var method = PasswordEncoderPort.class.getMethod("matches", String.class, String.class);
        assertThat(method).isNotNull();
        assertThat(method.getReturnType()).isEqualTo(boolean.class);
    }

    @Test
    @DisplayName("Should return true when rawPassword matches the encoded password")
    void matches_shouldReturnTrue_whenPasswordsMatch() {
        // Given
        PasswordEncoderPort port = Mockito.mock(PasswordEncoderPort.class);
        Mockito.when(port.matches("rawPass", "encodedHash")).thenReturn(true);

        // When
        boolean result = port.matches("rawPass", "encodedHash");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return false when rawPassword does not match the encoded password")
    void matches_shouldReturnFalse_whenPasswordsDoNotMatch() {
        // Given
        PasswordEncoderPort port = Mockito.mock(PasswordEncoderPort.class);
        Mockito.when(port.matches("wrongPass", "encodedHash")).thenReturn(false);

        // When
        boolean result = port.matches("wrongPass", "encodedHash");

        // Then
        assertThat(result).isFalse();
    }
}
