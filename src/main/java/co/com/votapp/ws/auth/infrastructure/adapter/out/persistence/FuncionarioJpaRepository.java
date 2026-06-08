package co.com.votapp.ws.auth.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
