package co.com.votapp.ws.electoral.domain.port.in;

import java.util.UUID;

/**
 * Input port: transition an election from ACTIVA to FINALIZADA.
 *
 * <p>Cannot transition from CANCELADA or already FINALIZADA.
 */
public interface FinalizeElectionUseCase {
    void finalize(UUID electionId);
}
