package co.com.votapp.ws.voting.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Spring Data JPA repository for {@link ParticipacionEntity}.
 *
 * <p>Used exclusively by {@link ParticipacionRepositoryAdapter}.
 */
public interface ParticipacionJpaRepository extends JpaRepository<ParticipacionEntity, UUID> {

    boolean existsByEleccionIdAndFuncionarioId(UUID eleccionId, Integer funcionarioId);

    /**
     * Count participation records for a given election.
     * Spring Data derives this as {@code SELECT COUNT(*) FROM participacion_electoral WHERE eleccion_id = ?}.
     *
     * @param eleccionId the election UUID
     * @return number of participation entries for the election
     */
    long countByEleccionId(UUID eleccionId);
}
