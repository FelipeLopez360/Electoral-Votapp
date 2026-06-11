package co.com.votapp.ws.electoral.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository para {@link EleccionEntity}.
 */
public interface EleccionJpaRepository extends JpaRepository<EleccionEntity, UUID> {
    Optional<EleccionEntity> findByCodigo(String codigo);

    List<EleccionEntity> findAllByOrderByCreatedAtDesc();

    /**
     * Find elections by estado (String) whose fechaInicio is at or before {@code now} ({@code <=}).
     * Used by the scheduler to detect PROGRAMADA elections due for activation.
     */
    List<EleccionEntity> findByEstadoAndFechaInicioLessThanEqual(String estado, Instant now);

    /**
     * Find elections by estado (String) whose fechaFin is at or before {@code now} ({@code <=}).
     * Used by the scheduler to detect ACTIVA elections due for finalization.
     */
    List<EleccionEntity> findByEstadoAndFechaFinLessThanEqual(String estado, Instant now);
}
