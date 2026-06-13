package co.com.votapp.ws.electoral.infrastructure.adapter.out.persistence;

import co.com.votapp.ws.electoral.domain.model.CensoEntry;
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

    @Override
    public List<CensoEntry> saveAll(List<CensoEntry> entries) {
        List<CensoEntity> entities = entries.stream()
                .map(CensoEntity::fromDomain)
                .toList();
        return jpaRepository.saveAll(entities).stream()
                .map(CensoEntity::toDomain)
                .toList();
    }

    @Override
    public void deleteByEleccionIdAndFuncionarioId(UUID eleccionId, Integer funcionarioId) {
        jpaRepository.deleteByEleccionIdAndFuncionarioId(eleccionId, funcionarioId);
    }

    @Override
    public void deleteAllByEleccionId(UUID eleccionId) {
        jpaRepository.deleteAllByEleccionId(eleccionId);
    }

    @Override
    public Page<CensoEntry> findByEleccionId(UUID eleccionId, int page, int size) {
        return jpaRepository
                .findByEleccionId(eleccionId, PageRequest.of(page, size))
                .map(CensoEntity::toDomain);
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
