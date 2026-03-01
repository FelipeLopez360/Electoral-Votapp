package co.com.votapp.ws.auth.infrastructure.adapter.out.persistence;

import co.com.votapp.ws.auth.application.port.out.FuncionarioRepositoryPort;
import co.com.votapp.ws.auth.domain.Funcionario;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Adaptador de persistencia para el módulo auth.
 *
 * <p>Implementa {@link FuncionarioRepositoryPort} usando Spring Data JPA.
 * Convierte entre {@link FuncionarioEntity} (infra) y {@link Funcionario} (dominio).
 */
@Component
public class FuncionarioRepositoryAdapter implements FuncionarioRepositoryPort {

    private final FuncionarioJpaRepository jpaRepository;

    public FuncionarioRepositoryAdapter(FuncionarioJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Funcionario> findByDocumentoIdentidad(String documentoIdentidad) {
        return jpaRepository.findByDocumentoIdentidad(documentoIdentidad)
                .map(this::toDomain);
    }

    private Funcionario toDomain(FuncionarioEntity entity) {
        return new Funcionario(
                entity.getUuid(),
                entity.getNumeroEmpleado(),
                entity.getDocumentoIdentidad(),
                entity.getEmail(),
                Boolean.TRUE.equals(entity.getPuedeVotar()),
                entity.getEstadoLaboral()
        );
    }
}
