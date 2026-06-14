package co.com.votapp.ws.auth.domain.port.in;

import co.com.votapp.ws.auth.domain.Funcionario;

/**
 * Input port for updating a funcionario's personal contact details.
 *
 * <p>Only {@code email} and {@code telefono} may be changed; all other fields
 * (nombres, apellidos, documentoIdentidad, numeroEmpleado, etc.) are preserved
 * from the existing persisted state.
 */
public interface UpdateProfileUseCase {

    /**
     * Updates the email and phone of the funcionario identified by {@code documentoIdentidad}.
     *
     * @param documentoIdentidad the funcionario's document identifier
     * @param email              the new email address (may be null to leave unchanged)
     * @param telefono           the new phone number (may be null to leave unchanged)
     * @return the updated {@link Funcionario} domain object
     * @throws co.com.votapp.ws.common.exception.NotFoundException if no funcionario exists for the given documento
     */
    Funcionario update(String documentoIdentidad, String email, String telefono);
}
