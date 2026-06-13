package co.com.votapp.ws.electoral.infrastructure.adapter.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link CensoEntity}.
 *
 * <p>Provides standard CRUD plus custom queries for census operations.
 * The native bulk-insert query uses {@code INSERT ... ON CONFLICT DO NOTHING}
 * to guarantee idempotency without a read-then-filter round-trip.
 */
public interface CensoJpaRepository extends JpaRepository<CensoEntity, UUID> {

    /**
     * Paginated list of census entries for an election.
     */
    Page<CensoEntity> findByEleccionId(UUID eleccionId, Pageable pageable);

    /**
     * Check whether a funcionario is already in an election's census.
     */
    boolean existsByEleccionIdAndFuncionarioId(UUID eleccionId, Integer funcionarioId);

    /**
     * Count entries in an election's census.
     */
    long countByEleccionId(UUID eleccionId);

    /**
     * Delete a single entry by election + funcionario composite key.
     */
    @Modifying
    @Query("DELETE FROM CensoEntity c WHERE c.eleccionId = :eleccionId AND c.funcionarioId = :funcionarioId")
    void deleteByEleccionIdAndFuncionarioId(
            @Param("eleccionId") UUID eleccionId,
            @Param("funcionarioId") Integer funcionarioId);

    /**
     * Delete all entries for a given election (clear census).
     */
    @Modifying
    @Query("DELETE FROM CensoEntity c WHERE c.eleccionId = :eleccionId")
    void deleteAllByEleccionId(@Param("eleccionId") UUID eleccionId);

    /**
     * Idempotent bulk insert using a native PostgreSQL query.
     * {@code ON CONFLICT DO NOTHING} skips rows that would violate the
     * {@code UNIQUE(eleccion_id, funcionario_id)} constraint — no exception is thrown.
     *
     * @param eleccionId     election UUID
     * @param funcionarioIds list of funcionario IDs to add
     * @param agregadoPor    identifier of the admin who added the entries (nullable)
     */
    @Modifying
    @Query(
            value = """
                    INSERT INTO censo_electoral (eleccion_id, funcionario_id, agregado_por, created_at)
                    SELECT :eleccionId, f.id, :agregadoPor, CURRENT_TIMESTAMP
                    FROM   unnest(:funcionarioIds) AS f(id)
                    ON CONFLICT (eleccion_id, funcionario_id) DO NOTHING
                    """,
            nativeQuery = true
    )
    int bulkInsertOnConflictDoNothing(
            @Param("eleccionId") UUID eleccionId,
            @Param("funcionarioIds") List<Integer> funcionarioIds,
            @Param("agregadoPor") Integer agregadoPor);
}
