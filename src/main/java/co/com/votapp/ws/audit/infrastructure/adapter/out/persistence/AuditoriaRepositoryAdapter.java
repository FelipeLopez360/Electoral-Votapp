package co.com.votapp.ws.audit.infrastructure.adapter.out.persistence;

import co.com.votapp.ws.audit.domain.AuditoriaEvento;
import co.com.votapp.ws.audit.domain.port.out.AuditoriaRepositoryPort;
import org.springframework.stereotype.Component;

/**
 * Adaptador de persistencia para el módulo audit.
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
        entity.setTablaAfectada(evento.getTablaAfectada());
        entity.setRegistroId(evento.getRegistroId());
        entity.setAccion(evento.getAccion());
        entity.setUsuarioId(evento.getUsuarioId());
        entity.setUsuarioTipo(evento.getUsuarioTipo());
        entity.setTimestampAccion(evento.getTimestampAccion());
        entity.setDescripcion(evento.getDescripcion());
        return entity;
    }
}
