package co.com.votapp.ws.electoral.infrastructure.adapter.out.persistence;

import co.com.votapp.ws.electoral.application.port.out.EleccionRepositoryPort;
import co.com.votapp.ws.electoral.domain.Eleccion;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Adaptador de persistencia para el módulo electoral.
 */
@Component
public class EleccionRepositoryAdapter implements EleccionRepositoryPort {

    private final EleccionJpaRepository jpaRepository;

    public EleccionRepositoryAdapter(EleccionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Eleccion> findByCodigo(String codigo) {
        return jpaRepository.findByCodigo(codigo)
                .map(this::toDomain);
    }

    private Eleccion toDomain(EleccionEntity entity) {
        return new Eleccion(
                entity.getUuid(),
                entity.getCodigo(),
                entity.getNombre(),
                entity.getEstado(),
                entity.getFechaInicio(),
                entity.getFechaFin()
        );
    }
}
