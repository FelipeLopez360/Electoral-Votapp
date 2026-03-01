package co.com.votapp.ws.electoral.application.usecase;

import co.com.votapp.ws.common.annotation.UseCase;
import co.com.votapp.ws.common.exception.DomainException;
import co.com.votapp.ws.electoral.application.port.in.GetEleccionPort;
import co.com.votapp.ws.electoral.application.port.out.EleccionRepositoryPort;
import co.com.votapp.ws.electoral.domain.Eleccion;

import java.util.Optional;

/**
 * Caso de uso: recupera una elección por su código.
 */
@UseCase
public class GetEleccionUseCase implements GetEleccionPort {

    private final EleccionRepositoryPort repositoryPort;

    public GetEleccionUseCase(EleccionRepositoryPort repositoryPort) {
        this.repositoryPort = repositoryPort;
    }

    @Override
    public Optional<Eleccion> findByCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new DomainException("El código de elección no puede estar vacío");
        }
        return repositoryPort.findByCodigo(codigo);
    }
}
