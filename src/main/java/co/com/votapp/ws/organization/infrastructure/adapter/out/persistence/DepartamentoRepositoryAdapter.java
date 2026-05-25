package co.com.votapp.ws.organization.infrastructure.adapter.out.persistence;

import co.com.votapp.ws.organization.domain.Departamento;
import co.com.votapp.ws.organization.domain.port.out.DepartamentoRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Adaptador de persistencia para el módulo organization.
 */
@Component
public class DepartamentoRepositoryAdapter implements DepartamentoRepositoryPort {

    private final DepartamentoJpaRepository jpaRepository;

    public DepartamentoRepositoryAdapter(DepartamentoJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<Departamento> findAllByActivoTrue() {
        return jpaRepository.findAllByActivoTrue().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    private Departamento toDomain(DepartamentoEntity entity) {
        return new Departamento(
                entity.getId(),
                entity.getCodigo(),
                entity.getNombre(),
                Boolean.TRUE.equals(entity.getActivo())
        );
    }
}
