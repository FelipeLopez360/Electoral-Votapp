package co.com.votapp.ws.voting.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain object representing an anonymous cast vote.
 *
 * <p>MVP single-category model: no categoryId — each election has one ballot.
 * tokenId is a UUID (internal identifier); the raw token secret is never persisted.
 */
public class Vote {

    private final UUID tokenId;
    private final UUID electionId;
    private final UUID candidateId;
    private final Instant castAt;

    public Vote(UUID tokenId, UUID electionId, UUID candidateId, Instant castAt) {
        if (tokenId == null) {
            throw new IllegalArgumentException("Token id must not be null");
        }
        if (electionId == null) {
            throw new IllegalArgumentException("Election id must not be null");
        }
        if (candidateId == null) {
            throw new IllegalArgumentException("Candidate id must not be null");
        }
        if (castAt == null) {
            throw new IllegalArgumentException("Cast time must not be null");
        }
        this.tokenId = tokenId;
        this.electionId = electionId;
        this.candidateId = candidateId;
        this.castAt = castAt;
    }

    public UUID getTokenId() {
        return tokenId;
    }

    public UUID getElectionId() {
        return electionId;
    }

    public UUID getCandidateId() {
        return candidateId;
    }

    public Instant getCastAt() {
        return castAt;
    }
}
