package co.com.votapp.ws.electoral.domain.port.in;

import co.com.votapp.ws.electoral.domain.Ballot;

/**
 * Input port: retrieve the ballot for a given raw token.
 *
 * <p>Validates the token (hashes it, looks up status), then returns the ballot
 * with candidates ordered by numero_orden. Only valid for ISSUED tokens in ACTIVA elections.
 */
public interface GetBallotUseCase {
    Ballot getBallot(String rawToken);
}
