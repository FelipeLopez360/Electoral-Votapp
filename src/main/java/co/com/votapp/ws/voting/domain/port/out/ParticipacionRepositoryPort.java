package co.com.votapp.ws.voting.domain.port.out;

import java.time.Instant;
import java.util.UUID;

/**
 * Output port for recording voter participation (participacion_electoral).
 *
 * <p>This table records THAT a funcionario voted — not WHAT they voted for.
 * It is used to enforce the one-vote-per-election invariant.
 * Must be written within the same DB transaction as the vote itself.
 */
public interface ParticipacionRepositoryPort {

    /**
     * Record that a funcionario participated in the election.
     * Must be called inside an existing DB transaction.
     *
     * @param eleccionId   the election UUID
     * @param funcionarioId the funcionario INTEGER id
     * @param votedAt       the exact timestamp of voting
     */
    void markParticipation(UUID eleccionId, Integer funcionarioId, Instant votedAt);
}
