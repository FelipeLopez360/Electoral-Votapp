package co.com.votapp.ws.candidates.infrastructure.adapter.in.web;

import co.com.votapp.ws.candidates.application.port.in.GetCandidatosPort;
import co.com.votapp.ws.candidates.domain.Candidato;
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

import java.util.List;

/**
 * Controlador REST para el módulo de candidatos.
 */
@Tag(name = "Candidates", description = "Query candidates for an election")
@RestController
@RequestMapping("/api/v1/candidates")
public class CandidatesController {

    private final GetCandidatosPort getCandidatosPort;

    public CandidatesController(GetCandidatosPort getCandidatosPort) {
        this.getCandidatosPort = getCandidatosPort;
    }

    @Operation(
            summary = "Get candidates by election",
            description = "Returns the list of candidates registered for the given election ID.",
            security = @SecurityRequirement(name = "basicAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of candidates returned (may be empty)"),
            @ApiResponse(responseCode = "401", description = "Authentication required — HTTP Basic credentials missing or invalid")
    })
    @GetMapping("/eleccion/{eleccionId}")
    public ResponseEntity<List<CandidatoResponse>> getCandidatos(@PathVariable Integer eleccionId) {
        List<CandidatoResponse> response = getCandidatosPort.findByEleccionId(eleccionId)
                .stream()
                .map(c -> new CandidatoResponse(
                        c.getUuid().toString(),
                        c.getNombres(),
                        c.getApellidos(),
                        c.getNombreCompleto(),
                        c.isEsVotoBlanco()
                ))
                .toList();
        return ResponseEntity.ok(response);
    }

    /** DTO de salida (Record — obligatorio por arquitectura). */
    public record CandidatoResponse(String uuid,
                                    String nombres,
                                    String apellidos,
                                    String nombreCompleto,
                                    boolean esVotoBlanco) {}
}
