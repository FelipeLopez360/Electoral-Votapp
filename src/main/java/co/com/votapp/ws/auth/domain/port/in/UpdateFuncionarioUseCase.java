package co.com.votapp.ws.auth.domain.port.in;

import co.com.votapp.ws.auth.domain.Funcionario;
import co.com.votapp.ws.auth.domain.usecase.UpdateFuncionarioUseCaseImpl;

/**
 * Input port — update an existing funcionario's editable fields.
 *
 * @see UpdateFuncionarioUseCaseImpl
 */
public interface UpdateFuncionarioUseCase {

    /**
     * Updates an existing funcionario.
     *
     * @param command update data (null fields are ignored — partial update)
     * @return the updated funcionario
     * @throws co.com.votapp.ws.common.exception.NotFoundException if the id does not exist
     */
    Funcionario update(Command command);

    /**
     * Update command — all fields except {@code id} are optional (null = no change).
     */
    record Command(
            Integer id,
            String nombres,
            String apellidos,
            String email,
            String telefono,
            Integer departamentoId,
            Integer cargoId,
            String estadoLaboral,
            Boolean puedeVotar,
            Boolean debeCambiarPassword
    ) {}
}
