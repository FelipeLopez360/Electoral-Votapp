package co.com.votapp.ws.auth.application.port.out;

import co.com.votapp.ws.auth.domain.Funcionario;

import java.util.Optional;

/**
 * Puerto de salida (driver) para recuperar funcionarios desde la capa de persistencia.
 */
public interface FuncionarioRepositoryPort {
    Optional<Funcionario> findByDocumentoIdentidad(String documentoIdentidad);
}
