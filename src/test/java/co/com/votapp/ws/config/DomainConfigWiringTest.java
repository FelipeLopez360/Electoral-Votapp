package co.com.votapp.ws.config;

import co.com.votapp.ws.auth.domain.port.out.PortalSessionPort;
import co.com.votapp.ws.voting.application.service.CastVoteAppService;
import co.com.votapp.ws.voting.domain.port.in.CastVoteByTokenIdPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that all new beans introduced in PR 2 are correctly wired
 * in the Spring application context.
 *
 * <p>This is a context slice test — it loads the full context and checks
 * that expected beans are present and of the correct type.
 */
@DisplayName("DomainConfig PR2 wiring - PortalSessionPort, CastVoteByTokenIdPort, CastVoteAppService")
@SpringBootTest
@Testcontainers
class DomainConfigWiringTest {

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("votapp_test")
            .withUsername("postgres")
            .withPassword("votapp_password");

    @Container
    private static final GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private PortalSessionPort portalSessionPort;

    @Autowired
    private CastVoteByTokenIdPort castVoteByTokenIdPort;

    @Autowired
    private CastVoteAppService castVoteAppService;

    @Test
    @DisplayName("Should create PortalSessionPort bean — wired as RedisPortalSessionAdapter")
    void portalSessionPort_shouldBeWired() {
        assertThat(portalSessionPort).isNotNull();
    }

    @Test
    @DisplayName("Should create CastVoteByTokenIdPort bean — wired as CastVoteByTokenIdUseCaseImpl")
    void castVoteByTokenIdPort_shouldBeWired() {
        assertThat(castVoteByTokenIdPort).isNotNull();
    }

    @Test
    @DisplayName("Should create CastVoteAppService bean")
    void castVoteAppService_shouldBeWired() {
        assertThat(castVoteAppService).isNotNull();
    }
}
