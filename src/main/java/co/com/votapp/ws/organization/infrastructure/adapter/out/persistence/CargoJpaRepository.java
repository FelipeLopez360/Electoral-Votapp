package co.com.votapp.ws.organization.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link CargoEntity}.
 */
public interface CargoJpaRepository extends JpaRepository<CargoEntity, Integer> {

    List<CargoEntity> findAllByActivoTrue();
}
