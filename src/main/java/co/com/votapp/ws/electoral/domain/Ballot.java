package co.com.votapp.ws.electoral.domain;

import java.util.List;
import java.util.UUID;

/**
 * Ballot presented to the voter after token validation.
 *
 * <p>Contains the election context and the ordered list of candidates
 * (including the synthetic blank-vote candidate).
 */
public record Ballot(
        UUID eleccionId,
        String eleccionNombre,
        List<CandidateOption> candidates
) {
    public Ballot {
        if (eleccionId == null) throw new IllegalArgumentException("eleccionId must not be null");
        if (eleccionNombre == null || eleccionNombre.isBlank())
            throw new IllegalArgumentException("eleccionNombre must not be blank");
        if (candidates == null || candidates.isEmpty())
            throw new IllegalArgumentException("candidates must not be empty");
    }
}
