package co.com.votapp.ws.voting.domain;

import java.time.Instant;

public class Vote {
    private final String tokenId;
    private final Long electionId;
    private final Long candidateId;
    private final Long categoryId;
    private final Instant castAt;

    public Vote(String tokenId, Long electionId, Long candidateId, Long categoryId, Instant castAt) {
        if (tokenId == null || tokenId.isBlank()) {
            throw new IllegalArgumentException("Token id must not be blank");
        }
        if (electionId == null) {
            throw new IllegalArgumentException("Election id must not be null");
        }
        if (candidateId == null) {
            throw new IllegalArgumentException("Candidate id must not be null");
        }
        if (categoryId == null) {
            throw new IllegalArgumentException("Category id must not be null");
        }
        if (castAt == null) {
            throw new IllegalArgumentException("Cast time must not be null");
        }
        this.tokenId = tokenId;
        this.electionId = electionId;
        this.candidateId = candidateId;
        this.categoryId = categoryId;
        this.castAt = castAt;
    }

    public String getTokenId() {
        return tokenId;
    }

    public Long getElectionId() {
        return electionId;
    }

    public Long getCandidateId() {
        return candidateId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public Instant getCastAt() {
        return castAt;
    }
}
