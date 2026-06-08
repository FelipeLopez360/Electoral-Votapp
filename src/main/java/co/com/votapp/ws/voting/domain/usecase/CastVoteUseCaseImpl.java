package co.com.votapp.ws.voting.domain.usecase;

import co.com.votapp.ws.common.exception.DomainException;
import co.com.votapp.ws.voting.application.command.CastVoteCommand;
import co.com.votapp.ws.voting.domain.port.in.CastVoteUseCase;
import co.com.votapp.ws.voting.domain.port.out.TokenLockPort;

import java.util.UUID;

/**
 * Use case implementation: cast an anonymous vote atomically.
 *
 * <p>PR 1 scope: Redis lock guard only. Full DB transaction (markUsed, participacion,
 * audit) will be added in PR 2.
 *
 * <p>Note: CastVoteCommand carries rawToken (not tokenId). PR 2 will resolve the
 * hash → tokenId lookup before acquiring the lock.
 * For now we use a deterministic UUID derived from rawToken as the lock key.
 */
public class CastVoteUseCaseImpl implements CastVoteUseCase {

    private final TokenLockPort tokenLockPort;

    public CastVoteUseCaseImpl(TokenLockPort tokenLockPort) {
        this.tokenLockPort = tokenLockPort;
    }

    @Override
    public void cast(CastVoteCommand command) {
        // PR 1: derive a stable UUID from the rawToken for locking purposes.
        // PR 2 will replace this with hash-based DB lookup → real tokenId.
        UUID lockKey = UUID.nameUUIDFromBytes(command.rawToken().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        boolean locked = tokenLockPort.acquire(lockKey);
        if (!locked) {
            throw new DomainException("Token already used");
        }
    }
}
