package co.com.votapp.ws.electoral.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository para {@link EleccionEntity}.
 */
public interface EleccionJpaRepository extends JpaRepository<EleccionEntity, UUID> {
    Optional<EleccionEntity> findByCodigo(String codigo);

    List<EleccionEntity> findAllByOrderByCreatedAtDesc();
}
