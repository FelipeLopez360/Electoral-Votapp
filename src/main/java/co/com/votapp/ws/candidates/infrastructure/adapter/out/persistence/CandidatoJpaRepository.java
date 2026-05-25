package co.com.votapp.ws.candidates.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link CandidatoEntity}.
 */
public interface CandidatoJpaRepository extends JpaRepository<CandidatoEntity, UUID> {
    List<CandidatoEntity> findByEleccionId(UUID eleccionId);
}
