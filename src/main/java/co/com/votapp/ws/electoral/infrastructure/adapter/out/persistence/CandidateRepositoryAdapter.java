package co.com.votapp.ws.electoral.infrastructure.adapter.out.persistence;

import co.com.votapp.ws.candidates.infrastructure.adapter.out.persistence.CandidatoEntity;
import co.com.votapp.ws.candidates.infrastructure.adapter.out.persistence.CandidatoJpaRepository;
import co.com.votapp.ws.electoral.domain.Candidate;
import co.com.votapp.ws.electoral.domain.port.out.CandidateRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Persistence adapter for the {@link CandidateRepositoryPort} output port.
 *
 * <p>Reuses {@link CandidatoEntity} and {@link CandidatoJpaRepository} from the
 * candidates infrastructure context — both map to the same {@code candidatos} table.
 *
 * <p>V5 changes:
 * - {@code findByEleccionIdOrderByNombre}: sorts real candidates alphabetically by nombre ASC,
 *   synthetic candidates (blank/null vote) always appear last in stable order.
 * - {@code existsByEleccionIdAndFuncionarioId}: replaces old {@code existsByEleccionIdAndNumeroOrden}.
 * - Mapping: {@code funcionarioId} added; {@code numeroOrden}/{@code afiliacionPolitica} removed.
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
     * Returns candidates for ballot display: real candidates alphabetically by {@code nombre} ASC,
     * followed by synthetic candidates (blank vote, null vote) at the end in stable order.
     *
     * <p>Real candidates: {@code esVotoEnBlanco=false AND esVotoNulo=false}, sorted by nombre ASC.
     * Synthetic candidates: sorted by nombre ASC among themselves (stable, predictable last position).
     */
    @Override
    public List<Candidate> findByEleccionIdOrderByNombre(UUID eleccionId) {
        Comparator<CandidatoEntity> ballotOrder = Comparator
                // Synthetic candidates (blank or null) sort last
                .comparingInt((CandidatoEntity e) -> isSynthetic(e) ? 1 : 0)
                // Among non-synthetics and among synthetics, sort alphabetically by nombre
                .thenComparing(CandidatoEntity::getNombre, String.CASE_INSENSITIVE_ORDER);

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
    public boolean existsByEleccionIdAndFuncionarioId(UUID eleccionId, Integer funcionarioId) {
        if (funcionarioId == null) return false;
        return jpaRepository.findByEleccionId(eleccionId)
                .stream()
                .anyMatch(e -> funcionarioId.equals(e.getFuncionarioId()));
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
                entity.getFuncionarioId(),
                entity.getFotoUrl(),
                entity.getBiografia(),
                entity.getPropuestas()
        );
    }

    private CandidatoEntity toEntity(Candidate candidate) {
        CandidatoEntity entity = new CandidatoEntity();
        // NOT setting ID — Persistable.isNew() checks id==null, so JPA calls persist() not merge()
        entity.setEleccionId(candidate.eleccionId());
        entity.setNombre(candidate.nombre());
        entity.setEsVotoEnBlanco(candidate.esVotoEnBlanco());
        entity.setEsVotoNulo(candidate.esVotoNulo());
        entity.setFuncionarioId(candidate.funcionarioId());
        entity.setFotoUrl(candidate.fotoUrl());
        entity.setBiografia(candidate.biografia());
        entity.setPropuestas(candidate.propuestas());
        return entity;
    }
}
