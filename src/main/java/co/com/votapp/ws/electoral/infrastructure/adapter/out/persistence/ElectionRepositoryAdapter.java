package co.com.votapp.ws.electoral.infrastructure.adapter.out.persistence;

import co.com.votapp.ws.common.domain.model.PageResult;
import co.com.votapp.ws.electoral.domain.Election;
import co.com.votapp.ws.electoral.domain.ElectionStatus;
import co.com.votapp.ws.electoral.domain.port.out.ElectionRepositoryPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    public List<Election> findAll() {
        return jpaRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public PageResult<Election> findAll(int page, int size, String search) {
        String effectiveSearch = search == null ? "" : search;
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<EleccionEntity> entityPage = jpaRepository.search(effectiveSearch, pageRequest);
        return new PageResult<>(
                entityPage.getContent().stream().map(this::toDomain).toList(),
                entityPage.getNumber(),
                entityPage.getSize(),
                entityPage.getTotalElements(),
                entityPage.getTotalPages()
        );
    }

    @Override
    public List<Election> findByStatusAndFechaInicioLessThanEqual(ElectionStatus status, LocalDateTime now) {
        Instant nowInstant = now.toInstant(ZoneOffset.UTC);
        return jpaRepository.findByEstadoAndFechaInicioLessThanEqual(status.name(), nowInstant)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Election> findByStatusAndFechaFinLessThanEqual(ElectionStatus status, LocalDateTime now) {
        Instant nowInstant = now.toInstant(ZoneOffset.UTC);
        return jpaRepository.findByEstadoAndFechaFinLessThanEqual(status.name(), nowInstant)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Election save(Election election) {
        EleccionEntity entity = toEntity(election);
        EleccionEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<Election> findByStatus(ElectionStatus status) {
        return jpaRepository.findByEstado(status.name())
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Map<String, Long> countByStatus() {
        // Zero-fill all enum values first, then override with DB-reported counts
        Map<String, Long> result = new HashMap<>();
        for (ElectionStatus status : EnumSet.allOf(ElectionStatus.class)) {
            result.put(status.name(), 0L);
        }
        List<Object[]> rows = jpaRepository.countGroupByEstado();
        for (Object[] row : rows) {
            String estado = (String) row[0];
            Long count = (Long) row[1];
            result.put(estado, count);
        }
        return Map.copyOf(result);
    }

    // ─── Mapping ─────────────────────────────────────────────────────────────

    private Election toDomain(EleccionEntity entity) {
        return new Election(
                entity.getId(),
                entity.getCodigo(),
                entity.getNombre(),
                ElectionStatus.valueOf(entity.getEstado()),
                LocalDateTime.ofInstant(entity.getFechaInicio(), ZoneOffset.UTC),
                LocalDateTime.ofInstant(entity.getFechaFin(), ZoneOffset.UTC),
                entity.isPermiteVotoBlanco(),
                entity.getMaxVotosPorElector()
        );
    }

    private EleccionEntity toEntity(Election election) {
        EleccionEntity entity = new EleccionEntity();
        entity.setId(election.id());
        entity.setCodigo(election.codigo());
        entity.setNombre(election.nombre());
        entity.setEstado(election.status().name());
        entity.setFechaInicio(election.fechaInicio().toInstant(ZoneOffset.UTC));
        entity.setFechaFin(election.fechaFin().toInstant(ZoneOffset.UTC));
        entity.setPermiteVotoBlanco(election.permiteVotoBlanco());
        entity.setMaxVotosPorElector(election.maxVotosPorElector());

        if (election.id() != null) {
            // UPDATE: preserve original createdAt from the DB row — never reset it
            Instant existingCreatedAt = jpaRepository.findById(election.id())
                    .map(EleccionEntity::getCreatedAt)
                    .orElse(Instant.now());
            entity.setCreatedAt(existingCreatedAt);
        } else {
            // INSERT: set createdAt once on first persist
            entity.setCreatedAt(Instant.now());
        }

        entity.setUpdatedAt(Instant.now());
        return entity;
    }
}
