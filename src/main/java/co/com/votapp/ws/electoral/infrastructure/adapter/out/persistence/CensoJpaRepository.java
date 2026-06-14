package co.com.votapp.ws.electoral.infrastructure.adapter.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

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
     * {@code @Transactional} is required — {@code @Modifying} JPQL queries
     * must execute within an active transaction or they fail with
     * {@code InvalidDataAccessApiUsageException: No active transaction}.
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM CensoEntity c WHERE c.eleccionId = :eleccionId AND c.funcionarioId = :funcionarioId")
    void deleteByEleccionIdAndFuncionarioId(
            @Param("eleccionId") UUID eleccionId,
            @Param("funcionarioId") Integer funcionarioId);

    /**
     * Delete all entries for a given election (clear census).
     * {@code @Transactional} is required — see note on {@link #deleteByEleccionIdAndFuncionarioId}.
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM CensoEntity c WHERE c.eleccionId = :eleccionId")
    void deleteAllByEleccionId(@Param("eleccionId") UUID eleccionId);

    /**
     * Idempotent single-row insert using a native PostgreSQL query.
     * {@code ON CONFLICT DO NOTHING} skips the row if it would violate the
     * {@code UNIQUE(eleccion_id, funcionario_id)} constraint — no exception is thrown.
     *
     * <p>Called in a loop from {@link co.com.votapp.ws.electoral.infrastructure.adapter.out.persistence.CensoRepositoryAdapter#saveAll}
     * to avoid the JDBC type-mapping issues that arise when trying to pass a Java
     * {@code List<Integer>} to PostgreSQL's {@code unnest()} function.
     *
     * @param eleccionId     election UUID
     * @param funcionarioId  the single funcionario ID to insert
     * @param agregadoPor    identifier of the admin who added the entry (nullable)
     */
    @Modifying
    @Transactional
    @Query(
            value = """
                    INSERT INTO censo_electoral (eleccion_id, funcionario_id, agregado_por, created_at)
                    VALUES (:eleccionId, :funcionarioId, :agregadoPor, CURRENT_TIMESTAMP)
                    ON CONFLICT (eleccion_id, funcionario_id) DO NOTHING
                    """,
            nativeQuery = true
    )
    int insertOnConflictDoNothing(
            @Param("eleccionId") UUID eleccionId,
            @Param("funcionarioId") Integer funcionarioId,
            @Param("agregadoPor") Integer agregadoPor);
}
