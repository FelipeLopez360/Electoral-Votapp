package co.com.votapp.ws.organization.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data JPA repository para {@link DepartamentoEntity}.
 */
public interface DepartamentoJpaRepository extends JpaRepository<DepartamentoEntity, Integer> {
    List<DepartamentoEntity> findAllByActivoTrue();
}
