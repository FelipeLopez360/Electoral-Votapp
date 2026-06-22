package co.com.votapp.ws.electoral.application.dto;

import java.util.Map;
import java.util.Objects;

/**
 * Read-only DTO for the admin dashboard metrics endpoint
 * ({@code GET /api/v1/dashboard/metrics}).
 *
 * <p>Serialized directly to JSON by the REST layer. All map values for
 * {@code elections.byStatus} include all five {@link co.com.votapp.ws.electoral.domain.ElectionStatus}
 * keys, zero-filled for statuses with no rows. The {@code funcionarios.byStatus} map
 * includes at minimum the ACTIVO and INACTIVO keys, zero-filled when absent.
 *
 * <p>No Spring or JPA imports — pure Java record per hexagonal architecture rules.
 *
 * @param funcionarios aggregate funcionario block
 * @param activeVoters count of globally eligible active voters
 *                     ({@code estadoLaboral='ACTIVO'} AND {@code puedeVotar=true})
 * @param elections    aggregate election block
 */
public record DashboardMetricsResponse(
        FuncionarioBlock funcionarios,
        long activeVoters,
        ElectionBlock elections
) {

    public DashboardMetricsResponse {
        Objects.requireNonNull(funcionarios, "funcionarios block is required");
        Objects.requireNonNull(elections, "elections block is required");
    }

    /**
     * Aggregate counts for the funcionario domain.
     *
     * @param total    total number of funcionarios (sum of all {@code byStatus} values)
     * @param byStatus map of labor status → count; minimum keys: ACTIVO, INACTIVO
     */
    public record FuncionarioBlock(
            long total,
            Map<String, Long> byStatus
    ) {
        public FuncionarioBlock {
            Objects.requireNonNull(byStatus, "byStatus map is required");
        }
    }

    /**
     * Aggregate counts for the election domain.
     *
     * @param total    total number of elections (sum of all {@code byStatus} values)
     * @param byStatus map of {@link co.com.votapp.ws.electoral.domain.ElectionStatus} name → count;
     *                 all 5 enum values always present (zero-filled)
     */
    public record ElectionBlock(
            long total,
            Map<String, Long> byStatus
    ) {
        public ElectionBlock {
            Objects.requireNonNull(byStatus, "byStatus map is required");
        }
    }
}
