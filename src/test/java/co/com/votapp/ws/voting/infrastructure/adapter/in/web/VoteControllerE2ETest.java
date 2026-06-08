package co.com.votapp.ws.voting.infrastructure.adapter.in.web;

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
import org.springframework.test.context.jdbc.Sql;
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
 *
 * <p>Test data is seeded via {@code vote-controller-test-data.sql} before each test method.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.security.user.name=test",
                "spring.security.user.password=test"
        }
)
@Testcontainers
@Sql("/vote-controller-test-data.sql")
class VoteControllerE2ETest {

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

    private static final String TEST_RAW_TOKEN = "e2e-test-raw-token-001";
    private static final String TEST_CANDIDATE_ID = "55d9a2a3-7e05-51fe-836d-4680dc900b58";
    private static final String TEST_TOKEN_ID = "4843a300-6bb8-52f0-9471-a692de52c3c4";

    @BeforeEach
    void setUp() {
        String credentials = Base64.getEncoder()
                .encodeToString("test:test".getBytes());
        client = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultHeader("Authorization", "Basic " + credentials)
                .build();

        rawToken = TEST_RAW_TOKEN;
        // Clean any stale Redis lock key from a previous test run
        redisTemplate.delete("token_lock:" + TEST_TOKEN_ID);
    }

    @Test
    void castVote_returnsCreated_whenTokenIsNew() {
        CastVoteRequest request = new CastVoteRequest(
                rawToken,
                TEST_CANDIDATE_ID
        );

        ResponseEntity<Void> response = client.post()
                .uri("/api/v1/votes")
                .body(request)
                .retrieve()
                .toBodilessEntity();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void castVote_returnsConflict_whenTokenAlreadyUsed() {
        CastVoteRequest request = new CastVoteRequest(rawToken, TEST_CANDIDATE_ID);

        // First call — should succeed with 204
        ResponseEntity<Void> first = client.post()
                .uri("/api/v1/votes")
                .body(request)
                .retrieve()
                .toBodilessEntity();
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

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
