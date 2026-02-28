package co.com.votapp.ws.auth.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA repository para {@link FuncionarioEntity}.
 */
public interface FuncionarioJpaRepository extends JpaRepository<FuncionarioEntity, Integer> {
    Optional<FuncionarioEntity> findByDocumentoIdentidad(String documentoIdentidad);
}
