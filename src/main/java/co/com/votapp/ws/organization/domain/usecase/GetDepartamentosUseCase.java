package co.com.votapp.ws.organization.domain.usecase;

import co.com.votapp.ws.organization.domain.Departamento;
import co.com.votapp.ws.organization.domain.port.in.GetDepartamentosPort;
import co.com.votapp.ws.organization.domain.port.out.DepartamentoRepositoryPort;

import java.util.List;

/**
 * Caso de uso: devuelve todos los departamentos activos de la organización.
 */
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
