package co.com.votapp.ws.voting.application.service;

import co.com.votapp.ws.voting.application.command.CastVoteCommand;
import co.com.votapp.ws.voting.domain.port.in.CastVoteByTokenIdPort;
import co.com.votapp.ws.voting.domain.port.in.CastVoteUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Unit tests for {@link CastVoteAppService}.
 *
 * <p>Verifies that the application service correctly delegates to the domain ports
 * without adding any business logic of its own.
 */
@DisplayName("CastVoteAppService - Unit tests (delegation to domain ports)")
@ExtendWith(MockitoExtension.class)
class CastVoteAppServiceTest {

    @Mock
    private CastVoteUseCase castVoteUseCase;

    @Mock
    private CastVoteByTokenIdPort castVoteByTokenIdPort;

    private CastVoteAppService service;

    @BeforeEach
    void setUp() {
        service = new CastVoteAppService(castVoteUseCase, castVoteByTokenIdPort);
    }

    // ─── castVote (rawToken flow) ─────────────────────────────────────────────

    @Test
    @DisplayName("Should delegate castVote(command) to CastVoteUseCase without modification")
    void castVote_shouldDelegateToCastVoteUseCase_withSameCommand() {
        // Given
        CastVoteCommand command = new CastVoteCommand("raw-token-abc", UUID.randomUUID());

        // When
        service.castVote(command);

        // Then
        verify(castVoteUseCase).cast(command);
        verifyNoMoreInteractions(castVoteUseCase, castVoteByTokenIdPort);
    }

    // ─── castVoteByTokenId (portal flow) ─────────────────────────────────────

    @Test
    @DisplayName("Should delegate castVoteByTokenId(tokenId, candidatoId) to CastVoteByTokenIdPort")
    void castVoteByTokenId_shouldDelegateToCastVoteByTokenIdPort_withSameArgs() {
        // Given
        UUID tokenId = UUID.randomUUID();
        UUID candidatoId = UUID.randomUUID();

        // When
        service.castVoteByTokenId(tokenId, candidatoId);

        // Then
        verify(castVoteByTokenIdPort).castVote(tokenId, candidatoId);
        verifyNoMoreInteractions(castVoteUseCase, castVoteByTokenIdPort);
    }
}
