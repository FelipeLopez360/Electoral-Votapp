package co.com.votapp.ws.electoral.domain.port.in;

import co.com.votapp.ws.electoral.domain.model.CensoEntry;
import co.com.votapp.ws.common.domain.model.PageResult;

import java.util.UUID;

/**
 * Input port for census management operations.
 *
 * <p>Defines what administrators can do with an election's voter census.
 * All mutation methods enforce that the election is in {@code PROGRAMADA} state.
 *
 * <p>ZERO Spring annotations on this interface — it is pure domain.
 * Wired via {@code DomainConfig}.
 */
public interface ManageCensoUseCase {

    /**
     * Bulk-add all globally eligible funcionarios from a department to the election's census.
     *
     * <p>Eligible means: {@code estadoLaboral = 'ACTIVO'} AND {@code puede_votar = true}.
     * Funcionarios already in the census are skipped (idempotent).
     *
     * @param eleccionId    the election UUID
     * @param departamentoId the department to pull eligible funcionarios from
     * @param adminId       the funcionario_id of the admin performing the action (nullable in MVP)
     * @return counts of added, skipped, and total matching funcionarios
     */
    BulkAddResult addByDepartamento(UUID eleccionId, Integer departamentoId, Integer adminId);

    /**
     * Bulk-add funcionarios using flexible filter criteria.
     *
     * @param eleccionId     the election UUID
     * @param departamentoId optional department filter (null = all departments)
     * @param estadoLaboral  optional labor status filter (e.g. "ACTIVO")
     * @param puedeVotar     optional voting eligibility filter
     * @param adminId        the admin performing the action
     * @return counts of added, skipped, and total matching funcionarios
     */
    BulkAddResult addByFilters(UUID eleccionId, Integer departamentoId,
                                String estadoLaboral, Boolean puedeVotar, Integer adminId);

    /**
     * Add a single funcionario to the election's census.
     *
     * <p>The funcionario must be {@code ACTIVO} and {@code puede_votar = true}.
     * Throws a domain exception if the funcionario is ineligible or already in census.
     *
     * @param eleccionId    the election UUID
     * @param funcionarioId the funcionario to add
     * @param adminId       the admin performing the action
     */
    void addIndividual(UUID eleccionId, Integer funcionarioId, Integer adminId);

    /**
     * Remove a single funcionario from the election's census.
     * Idempotent — does nothing if the funcionario is not in the census.
     *
     * @param eleccionId    the election UUID
     * @param funcionarioId the funcionario to remove
     */
    void removeIndividual(UUID eleccionId, Integer funcionarioId);

    /**
     * Remove all funcionarios from an election's census.
     *
     * @param eleccionId the election UUID
     */
    void clearCenso(UUID eleccionId);

    /**
     * Return a paginated view of the census for an election.
     *
     * @param eleccionId the election UUID
     * @param page       zero-based page number
     * @param size       page size
     * @return paginated census entries (pure Java — no Spring Page)
     */
    PageResult<CensoEntry> listCenso(UUID eleccionId, int page, int size);

    /**
     * Count the total number of entries in an election's census.
     *
     * @param eleccionId the election UUID
     * @return entry count
     */
    long countCenso(UUID eleccionId);

    /**
     * Result of a bulk-add census operation.
     *
     * @param added   number of funcionarios newly added to the census
     * @param skipped number of funcionarios skipped (already in census)
     * @param total   total number of funcionarios matching the filter criteria
     */
    record BulkAddResult(int added, int skipped, int total) {}
}
