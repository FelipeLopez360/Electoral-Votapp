package co.com.votapp.ws.electoral.infrastructure.adapter.out.persistence;

import co.com.votapp.ws.candidates.infrastructure.adapter.out.persistence.CandidatoEntity;
import co.com.votapp.ws.candidates.infrastructure.adapter.out.persistence.CandidatoJpaRepository;
import co.com.votapp.ws.electoral.domain.Candidate;
import co.com.votapp.ws.electoral.domain.port.out.CandidateRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Persistence adapter for the {@link CandidateRepositoryPort} output port.
 *
 * <p>Reuses {@link CandidatoEntity} and {@link CandidatoJpaRepository} from the
 * candidates infrastructure context — both map to the same {@code candidatos} table.
 * This adapter adds the query methods required by the electoral use cases.
 */
@Component
public class CandidateRepositoryAdapter implements CandidateRepositoryPort {

    private final CandidatoJpaRepository jpaRepository;

    public CandidateRepositoryAdapter(CandidatoJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Candidate save(Candidate candidate) {
        CandidatoEntity entity = toEntity(candidate);
        CandidatoEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<Candidate> findByEleccionIdOrderByNumeroOrden(UUID eleccionId) {
        return jpaRepository.findByEleccionId(eleccionId)
                .stream()
                .sorted(java.util.Comparator.comparingInt(CandidatoEntity::getNumeroOrden))
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByEleccionIdAndNumeroOrden(UUID eleccionId, int numeroOrden) {
        return jpaRepository.findByEleccionId(eleccionId)
                .stream()
                .anyMatch(e -> numeroOrden == e.getNumeroOrden());
    }

    @Override
    public Optional<Candidate> findByIdAndEleccionId(UUID candidateId, UUID eleccionId) {
        return jpaRepository.findById(candidateId)
                .filter(e -> eleccionId.equals(e.getEleccionId()))
                .map(this::toDomain);
    }

    // ─── Mapping ─────────────────────────────────────────────────────────────

    private Candidate toDomain(CandidatoEntity entity) {
        return new Candidate(
                entity.getId(),
                entity.getEleccionId(),
                entity.getNombre(),
                Boolean.TRUE.equals(entity.getEsVotoEnBlanco()),
                entity.getNumeroOrden()
        );
    }

    private CandidatoEntity toEntity(Candidate candidate) {
        CandidatoEntity entity = new CandidatoEntity();
        entity.setId(candidate.id());
        entity.setEleccionId(candidate.eleccionId());
        entity.setNombre(candidate.nombre());
        entity.setEsVotoEnBlanco(candidate.esVotoEnBlanco());
        entity.setNumeroOrden(candidate.numeroOrden());
        return entity;
    }
}
