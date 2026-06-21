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

    /**
     * Returns candidates sorted for ballot display: real candidates ascending by {@code numeroOrden},
     * followed by synthetic candidates (blank vote, null vote) at the end in stable order.
     *
     * <p>Synthetic candidates are stored with special {@code numeroOrden} values
     * ({@code 0} for blank, {@code -1} for null) that would otherwise sort before real candidates
     * when using a simple ascending comparator. This method corrects that by moving all synthetic
     * candidates to the end of the list regardless of their stored {@code numeroOrden}.
     */
    @Override
    public List<Candidate> findByEleccionIdOrderByNumeroOrden(UUID eleccionId) {
        java.util.Comparator<CandidatoEntity> ballotOrder =
                // synthetic candidates (blank or null) sort last; real candidates sort by numeroOrden ascending
                java.util.Comparator.comparingInt(
                        (CandidatoEntity e) -> isSynthetic(e) ? Integer.MAX_VALUE : e.getNumeroOrden()
                ).thenComparingInt(CandidatoEntity::getNumeroOrden);

        return jpaRepository.findByEleccionId(eleccionId)
                .stream()
                .sorted(ballotOrder)
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    /** Returns {@code true} if the entity represents a synthetic candidate (blank or null vote). */
    private boolean isSynthetic(CandidatoEntity entity) {
        return Boolean.TRUE.equals(entity.getEsVotoEnBlanco())
                || Boolean.TRUE.equals(entity.getEsVotoNulo());
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

    @Override
    public void deleteById(UUID candidateId) {
        jpaRepository.deleteById(candidateId);
    }

    // ─── Mapping ─────────────────────────────────────────────────────────────

    private Candidate toDomain(CandidatoEntity entity) {
        return new Candidate(
                entity.getId(),
                entity.getEleccionId(),
                entity.getNombre(),
                Boolean.TRUE.equals(entity.getEsVotoEnBlanco()),
                Boolean.TRUE.equals(entity.getEsVotoNulo()),
                entity.getNumeroOrden(),
                entity.getFotoUrl(),
                entity.getBiografia(),
                entity.getPropuestas(),
                entity.getAfiliacionPolitica()
        );
    }

    private CandidatoEntity toEntity(Candidate candidate) {
        CandidatoEntity entity = new CandidatoEntity();
        // NOT setting ID — Persistable.isNew() checks id==null, so JPA calls persist() not merge()
        entity.setEleccionId(candidate.eleccionId());
        entity.setNombre(candidate.nombre());
        entity.setEsVotoEnBlanco(candidate.esVotoEnBlanco());
        entity.setEsVotoNulo(candidate.esVotoNulo());
        entity.setNumeroOrden(candidate.numeroOrden());
        entity.setFotoUrl(candidate.fotoUrl());
        entity.setBiografia(candidate.biografia());
        entity.setPropuestas(candidate.propuestas());
        entity.setAfiliacionPolitica(candidate.afiliacionPolitica());
        return entity;
    }
}
