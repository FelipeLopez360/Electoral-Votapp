package co.com.votapp.ws.candidates.infrastructure.adapter.out.persistence;

import co.com.votapp.ws.candidates.application.port.out.CandidatoRepositoryPort;
import co.com.votapp.ws.candidates.domain.Candidato;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Adaptador de persistencia para el módulo candidates.
 */
@Component
public class CandidatoRepositoryAdapter implements CandidatoRepositoryPort {

    private final CandidatoJpaRepository jpaRepository;

    public CandidatoRepositoryAdapter(CandidatoJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<Candidato> findByEleccionIdAndActivoTrue(Integer eleccionId) {
        return jpaRepository.findByEleccionIdAndActivoTrue(eleccionId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    private Candidato toDomain(CandidatoEntity entity) {
        return new Candidato(
                entity.getUuid(),
                entity.getEleccionId(),
                entity.getCategoriaId(),
                entity.getNombres(),
                entity.getApellidos(),
                Boolean.TRUE.equals(entity.getEsVotoBlanco()),
                Boolean.TRUE.equals(entity.getActivo())
        );
    }
}
