package co.com.votapp.ws.organization.application.usecase;

import co.com.votapp.ws.common.annotation.UseCase;
import co.com.votapp.ws.organization.application.port.in.GetDepartamentosPort;
import co.com.votapp.ws.organization.application.port.out.DepartamentoRepositoryPort;
import co.com.votapp.ws.organization.domain.Departamento;

import java.util.List;

/**
 * Caso de uso: devuelve todos los departamentos activos de la organización.
 */
@UseCase
public class GetDepartamentosUseCase implements GetDepartamentosPort {

    private final DepartamentoRepositoryPort repositoryPort;

    public GetDepartamentosUseCase(DepartamentoRepositoryPort repositoryPort) {
        this.repositoryPort = repositoryPort;
    }

    @Override
    public List<Departamento> findAllActivos() {
        return repositoryPort.findAllByActivoTrue();
    }
}
