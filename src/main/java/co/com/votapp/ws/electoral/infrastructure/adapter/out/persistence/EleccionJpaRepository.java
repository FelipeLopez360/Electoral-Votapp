package co.com.votapp.ws.electoral.infrastructure.adapter.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
     * Paginated search across codigo and nombre (case-insensitive).
     * When search is null or blank, all records are returned.
     * Ordering is controlled by the caller via {@link Pageable}'s sort (createdAt DESC).
     */
    @Query("""
            SELECT e FROM EleccionEntity e
            WHERE :search IS NULL OR :search = ''
               OR LOWER(e.codigo) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(e.nombre) LIKE LOWER(CONCAT('%', :search, '%'))
            """)
    Page<EleccionEntity> search(@Param("search") String search, Pageable pageable);

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
