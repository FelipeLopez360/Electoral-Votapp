package co.com.votapp.ws.voting.infrastructure.adapter.out.persistence;

import co.com.votapp.ws.voting.domain.TokenStatus;
import co.com.votapp.ws.voting.domain.VotingToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link VotingTokenRepositoryAdapter#saveAllIssued}.
 *
 * <p>TDD RED → GREEN: tests written BEFORE adapter changes.
 * Mocks {@link VotingTokenJpaRepository} to verify adapter logic in isolation.
 *
 * <h3>Contract under test</h3>
 * <ul>
 *   <li>{@code saveAllIssued} returns ONLY tokens where
 *       {@code insertIssuedOnConflictDoNothing} returned 1 (actual insert).</li>
 *   <li>Tokens skipped at DB level (return 0) are excluded from the result.</li>
 *   <li>A complete retry (all rows conflict) returns an empty list.</li>
 * </ul>
 */
@DisplayName("VotingTokenRepositoryAdapter - saveAllIssued idempotency unit tests")
@ExtendWith(MockitoExtension.class)
class VotingTokenRepositoryAdapterTest {

    @Mock
    private VotingTokenJpaRepository jpaRepository;

    private VotingTokenRepositoryAdapter adapter;

    private static final UUID ELECCION_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final Instant NOW = Instant.parse("2026-06-15T10:00:00Z");

    @BeforeEach
    void setUp() {
        adapter = new VotingTokenRepositoryAdapter(jpaRepository);
    }

    // ── saveAllIssued — all inserted ──────────────────────────────────────────

    @Test
    @DisplayName("Should return all tokens when all inserts succeed (no conflicts)")
    void saveAllIssued_shouldReturnAllTokens_whenNoConflicts() {
        // Given
        VotingToken token1 = buildToken(UUID.randomUUID(), 1L);
        VotingToken token2 = buildToken(UUID.randomUUID(), 2L);
        when(jpaRepository.insertIssuedOnConflictDoNothing(
                eq(token1.id()), eq(ELECCION_ID), eq(1), any(), eq(NOW), any()))
                .thenReturn(1);
        when(jpaRepository.insertIssuedOnConflictDoNothing(
                eq(token2.id()), eq(ELECCION_ID), eq(2), any(), eq(NOW), any()))
                .thenReturn(1);

        // When
        List<VotingToken> result = adapter.saveAllIssued(List.of(token1, token2));

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyInAnyOrder(token1, token2);
    }

    // ── saveAllIssued — partial conflicts (idempotent skips) ──────────────────

    @Test
    @DisplayName("Should return only newly inserted tokens when some conflict (duplicate skipped)")
    void saveAllIssued_shouldReturnOnlyInserted_whenSomeConflict() {
        // Given — token1 inserts (1), token2 conflicts (0 = already exists)
        VotingToken token1 = buildToken(UUID.randomUUID(), 1L);
        VotingToken token2 = buildToken(UUID.randomUUID(), 2L);
        when(jpaRepository.insertIssuedOnConflictDoNothing(
                eq(token1.id()), eq(ELECCION_ID), eq(1), any(), eq(NOW), any()))
                .thenReturn(1);
        when(jpaRepository.insertIssuedOnConflictDoNothing(
                eq(token2.id()), eq(ELECCION_ID), eq(2), any(), eq(NOW), any()))
                .thenReturn(0);  // conflict — DO NOTHING

        // When
        List<VotingToken> result = adapter.saveAllIssued(List.of(token1, token2));

        // Then — only token1 was actually inserted
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(token1);
    }

    // ── saveAllIssued — complete retry (all conflict) ─────────────────────────

    @Test
    @DisplayName("Should return empty list when all tokens already exist (complete retry is safe)")
    void saveAllIssued_shouldReturnEmpty_whenAllConflict() {
        // Given — both tokens already in DB
        VotingToken token1 = buildToken(UUID.randomUUID(), 1L);
        VotingToken token2 = buildToken(UUID.randomUUID(), 2L);
        when(jpaRepository.insertIssuedOnConflictDoNothing(
                eq(token1.id()), eq(ELECCION_ID), eq(1), any(), eq(NOW), any()))
                .thenReturn(0);
        when(jpaRepository.insertIssuedOnConflictDoNothing(
                eq(token2.id()), eq(ELECCION_ID), eq(2), any(), eq(NOW), any()))
                .thenReturn(0);

        // When
        List<VotingToken> result = adapter.saveAllIssued(List.of(token1, token2));

        // Then — nothing inserted, no error
        assertThat(result).isEmpty();
        // Both inserts were attempted (not short-circuited)
        verify(jpaRepository).insertIssuedOnConflictDoNothing(
                eq(token1.id()), eq(ELECCION_ID), eq(1), any(), eq(NOW), any());
        verify(jpaRepository).insertIssuedOnConflictDoNothing(
                eq(token2.id()), eq(ELECCION_ID), eq(2), any(), eq(NOW), any());
    }

    // ── saveAllIssued — empty input ───────────────────────────────────────────

    @Test
    @DisplayName("Should return empty list and skip JPA call when input is empty")
    void saveAllIssued_shouldReturnEmpty_whenInputIsEmpty() {
        // When
        List<VotingToken> result = adapter.saveAllIssued(List.of());

        // Then
        assertThat(result).isEmpty();
        // JPA should never be called for empty input
        org.mockito.Mockito.verifyNoInteractions(jpaRepository);
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private VotingToken buildToken(UUID id, Long funcionarioId) {
        return new VotingToken(id, ELECCION_ID, funcionarioId,
                "hash-" + funcionarioId, TokenStatus.ISSUED, NOW);
    }
}
