package co.com.votapp.ws.electoral.domain;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Aggregate root for the electoral bounded context.
 *
 * <p>Holds the immutable core fields of an election.
 * Lifecycle transitions are enforced by use cases, not here.
 *
 * <p>Ballot configuration fields:
 * <ul>
 *   <li>{@code permiteVotoBlanco} — whether the synthetic "Voto en Blanco" candidate is
 *       created on election activation. Defaults to {@code true}.</li>
 *   <li>{@code maxVotosPorElector} — the maximum number of candidate selections a voter
 *       may submit in a single ballot. Must be &gt;= 1.</li>
 * </ul>
 */
public record Election(
        UUID id,
        String codigo,
        String nombre,
        ElectionStatus status,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin,
        boolean permiteVotoBlanco,
        int maxVotosPorElector
) {
    public Election {
        if (codigo == null || codigo.isBlank()) throw new IllegalArgumentException("codigo must not be blank");
        if (nombre == null || nombre.isBlank()) throw new IllegalArgumentException("nombre must not be blank");
        if (status == null) throw new IllegalArgumentException("status must not be null");
        if (fechaInicio == null) throw new IllegalArgumentException("fechaInicio must not be null");
        if (fechaFin == null) throw new IllegalArgumentException("fechaFin must not be null");
        if (!fechaFin.isAfter(fechaInicio))
            throw new IllegalArgumentException("fechaFin must be after fechaInicio");
        if (maxVotosPorElector < 1)
            throw new IllegalArgumentException("maxVotosPorElector must be >= 1");
    }
}
