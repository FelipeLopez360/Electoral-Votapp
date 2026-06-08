package co.com.votapp.ws.electoral.domain.port.out;

import co.com.votapp.ws.electoral.domain.Election;

import java.util.Optional;
import java.util.UUID;

/**
 * Output port for Election aggregate persistence (MVP domain types).
 *
 * <p>Separate from EleccionRepositoryPort which uses the legacy Eleccion class.
 */
public interface ElectionRepositoryPort {

    /**
     * Find an election by its unique business code.
     */
    Optional<Election> findByCodigo(String codigo);

    /**
     * Find an election by its internal UUID.
     */
    Optional<Election> findById(UUID id);

    /**
     * Persist a new or updated Election.
     *
     * @return the saved election (may have generated id)
     */
    Election save(Election election);
}
