package co.com.votapp.ws.voting.application.service;

import co.com.votapp.ws.voting.domain.port.in.CastVoteByTokenIdPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link CastVoteAppService} — portal flow only.
 *
 * <p>The legacy rawToken castVote(CastVoteCommand) method has been removed.
 * Only castVoteByTokenId (portal flow) remains.
 */
@DisplayName("CastVoteAppService - Portal flow delegation (castVoteByTokenId)")
@ExtendWith(MockitoExtension.class)
class CastVoteAppServiceTest {

    @Mock
    private CastVoteByTokenIdPort castVoteByTokenIdPort;

    private CastVoteAppService service;

    @BeforeEach
    void setUp() {
        service = new CastVoteAppService(castVoteByTokenIdPort);
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
    }

    @Test
    @DisplayName("Should delegate different tokenId and candidatoId values correctly")
    void castVoteByTokenId_shouldDelegateDistinctIds_toPort() {
        // Given — triangulation with different values
        UUID tokenId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        UUID candidatoId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

        // When
        service.castVoteByTokenId(tokenId, candidatoId);

        // Then
        verify(castVoteByTokenIdPort).castVote(tokenId, candidatoId);
    }
}
