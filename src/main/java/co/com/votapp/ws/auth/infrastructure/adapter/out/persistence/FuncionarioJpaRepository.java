package co.com.votapp.ws.auth.infrastructure.adapter.out.persistence;

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

    @Query("""
            SELECT f FROM FuncionarioEntity f
            WHERE :search IS NULL OR :search = ''
               OR LOWER(f.nombres)            LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(f.apellidos)          LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(f.documentoIdentidad) LIKE LOWER(CONCAT('%', :search, '%'))
            """)
    List<FuncionarioEntity> search(@Param("search") String search);

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
}
