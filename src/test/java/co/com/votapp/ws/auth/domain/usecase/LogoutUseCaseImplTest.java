package co.com.votapp.ws.auth.domain.usecase;

import co.com.votapp.ws.auth.domain.port.out.PortalSessionPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Unit tests for {@link LogoutUseCaseImpl}.
 *
 * <p>Strict TDD: tests written BEFORE the implementation class exists.
 */
@DisplayName("LogoutUseCaseImpl - Session invalidation")
@ExtendWith(MockitoExtension.class)
class LogoutUseCaseImplTest {

    private static final String SESSION_TOKEN = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";

    @Mock
    private PortalSessionPort sessionPort;

    private LogoutUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new LogoutUseCaseImpl(sessionPort);
    }

    // ─── Happy path ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should delegate to PortalSessionPort.removeSession exactly once with the given token")
    void logout_shouldCallRemoveSession_exactlyOnce_withGivenToken() {
        // When
        useCase.logout(SESSION_TOKEN);

        // Then
        verify(sessionPort, times(1)).removeSession(SESSION_TOKEN);
        verifyNoMoreInteractions(sessionPort);
    }

    // ─── Triangulate with a different token ──────────────────────────────────

    @Test
    @DisplayName("Should pass the correct token to removeSession — not a different token")
    void logout_shouldPassExactTokenToRemoveSession_notADifferentToken() {
        // Given — different token
        String otherToken = "ffffffff-0000-4000-8000-000000000001";

        // When
        useCase.logout(otherToken);

        // Then — exactly the supplied token, not the one from constants
        verify(sessionPort).removeSession(otherToken);
        verify(sessionPort, times(0)).removeSession(SESSION_TOKEN);
    }
}
