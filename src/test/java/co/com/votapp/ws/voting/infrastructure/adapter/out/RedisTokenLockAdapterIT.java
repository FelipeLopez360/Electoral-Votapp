package co.com.votapp.ws.voting.infrastructure.adapter.out;

import co.com.votapp.ws.TestcontainersDockerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test for {@link RedisTokenLockAdapter} — requires Docker.
 *
 * <p>Named {@code *IT.java} to run under Maven Failsafe ({@code ./mvnw verify})
 * and NOT under Surefire ({@code ./mvnw test}). This prevents the flaky
 * ApplicationContext failure (localhost:55555 refused) that occurred when the
 * test was named {@code *Test.java} and ran alongside lightweight unit tests
 * in the aggregate Surefire pass — Testcontainers startup is heavyweight and
 * races with the Surefire classloader on macOS Docker Desktop.
 *
 * <p>The test logic itself is unchanged. The rename moves it to the correct
 * Maven lifecycle phase per project conventions documented in {@code AGENTS.md}.
 */
@SpringBootTest
@Testcontainers
class RedisTokenLockAdapterIT {

    static {
        TestcontainersDockerConfig.configure();
    }

    @Container
    private static final GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379);

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("votapp_test")
        .withUsername("postgres")
        .withPassword("votapp_password");

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private RedisTokenLockAdapter adapter;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void cleanTokenKey() {
        redisTemplate.delete("token_lock:token-123");
    }

    @Test
    void setIfAbsentPreventsSecondConcurrentTokenUse() throws Exception {
        String tokenId = "token-123";
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);
        AtomicInteger successCount = new AtomicInteger();
        AtomicBoolean firstResult = new AtomicBoolean();
        AtomicBoolean secondResult = new AtomicBoolean();

        Thread t1 = new Thread(() -> {
            await(start);
            boolean result = adapter.acquireTokenLock(tokenId);
            firstResult.set(result);
            if (result) {
                successCount.incrementAndGet();
            }
            done.countDown();
        });

        Thread t2 = new Thread(() -> {
            await(start);
            boolean result = adapter.acquireTokenLock(tokenId);
            secondResult.set(result);
            if (result) {
                successCount.incrementAndGet();
            }
            done.countDown();
        });

        t1.start();
        t2.start();
        start.countDown();
        done.await();

        assertEquals(1, successCount.get());
        assertTrue(firstResult.get() ^ secondResult.get());
    }

    private void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
