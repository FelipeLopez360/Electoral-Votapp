package co.com.votapp.ws.voting.infrastructure.adapter.out;

import co.com.votapp.ws.voting.domain.port.out.TokenLockPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
public class RedisTokenLockAdapter implements TokenLockPort {

    private static final Duration TOKEN_TTL = Duration.ofHours(24);
    private final StringRedisTemplate redisTemplate;

    public RedisTokenLockAdapter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean acquire(UUID tokenId) {
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(keyFor(tokenId), "locked", TOKEN_TTL);
        return Boolean.TRUE.equals(success);
    }

    /**
     * Backward-compatible bridge: accepts String tokenId for existing callers.
     * Delegates to the canonical UUID-based acquire.
     */
    public boolean acquireTokenLock(String tokenId) {
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent("token_lock:" + tokenId, "locked", TOKEN_TTL);
        return Boolean.TRUE.equals(success);
    }

    @Override
    public void release(UUID tokenId) {
        redisTemplate.delete(keyFor(tokenId));
    }

    private String keyFor(UUID tokenId) {
        return "token_lock:" + tokenId.toString();
    }
}
