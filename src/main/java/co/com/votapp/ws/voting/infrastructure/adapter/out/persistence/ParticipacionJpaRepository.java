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
}
