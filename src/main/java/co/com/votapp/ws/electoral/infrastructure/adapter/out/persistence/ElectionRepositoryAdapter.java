package co.com.votapp.ws.electoral.infrastructure.adapter.out.persistence;

import co.com.votapp.ws.electoral.domain.Election;
import co.com.votapp.ws.electoral.domain.ElectionStatus;
import co.com.votapp.ws.electoral.domain.port.out.ElectionRepositoryPort;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence adapter for the {@link ElectionRepositoryPort} output port.
 *
 * <p>Maps between the {@link Election} domain record and {@link EleccionEntity} JPA entity.
 * Uses the existing {@link EleccionJpaRepository} backed by the {@code elecciones} table.
 */
@Component
public class ElectionRepositoryAdapter implements ElectionRepositoryPort {

    private final EleccionJpaRepository jpaRepository;

    public ElectionRepositoryAdapter(EleccionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Election> findByCodigo(String codigo) {
        return jpaRepository.findByCodigo(codigo)
                .map(this::toDomain);
    }

    @Override
    public Optional<Election> findById(UUID id) {
        return jpaRepository.findById(id)
                .map(this::toDomain);
    }

    @Override
    public Election save(Election election) {
        EleccionEntity entity = toEntity(election);
        EleccionEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    // ─── Mapping ─────────────────────────────────────────────────────────────

    private Election toDomain(EleccionEntity entity) {
        return new Election(
                entity.getId(),
                entity.getCodigo(),
                entity.getNombre(),
                ElectionStatus.valueOf(entity.getEstado()),
                LocalDateTime.ofInstant(entity.getFechaInicio(), ZoneOffset.UTC),
                LocalDateTime.ofInstant(entity.getFechaFin(), ZoneOffset.UTC)
        );
    }

    private EleccionEntity toEntity(Election election) {
        EleccionEntity entity = new EleccionEntity();
        // Preserve existing id on update; null means INSERT (DB generates UUID)
        entity.setId(election.id());
        entity.setCodigo(election.codigo());
        entity.setNombre(election.nombre());
        entity.setEstado(election.status().name());
        entity.setFechaInicio(election.fechaInicio().toInstant(ZoneOffset.UTC));
        entity.setFechaFin(election.fechaFin().toInstant(ZoneOffset.UTC));
        entity.setCreatedAt(java.time.Instant.now());
        entity.setUpdatedAt(java.time.Instant.now());
        return entity;
    }
}
