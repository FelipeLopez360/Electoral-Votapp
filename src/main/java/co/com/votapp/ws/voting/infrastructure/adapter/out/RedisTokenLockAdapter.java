package co.com.votapp.ws.voting.infrastructure.adapter.out;

import co.com.votapp.ws.voting.application.port.out.TokenLockPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RedisTokenLockAdapter implements TokenLockPort {
    private static final Duration TOKEN_TTL = Duration.ofHours(24);
    private final StringRedisTemplate redisTemplate;

    public RedisTokenLockAdapter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean acquireTokenLock(String tokenId) {
        Boolean success = redisTemplate.opsForValue()
            .setIfAbsent(keyFor(tokenId), "locked", TOKEN_TTL);
        return Boolean.TRUE.equals(success);
    }

    private String keyFor(String tokenId) {
        return "token_lock:" + tokenId;
    }
}
