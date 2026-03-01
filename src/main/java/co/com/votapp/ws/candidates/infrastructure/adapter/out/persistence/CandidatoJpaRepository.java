package co.com.votapp.ws.candidates.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data JPA repository para {@link CandidatoEntity}.
 */
public interface CandidatoJpaRepository extends JpaRepository<CandidatoEntity, Integer> {
    List<CandidatoEntity> findByEleccionIdAndActivoTrue(Integer eleccionId);
}
