package co.com.votapp.ws.audit.infrastructure.adapter.out.persistence;

import co.com.votapp.ws.audit.domain.AuditoriaEvento;
import co.com.votapp.ws.audit.domain.port.out.AuditoriaRepositoryPort;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Persistence adapter for the audit module (MVP schema: auditoria_eventos).
 */
@Component
public class AuditoriaRepositoryAdapter implements AuditoriaRepositoryPort {

    private final AuditoriaJpaRepository jpaRepository;

    public AuditoriaRepositoryAdapter(AuditoriaJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void save(AuditoriaEvento evento) {
        AuditoriaEntity entity = toEntity(evento);
        jpaRepository.save(entity);
    }

    private AuditoriaEntity toEntity(AuditoriaEvento evento) {
        AuditoriaEntity entity = new AuditoriaEntity();
        entity.setTipo(evento.getTipo());
        entity.setEleccionId(evento.getEleccionId());
        entity.setFuncionarioId(evento.getFuncionarioId());
        entity.setCandidatoId(evento.getCandidatoId());
        entity.setMetadata(evento.getMetadata());
        entity.setCreatedAt(evento.getCreatedAt() != null ? evento.getCreatedAt() : Instant.now());
        return entity;
    }
}
