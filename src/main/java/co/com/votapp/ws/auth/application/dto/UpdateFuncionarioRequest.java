package co.com.votapp.ws.auth.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request body for PUT /api/v1/funcionarios/{id}.
 * All fields are optional — null means "no change" (partial update semantics).
 */
public record UpdateFuncionarioRequest(
        String nombres,
        String apellidos,
        String email,
        String telefono,
        @JsonProperty("departamento_id") Integer departamentoId,
        @JsonProperty("cargo_id") Integer cargoId,
        @JsonProperty("estado_laboral") String estadoLaboral,
        @JsonProperty("puede_votar") Boolean puedeVotar,
        @JsonProperty("debe_cambiar_password") Boolean debeCambiarPassword
) {}
