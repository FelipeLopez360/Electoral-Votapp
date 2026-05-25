package co.com.votapp.ws.voting.domain.port.out;

import java.util.UUID;

/**
 * Output port for distributed token locking (Redis SETNX pattern).
 *
 * <p>Used to prevent concurrent or repeated use of the same voting token.
 * Lock must be released explicitly on error paths (CastVote atomic flow).
 */
public interface TokenLockPort {

    /**
     * Attempt to acquire an exclusive lock for the given token.
     * Uses Redis SETNX semantics: returns true only if the key was absent.
     *
     * @param tokenId internal UUID of the token
     * @return true if lock acquired; false if already held
     */
    boolean acquire(UUID tokenId);

    /**
     * Release the lock for the given token.
     * Called on error after a successful acquire to allow retries.
     *
     * @param tokenId internal UUID of the token
     */
    void release(UUID tokenId);
}
