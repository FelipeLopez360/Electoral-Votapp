package co.com.votapp.ws.candidates.infrastructure.adapter.in.web;

import co.com.votapp.ws.candidates.domain.Candidato;
import co.com.votapp.ws.candidates.domain.port.in.GetCandidatosPort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for the candidates module.
 */
@Tag(name = "Candidates", description = "Query candidates for an election")
@RestController
@RequestMapping("/api/v1/candidates")
public class CandidatesController {

    private final GetCandidatosPort getCandidatosPort;

    public CandidatesController(GetCandidatosPort getCandidatosPort) {
        this.getCandidatosPort = getCandidatosPort;
    }

    @Operation(summary = "Get candidates by election")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of candidates returned"),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @GetMapping("/eleccion/{eleccionId}")
    public ResponseEntity<List<CandidatoResponse>> getCandidatos(@PathVariable UUID eleccionId) {
        List<CandidatoResponse> response = getCandidatosPort.findByEleccionId(eleccionId)
                .stream()
                .map(c -> new CandidatoResponse(
                        c.getId().toString(),
                        c.getNombre(),
                        c.isEsVotoEnBlanco(),
                        c.getNumeroOrden()
                ))
                .toList();
        return ResponseEntity.ok(response);
    }

    public record CandidatoResponse(String id,
                                    String nombre,
                                    boolean esVotoEnBlanco,
                                    int numeroOrden) {}
}
