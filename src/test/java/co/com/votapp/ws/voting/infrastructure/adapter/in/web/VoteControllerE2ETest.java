package co.com.votapp.ws.voting.infrastructure.adapter.in.web;

import co.com.votapp.ws.TestcontainersDockerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * E2E integration test for {@link VoteController}.
 *
 * <p>Exercises the full HTTP → CastVoteUseCaseImpl → RedisTokenLockAdapter stack using
 * real Testcontainers (PostgreSQL + Redis).
 *
 * <p>MVP: CastVoteRequest uses rawToken + candidateId.
 * The use case derives a lock key from rawToken via UUID.nameUUIDFromBytes.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.security.user.name=test",
                "spring.security.user.password=test"
        }
)
@Testcontainers
class VoteControllerE2ETest {

    static {
        TestcontainersDockerConfig.configure();
    }

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("votapp_test")
            .withUsername("postgres")
            .withPassword("votapp_password");

    @Container
    @SuppressWarnings("resource")
    private static final GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void containerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", () -> redis.getMappedPort(6379));
    }

    @LocalServerPort
    private int port;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private RestClient client;
    private String rawToken;

    @BeforeEach
    void setUp() {
        String credentials = Base64.getEncoder()
                .encodeToString("test:test".getBytes());
        client = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultHeader("Authorization", "Basic " + credentials)
                .build();

        rawToken = UUID.randomUUID().toString();
        // Compute the same lock key as CastVoteUseCaseImpl to allow cleanup
        UUID lockKey = UUID.nameUUIDFromBytes(rawToken.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        redisTemplate.delete("token_lock:" + lockKey.toString());
    }

    @Test
    void castVote_returnsCreated_whenTokenIsNew() {
        CastVoteRequest request = new CastVoteRequest(
                rawToken,
                UUID.randomUUID().toString()
        );

        ResponseEntity<Void> response = client.post()
                .uri("/api/v1/votes")
                .body(request)
                .retrieve()
                .toBodilessEntity();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void castVote_returnsConflict_whenTokenAlreadyUsed() {
        String candidateId = UUID.randomUUID().toString();
        CastVoteRequest request = new CastVoteRequest(rawToken, candidateId);

        // First call — should succeed with 201
        ResponseEntity<Void> first = client.post()
                .uri("/api/v1/votes")
                .body(request)
                .retrieve()
                .toBodilessEntity();
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Second call with same rawToken — Redis lock already set → DomainException → 409 Conflict
        assertThatThrownBy(() ->
                client.post()
                        .uri("/api/v1/votes")
                        .body(request)
                        .retrieve()
                        .toBodilessEntity()
        )
                .isInstanceOf(RestClientResponseException.class)
                .satisfies(ex -> assertThat(
                        ((RestClientResponseException) ex).getStatusCode()
                ).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void castVote_returnsBadRequest_whenBodyMissing() {
        assertThatThrownBy(() ->
                client.post()
                        .uri("/api/v1/votes")
                        .retrieve()
                        .toBodilessEntity()
        )
                .isInstanceOf(RestClientResponseException.class)
                .satisfies(ex -> assertThat(
                        ((RestClientResponseException) ex).getStatusCode()
                ).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    // Record mirrors VoteController.CastVoteRequest
    record CastVoteRequest(String rawToken, String candidateId) {}
}
