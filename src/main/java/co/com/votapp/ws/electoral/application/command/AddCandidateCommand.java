package co.com.votapp.ws.electoral.application.command;

import java.util.UUID;

/**
 * Command to add a candidate to an existing election.
 *
 * <p>Cannot be used when the election is FINALIZADA or CANCELADA.
 */
public record AddCandidateCommand(
        UUID eleccionId,
        String nombre,
        String descripcion,
        int numeroOrden
) {
    public AddCandidateCommand {
        if (eleccionId == null) throw new IllegalArgumentException("eleccionId must not be null");
        if (nombre == null || nombre.isBlank()) throw new IllegalArgumentException("nombre must not be blank");
        if (numeroOrden < 1) throw new IllegalArgumentException("numeroOrden must be >= 1");
    }
}
