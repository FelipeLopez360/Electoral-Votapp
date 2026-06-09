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

    // ─── Portal voting support ────────────────────────────────────────────────

    /**
     * Find the single ISSUED token for a funcionario in a specific election.
     * Used by the portal to resolve the token ID without requiring the rawToken.
     *
     * <p>Only returns tokens in ISSUED status (partial unique index guarantees at most one).
     *
     * @param funcionarioId the funcionario's integer DB id
     * @param eleccionId    the election UUID
     * @return the ISSUED token, or empty if none exists
     */
    Optional<VotingToken> findIssuedByFuncionarioAndEleccion(Integer funcionarioId, UUID eleccionId);

    /**
     * Find a token by its internal UUID.
     * Used by {@code CastVoteByTokenIdUseCase} where the token is resolved by ID,
     * not by rawToken hash.
     *
     * @param tokenId the internal UUID of the token
     * @return the token if found, or empty
     */
    Optional<VotingToken> findById(UUID tokenId);
}
