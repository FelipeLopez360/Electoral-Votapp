package co.com.votapp.ws.electoral.domain.port.in;

import java.util.UUID;

/**
 * Input port: transition an election from PROGRAMADA to ACTIVA.
 *
 * <p>Side effect: creates the synthetic "Voto en Blanco" candidate automatically.
 */
public interface ActivateElectionUseCase {
    void activate(UUID electionId);
}
