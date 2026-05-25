package co.com.votapp.ws.electoral.domain;

import java.util.UUID;

/**
 * Domain type representing a candidate in an election.
 *
 * <p>MVP single-category model. Blank vote is a synthetic candidate
 * created automatically when an election is activated.
 */
public record Candidate(
        UUID id,
        UUID eleccionId,
        String nombre,
        boolean esVotoEnBlanco,
        int numeroOrden
) {
    public Candidate {
        if (id == null) throw new IllegalArgumentException("id must not be null");
        if (eleccionId == null) throw new IllegalArgumentException("eleccionId must not be null");
        if (nombre == null || nombre.isBlank()) throw new IllegalArgumentException("nombre must not be blank");
        if (esVotoEnBlanco && !"Voto en Blanco".equals(nombre))
            throw new IllegalArgumentException("blank vote candidate must be named 'Voto en Blanco'");
    }
}
