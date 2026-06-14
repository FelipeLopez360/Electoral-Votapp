package co.com.votapp.ws.electoral.infrastructure.adapter.out.persistence;

import co.com.votapp.ws.electoral.domain.model.CensoEntry;
import co.com.votapp.ws.electoral.domain.model.PageResult;
import co.com.votapp.ws.electoral.domain.port.out.CensoRepositoryPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Persistence adapter that implements {@link CensoRepositoryPort}.
 *
 * <p>Maps between the pure domain record {@link CensoEntry} and the JPA entity
 * {@link CensoEntity}. All Spring/JPA dependencies are confined to this adapter.
 */
@Component
public class CensoRepositoryAdapter implements CensoRepositoryPort {

    private final CensoJpaRepository jpaRepository;

    public CensoRepositoryAdapter(CensoJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public CensoEntry save(CensoEntry entry) {
        CensoEntity entity = CensoEntity.fromDomain(entry);
        CensoEntity saved = jpaRepository.save(entity);
        return saved.toDomain();
    }

    /**
     * Bulk insert using per-row native {@code ON CONFLICT DO NOTHING} queries.
     *
     * <p>Each row is inserted individually via the native query, which skips the row
     * if it would violate the {@code UNIQUE(eleccion_id, funcionario_id)} constraint.
     * This avoids {@code DataIntegrityViolationException} that would occur with
     * {@code jpaRepository.saveAll()} on duplicate composite keys.
     *
     * <p>A per-row approach is used instead of a set-based {@code unnest()} query
     * because Spring Data JPA cannot bind a {@code List<Integer>} to PostgreSQL's
     * {@code unnest()} in native queries (JDBC type-mapping limitation).
     *
     * <p>Returns the entries that were passed in. Callers that need inserted-vs-skipped
     * counts should use {@link #existsByEleccionIdAndFuncionarioId} before calling this method.
     */
    @Override
    public List<CensoEntry> saveAll(List<CensoEntry> entries) {
        if (entries.isEmpty()) {
            return List.of();
        }

        for (CensoEntry entry : entries) {
            jpaRepository.insertOnConflictDoNothing(
                    entry.eleccionId(),
                    entry.funcionarioId(),
                    entry.agregadoPor()
            );
        }

        return entries;
    }

    @Override
    public void deleteByEleccionIdAndFuncionarioId(UUID eleccionId, Integer funcionarioId) {
        jpaRepository.deleteByEleccionIdAndFuncionarioId(eleccionId, funcionarioId);
    }

    @Override
    public void deleteAllByEleccionId(UUID eleccionId) {
        jpaRepository.deleteAllByEleccionId(eleccionId);
    }

    /**
     * Maps Spring {@code Page<CensoEntity>} to the pure-Java {@link PageResult} at the
     * adapter boundary, so callers in the domain layer never see Spring types.
     */
    @Override
    public PageResult<CensoEntry> findByEleccionId(UUID eleccionId, int page, int size) {
        Page<CensoEntry> springPage = jpaRepository
                .findByEleccionId(eleccionId, PageRequest.of(page, size))
                .map(CensoEntity::toDomain);
        return new PageResult<>(
                springPage.getContent(),
                springPage.getNumber(),
                springPage.getSize(),
                springPage.getTotalElements(),
                springPage.getTotalPages()
        );
    }

    @Override
    public boolean existsByEleccionIdAndFuncionarioId(UUID eleccionId, Integer funcionarioId) {
        return jpaRepository.existsByEleccionIdAndFuncionarioId(eleccionId, funcionarioId);
    }

    @Override
    public long countByEleccionId(UUID eleccionId) {
        return jpaRepository.countByEleccionId(eleccionId);
    }

    @Override
    public boolean hasCensus(UUID eleccionId) {
        return jpaRepository.countByEleccionId(eleccionId) > 0;
    }
}
