package co.com.votapp.ws.organization.infrastructure.adapter.in.web;

import co.com.votapp.ws.organization.application.dto.CargoResponse;
import co.com.votapp.ws.organization.domain.Cargo;
import co.com.votapp.ws.organization.domain.Departamento;
import co.com.votapp.ws.organization.domain.port.in.GetDepartamentosPort;
import co.com.votapp.ws.organization.domain.port.out.CargoRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link OrganizationController}.
 *
 * <p>No Spring context — collaborators are mocked via Mockito.
 * Covers GET /api/v1/organization/cargos and /departamentos endpoint behaviour,
 * response mapping, and empty-list handling.
 */
@DisplayName("OrganizationController - Organization lookup REST adapter")
@ExtendWith(MockitoExtension.class)
class OrganizationControllerTest {

    @Mock private GetDepartamentosPort getDepartamentosPort;
    @Mock private CargoRepositoryPort cargoRepositoryPort;

    private OrganizationController controller;

    @BeforeEach
    void setUp() {
        controller = new OrganizationController(getDepartamentosPort, cargoRepositoryPort);
    }

    // ── GET /api/v1/organization/cargos ───────────────────────────────────────

    @Test
    @DisplayName("Should return 200 with list of active cargos")
    void getCargos_shouldReturn200WithCargoList_whenCargosExist() {
        // Given
        Cargo c1 = new Cargo(1, "DIR", "Director", 1, true);
        Cargo c2 = new Cargo(2, "JEF", "Jefe de Departamento", 2, true);
        Cargo c3 = new Cargo(3, "ANA", "Analista", 3, true);
        when(cargoRepositoryPort.findAll()).thenReturn(List.of(c1, c2, c3));

        // When
        ResponseEntity<List<CargoResponse>> response = controller.getCargos();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(3);

        CargoResponse first = response.getBody().get(0);
        assertThat(first.id()).isEqualTo(1);
        assertThat(first.codigo()).isEqualTo("DIR");
        assertThat(first.nombre()).isEqualTo("Director");
        assertThat(first.nivelJerarquico()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should return 200 with empty list when no active cargos exist")
    void getCargos_shouldReturn200WithEmptyList_whenNoCargosExist() {
        // Given
        when(cargoRepositoryPort.findAll()).thenReturn(List.of());

        // When
        ResponseEntity<List<CargoResponse>> response = controller.getCargos();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
        verify(cargoRepositoryPort).findAll();
    }

    @Test
    @DisplayName("Should map all required cargo fields: id, codigo, nombre, nivelJerarquico")
    void getCargos_shouldMapAllRequiredFields_forEachCargo() {
        // Given
        Cargo cargo = new Cargo(42, "TEC", "Técnico Especializado", 4, true);
        when(cargoRepositoryPort.findAll()).thenReturn(List.of(cargo));

        // When
        ResponseEntity<List<CargoResponse>> response = controller.getCargos();

        // Then
        assertThat(response.getBody()).hasSize(1);
        CargoResponse mapped = response.getBody().get(0);
        assertThat(mapped.id()).isEqualTo(42);
        assertThat(mapped.codigo()).isEqualTo("TEC");
        assertThat(mapped.nombre()).isEqualTo("Técnico Especializado");
        assertThat(mapped.nivelJerarquico()).isEqualTo(4);
    }

    // ── GET /api/v1/organization/departamentos ────────────────────────────────

    @Test
    @DisplayName("Should return 200 with list of active departamentos")
    void getDepartamentos_shouldReturn200WithDepartamentoList_whenDepartamentosExist() {
        // Given
        Departamento d1 = new Departamento(1, "RRHH", "Recursos Humanos", true);
        Departamento d2 = new Departamento(2, "TEC", "Tecnología", true);
        when(getDepartamentosPort.findAllActivos()).thenReturn(List.of(d1, d2));

        // When
        ResponseEntity<List<OrganizationController.DepartamentoResponse>> response =
                controller.getDepartamentos();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody().get(0).id()).isEqualTo(1);
        assertThat(response.getBody().get(0).codigo()).isEqualTo("RRHH");
    }
}
