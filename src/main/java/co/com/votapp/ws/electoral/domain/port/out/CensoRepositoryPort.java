package co.com.votapp.ws.electoral.domain.port.out;

import co.com.votapp.ws.electoral.domain.model.CensoEntry;
import co.com.votapp.ws.electoral.domain.model.PageResult;

import java.util.List;
import java.util.UUID;

/**
 * Output port for censo_electoral persistence.
 *
 * <p>Defined by the domain — ZERO Spring or infrastructure imports.
 * Uses {@link PageResult} as a pure-Java pagination abstraction
 * to avoid importing Spring framework types into the domain layer.
 *
 * <p>Implementations live in {@code infrastructure/adapter/out/persistence}.
 */
public interface CensoRepositoryPort {

    /**
     * Persist a single census entry.
     *
     * @return the saved entry with its generated id
     */
    CensoEntry save(CensoEntry entry);

    /**
     * Persist multiple census entries in bulk.
     * Implementations MUST use INSERT ... ON CONFLICT DO NOTHING for idempotency.
     *
     * @return list of entries that were actually inserted (skipping duplicates)
     */
    List<CensoEntry> saveAll(List<CensoEntry> entries);

    /**
     * Remove a single funcionario from an election's census.
     */
    void deleteByEleccionIdAndFuncionarioId(UUID eleccionId, Integer funcionarioId);

    /**
     * Remove all funcionarios from an election's census.
     */
    void deleteAllByEleccionId(UUID eleccionId);

    /**
     * Return a paginated view of the census for an election.
     */
    PageResult<CensoEntry> findByEleccionId(UUID eleccionId, int page, int size);

    /**
     * Check whether a funcionario is already in an election's census.
     */
    boolean existsByEleccionIdAndFuncionarioId(UUID eleccionId, Integer funcionarioId);

    /**
     * Count the total number of entries in an election's census.
     */
    long countByEleccionId(UUID eleccionId);

    /**
     * Return {@code true} if the election has at least one census entry.
     * Used for the backward-compatibility fallback (empty census → global-only check).
     */
    boolean hasCensus(UUID eleccionId);
}
