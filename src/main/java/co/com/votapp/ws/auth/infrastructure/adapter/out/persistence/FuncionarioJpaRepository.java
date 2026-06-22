package co.com.votapp.ws.auth.infrastructure.adapter.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository para {@link FuncionarioEntity}.
 */
public interface FuncionarioJpaRepository extends JpaRepository<FuncionarioEntity, Integer> {

    Optional<FuncionarioEntity> findByDocumentoIdentidad(String documentoIdentidad);

    boolean existsByDocumentoIdentidad(String documentoIdentidad);

    /**
     * Paginated search across nombres, apellidos and documentoIdentidad.
     * When search is null or blank, all records are returned (match-all JPQL condition).
     */
    @Query("""
            SELECT f FROM FuncionarioEntity f
            WHERE :search IS NULL OR :search = ''
               OR LOWER(f.nombres)            LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(f.apellidos)          LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(f.documentoIdentidad) LIKE LOWER(CONCAT('%', :search, '%'))
            """)
    Page<FuncionarioEntity> search(@Param("search") String search, Pageable pageable);

    // ─── Portal auth: lockout support ─────────────────────────────────────────

    @Query("SELECT f.passwordHash FROM FuncionarioEntity f WHERE f.documentoIdentidad = :doc")
    Optional<String> findPasswordHashByDocumentoIdentidad(@Param("doc") String documentoIdentidad);

    @Modifying
    @Transactional
    @Query("UPDATE FuncionarioEntity f SET f.intentosFallidos = COALESCE(f.intentosFallidos, 0) + 1 WHERE f.documentoIdentidad = :doc")
    void incrementFailedAttempts(@Param("doc") String documentoIdentidad);

    @Modifying
    @Transactional
    @Query("UPDATE FuncionarioEntity f SET f.intentosFallidos = 0 WHERE f.documentoIdentidad = :doc")
    void resetFailedAttempts(@Param("doc") String documentoIdentidad);

    @Modifying
    @Transactional
    @Query("UPDATE FuncionarioEntity f SET f.bloqueadoHasta = :lockedUntil WHERE f.documentoIdentidad = :doc")
    void lockAccount(@Param("doc") String documentoIdentidad, @Param("lockedUntil") LocalDateTime lockedUntil);

    @Query("SELECT f.bloqueadoHasta FROM FuncionarioEntity f WHERE f.documentoIdentidad = :doc")
    Optional<LocalDateTime> findBloqueadoHasta(@Param("doc") String documentoIdentidad);

    @Query("SELECT COALESCE(f.intentosFallidos, 0) FROM FuncionarioEntity f WHERE f.documentoIdentidad = :doc")
    int findFailedAttempts(@Param("doc") String documentoIdentidad);

    @Modifying
    @Transactional
    @Query("UPDATE FuncionarioEntity f SET f.ultimoAcceso = :accessTime WHERE f.documentoIdentidad = :doc")
    void updateUltimoAcceso(@Param("doc") String documentoIdentidad, @Param("accessTime") LocalDateTime accessTime);

    @Modifying
    @Transactional
    @Query("UPDATE FuncionarioEntity f SET f.passwordHash = :newHash, f.debeCambiarPassword = false WHERE f.documentoIdentidad = :doc")
    void updatePasswordHash(@Param("doc") String documentoIdentidad, @Param("newHash") String newHash);

    // ─── Census bulk-add queries ───────────────────────────────────────────────

    /**
     * Returns all ACTIVO + puede_votar=true funcionarios from a given department.
     * Used by the electoral context for bulk census population.
     */
    @Query("""
            SELECT f FROM FuncionarioEntity f
            WHERE f.departamentoId = :departamentoId
              AND f.estadoLaboral = 'ACTIVO'
              AND f.puedeVotar = true
            """)
    List<FuncionarioEntity> findEligibleByDepartamento(@Param("departamentoId") Integer departamentoId);

    /**
     * Returns funcionarios matching flexible filter criteria.
     * Null parameters are treated as "match any".
     */
    @Query("""
            SELECT f FROM FuncionarioEntity f
            WHERE (:departamentoId IS NULL OR f.departamentoId = :departamentoId)
              AND (:estadoLaboral IS NULL OR f.estadoLaboral = :estadoLaboral)
              AND (:puedeVotar IS NULL OR f.puedeVotar = :puedeVotar)
            """)
    List<FuncionarioEntity> findEligibleByFilters(
            @Param("departamentoId") Integer departamentoId,
            @Param("estadoLaboral") String estadoLaboral,
            @Param("puedeVotar") Boolean puedeVotar);

    // ─── Dashboard aggregate counts ────────────────────────────────────────────

    /**
     * Aggregate funcionario counts grouped by estadoLaboral.
     *
     * @return list of Object[] pairs: [0] = estadoLaboral (String), [1] = count (Long)
     */
    @Query("SELECT f.estadoLaboral, COUNT(f) FROM FuncionarioEntity f GROUP BY f.estadoLaboral")
    List<Object[]> countGroupByEstadoLaboral();

    /**
     * Count funcionarios where estadoLaboral = 'ACTIVO' AND puedeVotar = true.
     *
     * <p>Mirrors the exact predicate used by {@code findEligibleByDepartamento}
     * but applied globally (no department filter).
     */
    @Query("SELECT COUNT(f) FROM FuncionarioEntity f WHERE f.estadoLaboral = 'ACTIVO' AND f.puedeVotar = true")
    long countActiveEligibleVoters();
}
