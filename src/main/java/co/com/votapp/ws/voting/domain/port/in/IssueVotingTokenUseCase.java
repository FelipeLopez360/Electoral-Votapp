package co.com.votapp.ws.voting.domain.port.in;

import co.com.votapp.ws.voting.application.command.IssueVotingTokenCommand;
import co.com.votapp.ws.voting.domain.IssuedVotingToken;

/**
 * Input port: issue a single-use voting token.
 *
 * <p>Returns the ephemeral rawToken (returned ONCE to the caller) and the internal tokenId.
 * The rawToken is never stored; only its SHA-256 hash is persisted.
 */
public interface IssueVotingTokenUseCase {
    IssuedVotingToken issue(IssueVotingTokenCommand command);
}
