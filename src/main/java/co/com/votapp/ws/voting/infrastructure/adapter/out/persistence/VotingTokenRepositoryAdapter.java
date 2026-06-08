package co.com.votapp.ws.voting.infrastructure.adapter.out.persistence;

import co.com.votapp.ws.voting.domain.TokenStatus;
import co.com.votapp.ws.voting.domain.VotingToken;
import co.com.votapp.ws.voting.domain.port.out.VotingTokenRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence adapter for voting token lifecycle management.
 *
 * <p>Implements {@link VotingTokenRepository}. All token operations go through the hash —
 * the raw token secret is NEVER passed to or returned from this adapter.
 */
@Component
public class VotingTokenRepositoryAdapter implements VotingTokenRepository {

    private final VotingTokenJpaRepository jpaRepository;

    public VotingTokenRepositoryAdapter(VotingTokenJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<VotingToken> findValidByHash(String tokenHash, UUID electionId) {
        return jpaRepository
                .findByTokenHashAndEleccionIdAndStatus(tokenHash, electionId, TokenStatus.ISSUED.name())
                .map(this::toDomain);
    }

    @Override
    public Optional<VotingToken> findIssuedByHash(String tokenHash) {
        return jpaRepository
                .findByTokenHashAndStatus(tokenHash, TokenStatus.ISSUED.name())
                .map(this::toDomain);
    }

    @Override
    public boolean existsIssuedTokenFor(UUID electionId, Long funcionarioId) {
        return jpaRepository.existsByEleccionIdAndFuncionarioIdAndStatus(
                electionId, funcionarioId.intValue(), TokenStatus.ISSUED.name());
    }

    @Override
    public VotingToken saveIssued(VotingToken token) {
        VotingTokenEntity entity = toEntity(token);
        VotingTokenEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    @Transactional
    public boolean markUsed(UUID tokenId, Instant usedAt, String usedIp, String userAgent) {
        int rows = jpaRepository.markUsed(tokenId, usedAt, usedIp, userAgent);
        return rows == 1;
    }

    // ─── Mapping ─────────────────────────────────────────────────────────────

    private VotingToken toDomain(VotingTokenEntity entity) {
        return new VotingToken(
                entity.getId(),
                entity.getEleccionId(),
                Long.valueOf(entity.getFuncionarioId()),
                entity.getTokenHash(),
                TokenStatus.valueOf(entity.getStatus()),
                entity.getIssuedAt()
        );
    }

    private VotingTokenEntity toEntity(VotingToken token) {
        VotingTokenEntity entity = new VotingTokenEntity();
        // NOT setting ID — Persistable.isNew() stays true, JPA calls persist() not merge()
        entity.setEleccionId(token.eleccionId());
        entity.setFuncionarioId(token.funcionarioId().intValue());
        entity.setTokenHash(token.tokenHash());
        entity.setStatus(token.status().name());
        entity.setIssuedAt(token.issuedAt());
        entity.setCreatedAt(Instant.now());
        return entity;
    }
}
