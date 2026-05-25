package co.com.votapp.ws.candidates.domain.port.in;

import co.com.votapp.ws.candidates.domain.Candidato;

import java.util.List;
import java.util.UUID;

/**
 * Input port: list candidates for an election.
 */
public interface GetCandidatosPort {
    List<Candidato> findByEleccionId(UUID eleccionId);
}
