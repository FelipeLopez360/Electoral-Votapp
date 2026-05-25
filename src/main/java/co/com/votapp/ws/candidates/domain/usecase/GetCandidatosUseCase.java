package co.com.votapp.ws.candidates.domain.usecase;

import co.com.votapp.ws.candidates.domain.Candidato;
import co.com.votapp.ws.candidates.domain.port.in.GetCandidatosPort;
import co.com.votapp.ws.candidates.domain.port.out.CandidatoRepositoryPort;
import co.com.votapp.ws.common.exception.DomainException;

import java.util.List;

/**
 * Caso de uso: devuelve los candidatos activos de una elección.
 */
public class GetCandidatosUseCase implements GetCandidatosPort {

    private final CandidatoRepositoryPort repositoryPort;

    public GetCandidatosUseCase(CandidatoRepositoryPort repositoryPort) {
        this.repositoryPort = repositoryPort;
    }

    @Override
    public List<Candidato> findByEleccionId(Integer eleccionId) {
        if (eleccionId == null) {
            throw new DomainException("eleccionId no puede ser nulo");
        }
        return repositoryPort.findByEleccionIdAndActivoTrue(eleccionId);
    }
}
