package co.com.votapp.ws.audit.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository para {@link AuditoriaEntity}.
 */
public interface AuditoriaJpaRepository extends JpaRepository<AuditoriaEntity, Integer> {
}
