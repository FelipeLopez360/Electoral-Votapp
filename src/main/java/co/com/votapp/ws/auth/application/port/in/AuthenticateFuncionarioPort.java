package co.com.votapp.ws.auth.application.port.in;

import co.com.votapp.ws.auth.domain.Funcionario;

/**
 * Puerto de entrada (driven) para autenticación de funcionarios.
 */
public interface AuthenticateFuncionarioPort {
    Funcionario authenticate(String documentoIdentidad, String rawPassword);
}
