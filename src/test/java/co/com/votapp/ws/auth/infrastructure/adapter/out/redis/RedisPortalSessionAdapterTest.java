package co.com.votapp.ws.auth.infrastructure.adapter.out.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RedisPortalSessionAdapter}.
 *
 * <p>Tests the adapter logic by mocking RedisTemplate. No Spring context required.
 */
@DisplayName("RedisPortalSessionAdapter - Unit tests (mock RedisTemplate)")
@ExtendWith(MockitoExtension.class)
class RedisPortalSessionAdapterTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisPortalSessionAdapter adapter;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        adapter = new RedisPortalSessionAdapter(redisTemplate);
    }

    // ─── createSession ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return a non-blank UUID session token when creating a session")
    void createSession_shouldReturnNonBlankToken_whenCalled() {
        // Given
        Integer funcionarioId = 42;

        // When
        String token = adapter.createSession(funcionarioId);

        // Then
        assertThat(token).isNotBlank();
        // UUID format: 8-4-4-4-12 chars with dashes = 36 chars
        assertThat(token).hasSize(36);
    }

    @Test
    @DisplayName("Should store funcionarioId in Redis with the correct key and TTL when creating session")
    void createSession_shouldStoreInRedisWithTtl_whenCalled() {
        // Given
        Integer funcionarioId = 42;

        // When
        String token = adapter.createSession(funcionarioId);

        // Then
        verify(redisTemplate).opsForValue();
        verify(valueOperations).set(
                eq("portal:session:" + token),
                eq("42"),
                eq(Duration.ofMinutes(30))
        );
    }

    @Test
    @DisplayName("Should return different tokens for different calls — UUIDs must be unique")
    void createSession_shouldReturnUniqueTokens_forEachCall() {
        // Given
        Integer funcionarioId = 99;

        // When
        String token1 = adapter.createSession(funcionarioId);
        String token2 = adapter.createSession(funcionarioId);

        // Then
        assertThat(token1).isNotEqualTo(token2);
    }

    // ─── getFuncionarioIdFromSession ─────────────────────────────────────────

    @Test
    @DisplayName("Should return funcionarioId when session token exists in Redis")
    void getFuncionarioIdFromSession_shouldReturnId_whenKeyExists() {
        // Given
        String sessionToken = "some-uuid-token";
        when(valueOperations.get("portal:session:" + sessionToken)).thenReturn("7");

        // When
        Optional<Integer> result = adapter.getFuncionarioIdFromSession(sessionToken);

        // Then
        assertThat(result).isPresent().contains(7);
    }

    @Test
    @DisplayName("Should return empty when session token does not exist in Redis")
    void getFuncionarioIdFromSession_shouldReturnEmpty_whenKeyMissing() {
        // Given
        String sessionToken = "expired-or-missing-token";
        when(valueOperations.get("portal:session:" + sessionToken)).thenReturn(null);

        // When
        Optional<Integer> result = adapter.getFuncionarioIdFromSession(sessionToken);

        // Then
        assertThat(result).isEmpty();
    }

    // ─── removeSession ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should delete the Redis key when removing a session")
    void removeSession_shouldDeleteKey_whenCalled() {
        // Given
        String sessionToken = "token-to-remove";

        // When
        adapter.removeSession(sessionToken);

        // Then
        verify(redisTemplate).delete("portal:session:" + sessionToken);
    }
}
