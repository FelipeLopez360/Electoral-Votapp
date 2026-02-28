package co.com.votapp.ws.electoral.application.port.out;

import co.com.votapp.ws.electoral.domain.Eleccion;

import java.util.Optional;

/**
 * Puerto de salida para acceder al repositorio de elecciones.
 */
public interface EleccionRepositoryPort {
    Optional<Eleccion> findByCodigo(String codigo);
}
