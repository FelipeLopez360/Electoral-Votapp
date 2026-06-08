package co.com.votapp.ws.auth.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request body for POST /api/v1/funcionarios.
 * All required fields are validated in the domain use case.
 */
public record CreateFuncionarioRequest(
        String nombres,
        String apellidos,
        @JsonProperty("tipo_documento") String tipoDocumento,
        @JsonProperty("documento_identidad") String documentoIdentidad,
        String email,
        String telefono,
        @JsonProperty("departamento_id") Integer departamentoId,
        @JsonProperty("cargo_id") Integer cargoId,
        @JsonProperty("estado_laboral") String estadoLaboral,
        @JsonProperty("puede_votar") boolean puedeVotar
) {}
