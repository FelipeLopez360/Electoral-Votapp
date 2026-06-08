package co.com.votapp.ws.voting.application.command;

import java.util.UUID;

/**
 * Command to issue a single-use voting token for a funcionario in an election.
 *
 * <p>Pre-conditions enforced by the use case:
 * - Funcionario must be eligible (ACTIVO + puede_votar = true)
 * - No existing ISSUED token for this funcionario+election pair
 * - Election must be ACTIVA
 */
public record IssueVotingTokenCommand(
        Long funcionarioId,
        UUID eleccionId
) {
    public IssueVotingTokenCommand {
        if (funcionarioId == null) throw new IllegalArgumentException("funcionarioId must not be null");
        if (eleccionId == null) throw new IllegalArgumentException("eleccionId must not be null");
    }
}
