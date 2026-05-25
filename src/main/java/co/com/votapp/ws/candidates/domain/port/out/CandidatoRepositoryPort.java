package co.com.votapp.ws.candidates.domain.port.out;

import co.com.votapp.ws.candidates.domain.Candidato;

import java.util.List;
import java.util.UUID;

/**
 * Output port for candidate repository access.
 */
public interface CandidatoRepositoryPort {
    List<Candidato> findByEleccionId(UUID eleccionId);
}
