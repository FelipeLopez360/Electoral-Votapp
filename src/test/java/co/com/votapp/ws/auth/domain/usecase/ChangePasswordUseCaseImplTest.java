package co.com.votapp.ws.auth.domain.usecase;

import co.com.votapp.ws.auth.domain.exception.InvalidCredentialsException;
import co.com.votapp.ws.auth.domain.exception.WeakPasswordException;
import co.com.votapp.ws.auth.domain.port.out.FuncionarioRepositoryPort;
import co.com.votapp.ws.auth.domain.port.out.PasswordEncoderPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ChangePasswordUseCaseImpl}.
 *
 * <p>Strict TDD: tests written BEFORE the implementation class exists.
 */
@DisplayName("ChangePasswordUseCaseImpl - Password change business rules")
@ExtendWith(MockitoExtension.class)
class ChangePasswordUseCaseImplTest {

    private static final String DOCUMENTO = "12345678";
    private static final String CURRENT_RAW = "OldPass1!";
    private static final String STORED_HASH = "$2a$12$someExistingHash";
    private static final String NEW_STRONG = "NewPass8@";
    private static final String NEW_ENCODED = "$2a$12$newEncodedHash";

    @Mock
    private FuncionarioRepositoryPort repositoryPort;

    @Mock
    private PasswordEncoderPort passwordEncoderPort;

    private ChangePasswordUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new ChangePasswordUseCaseImpl(repositoryPort, passwordEncoderPort);
    }

    // ─── Happy path ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should encode new password and update hash when current password is correct and new is strong")
    void change_shouldUpdateHash_whenCurrentPasswordCorrectAndNewPasswordStrong() {
        // Given
        when(repositoryPort.findPasswordHashByDocumentoIdentidad(DOCUMENTO))
                .thenReturn(Optional.of(STORED_HASH));
        when(passwordEncoderPort.matches(CURRENT_RAW, STORED_HASH)).thenReturn(true);
        when(passwordEncoderPort.encode(NEW_STRONG)).thenReturn(NEW_ENCODED);

        // When
        assertThatNoException().isThrownBy(() -> useCase.change(DOCUMENTO, CURRENT_RAW, NEW_STRONG));

        // Then
        verify(repositoryPort).updatePasswordHash(DOCUMENTO, NEW_ENCODED);
    }

    // ─── Bad current password ─────────────────────────────────────────────────

    @Test
    @DisplayName("Should throw InvalidCredentialsException when current password does not match stored hash")
    void change_shouldThrowInvalidCredentials_whenCurrentPasswordWrong() {
        // Given
        when(repositoryPort.findPasswordHashByDocumentoIdentidad(DOCUMENTO))
                .thenReturn(Optional.of(STORED_HASH));
        when(passwordEncoderPort.matches(CURRENT_RAW, STORED_HASH)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> useCase.change(DOCUMENTO, CURRENT_RAW, NEW_STRONG))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(repositoryPort, never()).updatePasswordHash(DOCUMENTO, NEW_ENCODED);
    }

    @Test
    @DisplayName("Should throw InvalidCredentialsException when no stored hash found (funcionario not found)")
    void change_shouldThrowInvalidCredentials_whenNoStoredHashFound() {
        // Given
        when(repositoryPort.findPasswordHashByDocumentoIdentidad(DOCUMENTO))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> useCase.change(DOCUMENTO, CURRENT_RAW, NEW_STRONG))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(passwordEncoderPort, never()).matches(CURRENT_RAW, STORED_HASH);
        verify(repositoryPort, never()).updatePasswordHash(DOCUMENTO, NEW_ENCODED);
    }

    // ─── Same password rejection ──────────────────────────────────────────────

    @Test
    @DisplayName("Should throw WeakPasswordException when new password equals current password")
    void change_shouldThrowWeakPasswordException_whenNewPasswordSameAsCurrent() {
        // Given
        when(repositoryPort.findPasswordHashByDocumentoIdentidad(DOCUMENTO))
                .thenReturn(Optional.of(STORED_HASH));
        when(passwordEncoderPort.matches(CURRENT_RAW, STORED_HASH)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> useCase.change(DOCUMENTO, CURRENT_RAW, CURRENT_RAW))
                .isInstanceOf(WeakPasswordException.class);

        verify(repositoryPort, never()).updatePasswordHash(anyString(), anyString());
    }

    // ─── Weak password rejection ──────────────────────────────────────────────

    @ParameterizedTest(name = "Weak password: [{0}]")
    @ValueSource(strings = {
            "short1A",     // under 8 chars
            "alllowercase1", // no uppercase
            "ALLUPPERCASE1", // no lowercase
            "NoNumberHere",  // no digit
            "1234567",       // only digits, too short
    })
    @DisplayName("Should throw WeakPasswordException when new password fails strength rules")
    void change_shouldThrowWeakPasswordException_whenNewPasswordIsWeak(String weakPassword) {
        // Given
        when(repositoryPort.findPasswordHashByDocumentoIdentidad(DOCUMENTO))
                .thenReturn(Optional.of(STORED_HASH));
        when(passwordEncoderPort.matches(CURRENT_RAW, STORED_HASH)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> useCase.change(DOCUMENTO, CURRENT_RAW, weakPassword))
                .isInstanceOf(WeakPasswordException.class);

        verify(repositoryPort, never()).updatePasswordHash(DOCUMENTO, weakPassword);
    }

    @Test
    @DisplayName("Should accept minimum valid password: exactly 8 chars with upper, lower, and digit")
    void change_shouldAccept_whenPasswordMeetsMinimumStrengthRules() {
        // Given — "Passwrd1" is exactly 8 chars: upper + lower + digit
        String minValidPassword = "Passwrd1";
        String encodedMin = "$2a$12$encodedMin";
        when(repositoryPort.findPasswordHashByDocumentoIdentidad(DOCUMENTO))
                .thenReturn(Optional.of(STORED_HASH));
        when(passwordEncoderPort.matches(CURRENT_RAW, STORED_HASH)).thenReturn(true);
        when(passwordEncoderPort.encode(minValidPassword)).thenReturn(encodedMin);

        // When & Then — must not throw
        assertThatNoException().isThrownBy(() -> useCase.change(DOCUMENTO, CURRENT_RAW, minValidPassword));

        verify(repositoryPort).updatePasswordHash(DOCUMENTO, encodedMin);
    }
}
