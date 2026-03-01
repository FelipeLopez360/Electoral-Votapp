package co.com.votapp.ws.voting.application.usecase;

import co.com.votapp.ws.common.annotation.UseCase;
import co.com.votapp.ws.common.exception.DomainException;
import co.com.votapp.ws.voting.application.port.out.TokenLockPort;
import co.com.votapp.ws.voting.domain.Vote;

@UseCase
public class CastVoteUseCase {
    private final TokenLockPort tokenLockPort;

    public CastVoteUseCase(TokenLockPort tokenLockPort) {
        this.tokenLockPort = tokenLockPort;
    }

    public void cast(Vote vote) {
        boolean locked = tokenLockPort.acquireTokenLock(vote.getTokenId());
        if (!locked) {
            throw new DomainException("Token already used");
        }
    }
}
