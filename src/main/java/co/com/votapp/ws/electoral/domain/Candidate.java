package co.com.votapp.ws.electoral.domain;

import java.util.UUID;

/**
 * Domain type representing a candidate in an election.
 *
 * <p>MVP single-category model. Blank vote and null vote are synthetic candidates
 * created automatically when an election is activated.
 *
 * <p>Rich profile fields ({@code fotoUrl}, {@code biografia}, {@code propuestas})
 * are optional (nullable). Synthetic candidates (blank vote, null vote) always pass
 * {@code null} for these fields.
 *
 * <p>V5 changes: {@code numeroOrden} and {@code afiliacionPolitica} removed;
 * {@code funcionarioId} (nullable Integer FK → funcionarios) added.
 * Ordering is now alphabetical by {@code nombre} in the persistence adapter.
 * Synthetic candidates have {@code null} funcionarioId (partial unique index allows this).
 */
public record Candidate(
        UUID id,
        UUID eleccionId,
        String nombre,
        boolean esVotoEnBlanco,
        boolean esVotoNulo,
        Integer funcionarioId,
        String fotoUrl,
        String biografia,
        String propuestas
) {
    public Candidate {
        if (id == null) throw new IllegalArgumentException("id must not be null");
        if (eleccionId == null) throw new IllegalArgumentException("eleccionId must not be null");
        if (nombre == null || nombre.isBlank()) throw new IllegalArgumentException("nombre must not be blank");
        if (esVotoEnBlanco && !"Voto en Blanco".equals(nombre))
            throw new IllegalArgumentException("blank vote candidate must be named 'Voto en Blanco'");
        if (esVotoNulo && !"Voto Nulo".equals(nombre))
            throw new IllegalArgumentException("null vote candidate must be named 'Voto Nulo'");
        if (esVotoEnBlanco && esVotoNulo)
            throw new IllegalArgumentException("a candidate cannot be both blank vote and null vote");
    }
}
