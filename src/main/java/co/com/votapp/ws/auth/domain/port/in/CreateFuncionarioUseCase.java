package co.com.votapp.ws.auth.domain.port.in;

import co.com.votapp.ws.auth.domain.usecase.CreateFuncionarioUseCaseImpl;

/**
 * Input port — create a new funcionario with an auto-generated temporary password.
 *
 * @see CreateFuncionarioUseCaseImpl
 */
public interface CreateFuncionarioUseCase {

    /**
     * Creates a new funcionario.
     *
     * @param command validated creation data
     * @return a result containing the persisted funcionario and the raw temporary password
     *         (shown once to the admin — never persisted in plain text)
     */
    Result create(Command command);

    /**
     * Input data for the create operation.
     *
     * @param nombres             required
     * @param apellidos           required
     * @param tipoDocumento       optional (default CC)
     * @param documentoIdentidad  required, must be unique
     * @param email               optional
     * @param telefono            optional
     * @param departamentoId      optional FK
     * @param cargoId             optional FK
     * @param estadoLaboral       optional (default ACTIVO)
     * @param puedeVotar          eligibility flag
     */
    record Command(
            String nombres,
            String apellidos,
            String tipoDocumento,
            String documentoIdentidad,
            String email,
            String telefono,
            Integer departamentoId,
            Integer cargoId,
            String estadoLaboral,
            boolean puedeVotar
    ) {}

    /**
     * Output of the create operation.
     *
     * @param funcionario      the persisted funcionario (id populated by DB)
     * @param temporaryPassword the raw temporary password — show to admin once, never store
     */
    record Result(
            co.com.votapp.ws.auth.domain.Funcionario funcionario,
            String temporaryPassword
    ) {}
}
