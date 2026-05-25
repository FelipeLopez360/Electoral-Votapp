package co.com.votapp.ws.candidates.domain.usecase;

import co.com.votapp.ws.candidates.domain.Candidato;
import co.com.votapp.ws.candidates.domain.port.in.GetCandidatosPort;
import co.com.votapp.ws.candidates.domain.port.out.CandidatoRepositoryPort;

import java.util.List;
import java.util.UUID;

/**
 * Use case: returns candidates for an election.
 */
public class GetCandidatosUseCase implements GetCandidatosPort {

    private final CandidatoRepositoryPort repositoryPort;

    public GetCandidatosUseCase(CandidatoRepositoryPort repositoryPort) {
        this.repositoryPort = repositoryPort;
    }

    @Override
    public List<Candidato> findByEleccionId(UUID eleccionId) {
        if (eleccionId == null) {
            throw new IllegalArgumentException("eleccionId must not be null");
        }
        return repositoryPort.findByEleccionId(eleccionId);
    }
}
