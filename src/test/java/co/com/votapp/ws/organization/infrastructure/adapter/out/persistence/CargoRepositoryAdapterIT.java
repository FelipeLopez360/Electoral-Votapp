package co.com.votapp.ws.organization.infrastructure.adapter.out.persistence;

import co.com.votapp.ws.TestcontainersDockerConfig;
import co.com.votapp.ws.organization.domain.Cargo;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link CargoRepositoryAdapter}.
 *
 * <p>Verifies that the cargo adapter correctly reads seed data from the database.
 * The V1 migration seeds 4 active cargos: DIR_GEN, JEFE_DEPT, COORD, FUNC.
 */
@SpringBootTest
@Testcontainers
@DisplayName("CargoRepositoryAdapter - Integration tests with real PostgreSQL")
class CargoRepositoryAdapterIT {

    static {
        TestcontainersDockerConfig.configure();
    }

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("votapp_test")
            .withUsername("postgres")
            .withPassword("votapp_password");

    @Container
    private static final GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private CargoRepositoryAdapter adapter;

    // ─── Tests ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return all 4 active cargos from seed data")
    void shouldFindAllActiveCargos() {
        // When
        List<Cargo> result = adapter.findAll();

        // Then — V1 migration seeds exactly 4 active cargos
        assertThat(result).hasSize(4);
        assertThat(result).allSatisfy(c -> assertThat(c.isActivo()).isTrue());
    }

    @Test
    @DisplayName("Should return cargos with expected seed data codes and hierarchy levels")
    void shouldReturnList() {
        // When
        List<Cargo> result = adapter.findAll();

        // Then — verify expected seed cargos exist
        List<String> codigos = result.stream().map(Cargo::getCodigo).toList();
        assertThat(codigos).containsExactlyInAnyOrder("DIR_GEN", "JEFE_DEPT", "COORD", "FUNC");

        // Verify all have required fields populated
        assertThat(result).allSatisfy(c -> {
            assertThat(c.getId()).isPositive();
            assertThat(c.getCodigo()).isNotBlank();
            assertThat(c.getNombre()).isNotBlank();
            assertThat(c.getNivelJerarquico()).isPositive();
        });

        // Spot-check Director General
        Cargo dirGen = result.stream()
                .filter(c -> "DIR_GEN".equals(c.getCodigo()))
                .findFirst()
                .orElseThrow();
        assertThat(dirGen.getNombre()).isEqualTo("Director General");
        assertThat(dirGen.getNivelJerarquico()).isEqualTo(5);
    }
}
