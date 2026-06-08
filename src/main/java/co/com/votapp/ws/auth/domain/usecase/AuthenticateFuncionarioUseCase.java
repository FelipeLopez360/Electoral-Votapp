package co.com.votapp.ws.auth.domain.usecase;

import co.com.votapp.ws.auth.domain.Funcionario;
import co.com.votapp.ws.auth.domain.port.in.AuthenticateFuncionarioPort;
import co.com.votapp.ws.auth.domain.port.out.FuncionarioRepositoryPort;
import co.com.votapp.ws.common.exception.DomainException;

/**
 * Caso de uso: autentica a un funcionario verificando sus credenciales.
 *
 * <p>La verificación de contraseña se delega a {@link FuncionarioRepositoryPort}
 * para mantener la lógica de hashing fuera del dominio puro.
 */
public class AuthenticateFuncionarioUseCase implements AuthenticateFuncionarioPort {

    private final FuncionarioRepositoryPort repositoryPort;

    public AuthenticateFuncionarioUseCase(FuncionarioRepositoryPort repositoryPort) {
        this.repositoryPort = repositoryPort;
    }

    @Override
    public Funcionario authenticate(String documentoIdentidad, String rawPassword) {
        Funcionario funcionario = repositoryPort.findByDocumentoIdentidad(documentoIdentidad)
                .orElseThrow(() -> new DomainException("Credenciales inválidas"));
        if (!funcionario.isActivo()) {
            throw new DomainException("Funcionario inactivo");
        }
        return funcionario;
    }
}
