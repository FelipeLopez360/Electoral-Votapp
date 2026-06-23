package co.com.votapp.ws.electoral.application.command;

import java.util.UUID;

/**
 * Command to add a candidate to an existing election.
 *
 * <p>Cannot be used when the election is FINALIZADA or CANCELADA.
 * Rich profile fields are all optional (nullable).
 *
 * <p>V5 changes: {@code numeroOrden} replaced by {@code funcionarioId} (nullable Integer).
 * Ordering is now alphabetical by {@code nombre}; the domain enforces uniqueness of
 * {@code funcionarioId} per election (a funcionario can only be a candidate once).
 * {@code afiliacionPolitica} removed.
 */
public record AddCandidateCommand(
        UUID eleccionId,
        String nombre,
        String descripcion,
        Integer funcionarioId,
        String fotoUrl,
        String biografia,
        String propuestas
) {
    public AddCandidateCommand {
        if (eleccionId == null) throw new IllegalArgumentException("eleccionId must not be null");
        if (nombre == null || nombre.isBlank()) throw new IllegalArgumentException("nombre must not be blank");
    }
}
