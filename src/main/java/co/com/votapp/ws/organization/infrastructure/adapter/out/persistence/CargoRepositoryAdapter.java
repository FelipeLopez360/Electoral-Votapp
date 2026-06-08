package co.com.votapp.ws.organization.infrastructure.adapter.out.persistence;

import co.com.votapp.ws.organization.domain.Cargo;
import co.com.votapp.ws.organization.domain.port.out.CargoRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Persistence adapter for the Cargo read-model.
 */
@Component
public class CargoRepositoryAdapter implements CargoRepositoryPort {

    private final CargoJpaRepository jpaRepository;

    public CargoRepositoryAdapter(CargoJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<Cargo> findAll() {
        return jpaRepository.findAllByActivoTrue().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    private Cargo toDomain(CargoEntity entity) {
        return new Cargo(
                entity.getId(),
                entity.getCodigo(),
                entity.getNombre(),
                entity.getNivelJerarquico() != null ? entity.getNivelJerarquico() : 1,
                Boolean.TRUE.equals(entity.getActivo())
        );
    }
}
