package co.com.votapp.ws.organization.infrastructure.adapter.in.web;

import co.com.votapp.ws.organization.domain.Departamento;
import co.com.votapp.ws.organization.domain.port.in.GetDepartamentosPort;
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
 * Controlador REST para el módulo de organización.
 */
@Tag(name = "Organizations", description = "Query departments and organizational data")
@RestController
@RequestMapping("/api/v1/organization")
public class OrganizationController {

    private final GetDepartamentosPort getDepartamentosPort;

    public OrganizationController(GetDepartamentosPort getDepartamentosPort) {
        this.getDepartamentosPort = getDepartamentosPort;
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

    /** DTO de salida (Record — obligatorio por arquitectura). */
    public record DepartamentoResponse(Integer id, String codigo, String nombre) {}
}
