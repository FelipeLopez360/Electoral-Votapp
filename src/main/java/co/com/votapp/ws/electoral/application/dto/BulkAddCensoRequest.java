package co.com.votapp.ws.electoral.application.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request body for bulk-adding funcionarios to an election's census by filter criteria.
 *
 * <p>Self-validating record — Bean Validation is applied via {@code @Valid} on controller parameter.
 *
 * @param departamentoId the department to filter by (required)
 * @param estadoLaboral  optional labor status filter; defaults to "ACTIVO" if null
 * @param puedeVotar     optional voting eligibility filter; defaults to true if null
 */
public record BulkAddCensoRequest(
        @NotNull Integer departamentoId,
        String estadoLaboral,
        Boolean puedeVotar
) {}
