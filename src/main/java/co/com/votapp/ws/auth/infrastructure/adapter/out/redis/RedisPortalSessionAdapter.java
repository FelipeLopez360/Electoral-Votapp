package co.com.votapp.ws.auth.infrastructure.adapter.out.redis;

import co.com.votapp.ws.auth.domain.port.out.PortalSessionPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Redis-backed adapter for portal session management.
 *
 * <p>Implements {@link PortalSessionPort} using a {@link StringRedisTemplate}.
 * Each session is stored as a key-value pair:
 * <ul>
 *   <li><b>Key</b>: {@code portal:session:{uuid}}</li>
 *   <li><b>Value</b>: {@code funcionarioId} as a string</li>
 *   <li><b>TTL</b>: 30 minutes — refreshed only on explicit re-login</li>
 * </ul>
 *
 * <p>This adapter is the ONLY place that imports Redis infrastructure types for
 * portal sessions. Domain use cases interact exclusively with {@link PortalSessionPort}.
 */
@Component
public class RedisPortalSessionAdapter implements PortalSessionPort {

    private static final String KEY_PREFIX = "portal:session:";
    private static final Duration SESSION_TTL = Duration.ofMinutes(30);

    private final StringRedisTemplate redisTemplate;

    public RedisPortalSessionAdapter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public String createSession(Integer funcionarioId) {
        String sessionToken = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(
                keyFor(sessionToken),
                funcionarioId.toString(),
                SESSION_TTL
        );
        return sessionToken;
    }

    @Override
    public Optional<Integer> getFuncionarioIdFromSession(String sessionToken) {
        String value = redisTemplate.opsForValue().get(keyFor(sessionToken));
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(Integer.parseInt(value));
    }

    @Override
    public void removeSession(String sessionToken) {
        redisTemplate.delete(keyFor(sessionToken));
    }

    private String keyFor(String sessionToken) {
        return KEY_PREFIX + sessionToken;
    }
}
