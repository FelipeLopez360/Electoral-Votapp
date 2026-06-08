package co.com.votapp.ws.voting.domain.port.out;

import co.com.votapp.ws.voting.domain.VotingToken;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port for voting token persistence.
 *
 * <p>All query methods operate on hashed tokens — the raw secret is never passed here.
 */
public interface VotingTokenRepository {

    /**
     * Find a token by its SHA-256 hash. Returns the token only if it belongs
     * to the given election and its status is ISSUED.
     */
    Optional<VotingToken> findValidByHash(String tokenHash, UUID electionId);

    /**
     * Find any ISSUED token by its SHA-256 hash alone (without knowing the election).
     * Used by GetBallotUseCase where the election is derived from the token.
     */
    Optional<VotingToken> findIssuedByHash(String tokenHash);

    /**
     * Check if a funcionario already has an ISSUED token for the given election.
     * Used to enforce the one-token-per-funcionario-per-election invariant.
     */
    boolean existsIssuedTokenFor(UUID electionId, Long funcionarioId);

    /**
     * Persist a new ISSUED token.
     *
     * @return the saved token with its generated UUID id
     */
    VotingToken saveIssued(VotingToken token);

    /**
     * Atomically mark the token as USED within an existing DB transaction.
     *
     * @return true if exactly one row was updated; false if the token was already USED/INVALIDATED
     */
    boolean markUsed(UUID tokenId, Instant usedAt, String usedIp, String userAgent);
}
