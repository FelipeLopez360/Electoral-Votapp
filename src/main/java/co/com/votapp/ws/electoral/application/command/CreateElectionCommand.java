package co.com.votapp.ws.electoral.application.command;

import java.time.LocalDateTime;

/**
 * Command to create a new election.
 *
 * <p>The initial status is always PROGRAMADA — enforced by the use case.
 */
public record CreateElectionCommand(
        String codigo,
        String nombre,
        String descripcion,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin
) {
    public CreateElectionCommand {
        if (codigo == null || codigo.isBlank()) throw new IllegalArgumentException("codigo must not be blank");
        if (nombre == null || nombre.isBlank()) throw new IllegalArgumentException("nombre must not be blank");
        if (fechaInicio == null) throw new IllegalArgumentException("fechaInicio must not be null");
        if (fechaFin == null) throw new IllegalArgumentException("fechaFin must not be null");
        if (!fechaFin.isAfter(fechaInicio))
            throw new IllegalArgumentException("fechaFin must be after fechaInicio");
    }
}
