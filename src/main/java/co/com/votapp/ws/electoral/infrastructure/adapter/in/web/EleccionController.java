package co.com.votapp.ws.electoral.infrastructure.adapter.in.web;

import co.com.votapp.ws.electoral.domain.Eleccion;
import co.com.votapp.ws.electoral.domain.port.in.GetEleccionPort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para el módulo electoral.
 */
@Tag(name = "Elections", description = "Query active elections")
@RestController
@RequestMapping("/api/v1/elecciones")
public class EleccionController {

    private final GetEleccionPort getEleccionPort;

    public EleccionController(GetEleccionPort getEleccionPort) {
        this.getEleccionPort = getEleccionPort;
    }

    @Operation(
            summary = "Get election by code",
            description = "Returns the election matching the given code if it exists and is active.",
            security = @SecurityRequirement(name = "basicAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Election found"),
            @ApiResponse(responseCode = "401", description = "Authentication required — HTTP Basic credentials missing or invalid"),
            @ApiResponse(responseCode = "404", description = "Election not found for the given code")
    })
    @GetMapping("/{codigo}")
    public ResponseEntity<EleccionResponse> getEleccion(@PathVariable String codigo) {
        return getEleccionPort.findByCodigo(codigo)
                .map(e -> ResponseEntity.ok(new EleccionResponse(
                        e.getUuid().toString(),
                        e.getCodigo(),
                        e.getNombre(),
                        e.getEstado(),
                        e.isActiva()
                )))
                .orElse(ResponseEntity.notFound().build());
    }

    /** DTO de salida (Record — obligatorio por arquitectura). */
    public record EleccionResponse(String uuid,
                                   String codigo,
                                   String nombre,
                                   String estado,
                                   boolean activa) {}
}
