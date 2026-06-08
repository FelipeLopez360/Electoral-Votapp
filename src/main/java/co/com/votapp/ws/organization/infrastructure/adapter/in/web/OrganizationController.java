package co.com.votapp.ws.organization.infrastructure.adapter.in.web;

import co.com.votapp.ws.organization.application.dto.CargoResponse;
import co.com.votapp.ws.organization.domain.Departamento;
import co.com.votapp.ws.organization.domain.port.in.GetDepartamentosPort;
import co.com.votapp.ws.organization.domain.port.out.CargoRepositoryPort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for the organization module.
 */
@Tag(name = "Organizations", description = "Query departments, cargos, and organizational data")
@RestController
@RequestMapping("/api/v1/organization")
public class OrganizationController {

    private final GetDepartamentosPort getDepartamentosPort;
    private final CargoRepositoryPort cargoRepositoryPort;

    public OrganizationController(GetDepartamentosPort getDepartamentosPort,
                                   CargoRepositoryPort cargoRepositoryPort) {
        this.getDepartamentosPort = getDepartamentosPort;
        this.cargoRepositoryPort = cargoRepositoryPort;
    }

    @Operation(
            summary = "Get all active departments",
            description = "Returns the list of all active departments registered in the system.",
            security = @SecurityRequirement(name = "basicAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of departments returned (may be empty)"),
            @ApiResponse(responseCode = "401", description = "Authentication required — HTTP Basic credentials missing or invalid")
    })
    @GetMapping("/departamentos")
    public ResponseEntity<List<DepartamentoResponse>> getDepartamentos() {
        List<DepartamentoResponse> response = getDepartamentosPort.findAllActivos()
                .stream()
                .map(d -> new DepartamentoResponse(d.getId(), d.getCodigo(), d.getNombre()))
                .toList();
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get all active cargos",
            description = "Returns the list of all active job positions for use in form dropdowns.",
            security = @SecurityRequirement(name = "basicAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of cargos returned (may be empty)"),
            @ApiResponse(responseCode = "401", description = "Authentication required — HTTP Basic credentials missing or invalid")
    })
    @GetMapping("/cargos")
    public ResponseEntity<List<CargoResponse>> getCargos() {
        List<CargoResponse> response = cargoRepositoryPort.findAll()
                .stream()
                .map(c -> new CargoResponse(c.getId(), c.getCodigo(), c.getNombre(), c.getNivelJerarquico()))
                .toList();
        return ResponseEntity.ok(response);
    }

    /** DTO de salida (Record — obligatorio por arquitectura). */
    public record DepartamentoResponse(Integer id, String codigo, String nombre) {}
}
