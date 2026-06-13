package co.com.votapp.ws.electoral.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable domain record representing a single census entry in an election.
 *
 * <p>Belongs exclusively to the domain layer. ZERO Spring or JPA imports.
 *
 * @param id            Internal UUID — null before first persistence.
 * @param eleccionId    UUID of the election this entry belongs to.
 * @param funcionarioId Integer (SERIAL) PK of the funcionario in the census.
 * @param agregadoPor   funcionario_id of the admin who added this entry — nullable in MVP.
 * @param fechaAgregado Timestamp when the entry was added.
 */
public record CensoEntry(
        UUID id,
        UUID eleccionId,
        Integer funcionarioId,
        Integer agregadoPor,
        Instant fechaAgregado
) {
    // Compact constructor: enforce non-null invariants that matter for domain correctness.
    public CensoEntry {
        if (eleccionId == null) throw new IllegalArgumentException("eleccionId must not be null");
        if (funcionarioId == null) throw new IllegalArgumentException("funcionarioId must not be null");
        if (fechaAgregado == null) throw new IllegalArgumentException("fechaAgregado must not be null");
    }
}
