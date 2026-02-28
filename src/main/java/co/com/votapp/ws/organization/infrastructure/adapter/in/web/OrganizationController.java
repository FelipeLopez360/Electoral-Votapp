package co.com.votapp.ws.organization.infrastructure.adapter.in.web;

import co.com.votapp.ws.organization.application.port.in.GetDepartamentosPort;
import co.com.votapp.ws.organization.domain.Departamento;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controlador REST para el módulo de organización.
 */
@RestController
@RequestMapping("/api/v1/organization")
public class OrganizationController {

    private final GetDepartamentosPort getDepartamentosPort;

    public OrganizationController(GetDepartamentosPort getDepartamentosPort) {
        this.getDepartamentosPort = getDepartamentosPort;
    }

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
