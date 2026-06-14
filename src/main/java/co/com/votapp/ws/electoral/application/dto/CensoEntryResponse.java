package co.com.votapp.ws.electoral.application.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Response projection for a single census entry in the list endpoint.
 * Enriched with funcionario details for frontend display.
 *
 * @param id                 internal census entry UUID
 * @param funcionarioId      the funcionario's ID in the census
 * @param numeroEmpleado     the funcionario's employee number
 * @param nombres            the funcionario's first names
 * @param apellidos          the funcionario's last names
 * @param departamentoNombre the name of the funcionario's department
 * @param fechaAgregado      timestamp when the entry was added to the census
 */
public record CensoEntryResponse(
        UUID id,
        Integer funcionarioId,
        String numeroEmpleado,
        String nombres,
        String apellidos,
        String departamentoNombre,
        Instant fechaAgregado
) {}
