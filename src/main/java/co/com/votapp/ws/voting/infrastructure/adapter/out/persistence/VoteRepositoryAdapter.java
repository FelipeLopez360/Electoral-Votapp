package co.com.votapp.ws.voting.infrastructure.adapter.out.persistence;

import co.com.votapp.ws.voting.domain.Vote;
import co.com.votapp.ws.voting.domain.port.out.VoteRepositoryPort;
import org.springframework.stereotype.Component;

/**
 * Persistence adapter for anonymous vote storage.
 *
 * <p>Implements {@link VoteRepositoryPort}. Maps the {@link Vote} domain object
 * to {@link VoteEntity} and persists it via {@link VoteRepository}.
 * No funcionario_id is ever written — anonymity is guaranteed by design.
 */
@Component
public class VoteRepositoryAdapter implements VoteRepositoryPort {

    private final VoteRepository voteRepository;

    public VoteRepositoryAdapter(VoteRepository voteRepository) {
        this.voteRepository = voteRepository;
    }

    @Override
    public void save(Vote vote) {
        VoteEntity entity = toEntity(vote);
        voteRepository.save(entity);
    }

    // ─── Mapping ─────────────────────────────────────────────────────────────

    private VoteEntity toEntity(Vote vote) {
        VoteEntity entity = new VoteEntity();
        // id is null → DB generates UUID via @GeneratedValue(UUID)
        entity.setEleccionId(vote.getElectionId());
        entity.setCandidatoId(vote.getCandidateId());
        entity.setTokenId(vote.getTokenId());
        entity.setCreatedAt(vote.getCastAt());
        return entity;
    }
}
