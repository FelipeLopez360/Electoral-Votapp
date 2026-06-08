package co.com.votapp.ws.auth.application.dto;

import co.com.votapp.ws.auth.domain.Funcionario;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Response DTO for funcionario endpoints.
 *
 * <p>{@code temporaryPassword} is only present in the create (POST 201) response.
 * It is {@code null} in list and detail responses, and Jackson omits null fields
 * via {@link JsonInclude#NON_NULL}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FuncionarioResponse(
        Integer id,
        String numeroEmpleado,
        String documentoIdentidad,
        String nombres,
        String apellidos,
        String tipoDocumento,
        String email,
        String telefono,
        Integer departamentoId,
        Integer cargoId,
        String estadoLaboral,
        boolean puedeVotar,
        boolean debeCambiarPassword,
        String temporaryPassword
) {

    /** Maps from domain object — no temporary password (list/detail responses). */
    public static FuncionarioResponse fromDomain(Funcionario f) {
        return new FuncionarioResponse(
                f.getId(),
                f.getNumeroEmpleado(),
                f.getDocumentoIdentidad(),
                f.getNombres(),
                f.getApellidos(),
                f.getTipoDocumento(),
                f.getEmail(),
                f.getTelefono(),
                f.getDepartamentoId(),
                f.getCargoId(),
                f.getEstadoLaboral(),
                f.isPuedeVotar(),
                f.isDebeCambiarPassword(),
                null
        );
    }

    /** Maps from domain object and includes the temporary password (create response only). */
    public static FuncionarioResponse fromDomainWithPassword(Funcionario f, String temporaryPassword) {
        return new FuncionarioResponse(
                f.getId(),
                f.getNumeroEmpleado(),
                f.getDocumentoIdentidad(),
                f.getNombres(),
                f.getApellidos(),
                f.getTipoDocumento(),
                f.getEmail(),
                f.getTelefono(),
                f.getDepartamentoId(),
                f.getCargoId(),
                f.getEstadoLaboral(),
                f.isPuedeVotar(),
                f.isDebeCambiarPassword(),
                temporaryPassword
        );
    }
}
