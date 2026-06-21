package co.com.votapp.ws.voting.application.service;

import co.com.votapp.ws.voting.domain.port.in.CastVoteByTokenIdPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link CastVoteAppService} — portal flow only (multi-candidate).
 *
 * <p>The legacy rawToken castVote(CastVoteCommand) method has been removed.
 * The canonical flow uses castVoteByTokenId with a list of candidate UUIDs.
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
    @DisplayName("Should delegate castVoteByTokenId(tokenId, candidatoIds) to CastVoteByTokenIdPort")
    void castVoteByTokenId_shouldDelegateToCastVoteByTokenIdPort_withSameArgs() {
        // Given
        UUID tokenId = UUID.randomUUID();
        UUID candidatoId = UUID.randomUUID();
        List<UUID> candidatoIds = List.of(candidatoId);

        // When
        service.castVoteByTokenId(tokenId, candidatoIds);

        // Then
        verify(castVoteByTokenIdPort).castVote(tokenId, candidatoIds);
    }

    @Test
    @DisplayName("Should delegate multiple candidate IDs correctly to port")
    void castVoteByTokenId_shouldDelegateMultipleCandidateIds_toPort() {
        // Given — triangulation with multiple candidates
        UUID tokenId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        UUID cand1 = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        UUID cand2 = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
        List<UUID> candidatoIds = List.of(cand1, cand2);

        // When
        service.castVoteByTokenId(tokenId, candidatoIds);

        // Then
        verify(castVoteByTokenIdPort).castVote(tokenId, candidatoIds);
    }
}
