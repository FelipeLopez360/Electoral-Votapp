package co.com.votapp.ws.electoral.application.port.in;

import co.com.votapp.ws.electoral.domain.Eleccion;

import java.util.Optional;

/**
 * Puerto de entrada para obtener detalles de una elección activa.
 */
public interface GetEleccionPort {
    Optional<Eleccion> findByCodigo(String codigo);
}
