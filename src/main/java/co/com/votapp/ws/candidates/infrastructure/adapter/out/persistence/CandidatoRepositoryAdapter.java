package co.com.votapp.ws.candidates.infrastructure.adapter.out.persistence;

import co.com.votapp.ws.candidates.domain.Candidato;
import co.com.votapp.ws.candidates.domain.port.out.CandidatoRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Persistence adapter for the candidates module.
 */
@Component
public class CandidatoRepositoryAdapter implements CandidatoRepositoryPort {

    private final CandidatoJpaRepository jpaRepository;

    public CandidatoRepositoryAdapter(CandidatoJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<Candidato> findByEleccionId(UUID eleccionId) {
        return jpaRepository.findByEleccionId(eleccionId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    private Candidato toDomain(CandidatoEntity entity) {
        return new Candidato(
                entity.getId(),
                entity.getEleccionId(),
                entity.getNombre(),
                Boolean.TRUE.equals(entity.getEsVotoEnBlanco()),
                entity.getNumeroOrden()
        );
    }
}
