package co.com.votapp.ws.organization.domain.port.out;

import co.com.votapp.ws.organization.domain.Cargo;

import java.util.List;

/**
 * Output port for accessing the cargo repository.
 */
public interface CargoRepositoryPort {

    /**
     * Returns all active cargos.
     *
     * @return list of active {@link Cargo} instances; empty if none
     */
    List<Cargo> findAll();
}
