package co.com.votapp.ws.electoral.infrastructure.adapter.in.web;

import co.com.votapp.ws.electoral.application.port.in.GetEleccionPort;
import co.com.votapp.ws.electoral.domain.Eleccion;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para el módulo electoral.
 */
@RestController
@RequestMapping("/api/v1/elecciones")
public class EleccionController {

    private final GetEleccionPort getEleccionPort;

    public EleccionController(GetEleccionPort getEleccionPort) {
        this.getEleccionPort = getEleccionPort;
    }

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
