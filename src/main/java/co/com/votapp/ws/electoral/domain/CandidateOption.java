package co.com.votapp.ws.electoral.domain;

import java.util.UUID;

/**
 * Projection of a candidate for the ballot presentation.
 */
public record CandidateOption(
        UUID id,
        String nombre,
        boolean esVotoEnBlanco
) {
    public CandidateOption {
        if (id == null) throw new IllegalArgumentException("id must not be null");
        if (nombre == null || nombre.isBlank()) throw new IllegalArgumentException("nombre must not be blank");
    }
}
