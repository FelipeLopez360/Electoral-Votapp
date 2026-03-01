package co.com.votapp.ws.electoral.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA repository para {@link EleccionEntity}.
 */
public interface EleccionJpaRepository extends JpaRepository<EleccionEntity, Integer> {
    Optional<EleccionEntity> findByCodigo(String codigo);
}
