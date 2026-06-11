package co.com.votapp.ws.electoral.domain.port.out;

import co.com.votapp.ws.electoral.domain.Election;
import co.com.votapp.ws.electoral.domain.ElectionStatus;

import java.time.LocalDateTime;
import java.util.List;
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

    /**
     * Return all elections ordered by creation date descending.
     */
    List<Election> findAll();

    /**
     * Return elections with the given status whose fechaInicio is at or before {@code now}.
     *
     * <p>Used by the scheduler to find PROGRAMADA elections ready to activate.
     * Uses inclusive comparison ({@code <= now}) so elections whose start time has just
     * arrived are picked up immediately. All date comparisons are performed in UTC.
     */
    List<Election> findByStatusAndFechaInicioLessThanEqual(ElectionStatus status, LocalDateTime now);

    /**
     * Return elections with the given status whose fechaFin is at or before {@code now}.
     *
     * <p>Used by the scheduler to find ACTIVA elections ready to finalize.
     * Uses inclusive comparison ({@code <= now}) so elections whose end time has just
     * arrived are picked up immediately. All date comparisons are performed in UTC.
     */
    List<Election> findByStatusAndFechaFinLessThanEqual(ElectionStatus status, LocalDateTime now);
}
