package co.com.votapp.ws.voting.domain.port.in;

import co.com.votapp.ws.voting.application.command.CastVoteCommand;

/**
 * Input port: cast an anonymous vote atomically.
 *
 * <p>Atomic flow:
 * 1. Redis SETNX to prevent concurrent use of the same token
 * 2. DB transaction: revalidate token, markUsed, insert voto, update participacion, write audit
 * 3. Release Redis lock on error (release port)
 */
public interface CastVoteUseCase {
    void cast(CastVoteCommand command);
}
