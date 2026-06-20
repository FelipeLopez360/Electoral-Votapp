package co.com.votapp.ws.voting.infrastructure.adapter.out.persistence;

import co.com.votapp.ws.voting.domain.TokenStatus;
import co.com.votapp.ws.voting.domain.VotingToken;
import co.com.votapp.ws.voting.domain.port.out.VotingTokenRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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

    /**
     * Batch-insert ISSUED tokens with ON CONFLICT DO NOTHING on the partial unique index.
     *
     * <p>Each token is inserted using a native query. Tokens that already exist for a
     * (eleccion_id, funcionario_id) pair WHERE status='ISSUED' are silently skipped.
     * Returns ONLY the tokens that were actually inserted (insert count = 1);
     * tokens skipped due to conflict are excluded from the returned list.
     *
     * <p>Callers may use {@code result.size()} to obtain the precise inserted count,
     * which correctly reflects idempotent re-runs.
     */
    @Override
    @Transactional
    public List<VotingToken> saveAllIssued(List<VotingToken> tokens) {
        if (tokens.isEmpty()) {
            return List.of();
        }
        Instant now = Instant.now();
        List<VotingToken> inserted = new ArrayList<>();
        for (VotingToken token : tokens) {
            int rows = jpaRepository.insertIssuedOnConflictDoNothing(
                    token.id(),
                    token.eleccionId(),
                    token.funcionarioId().intValue(),
                    token.tokenHash(),
                    token.issuedAt(),
                    now
            );
            if (rows == 1) {
                inserted.add(token);
            }
        }
        return inserted;
    }

    @Override
    @Transactional
    public boolean markUsed(UUID tokenId, Instant usedAt, String usedIp, String userAgent) {
        int rows = jpaRepository.markUsed(tokenId, usedAt, usedIp, userAgent);
        return rows == 1;
    }

    // ─── Portal voting support ────────────────────────────────────────────────

    @Override
    public Optional<VotingToken> findIssuedByFuncionarioAndEleccion(Integer funcionarioId, UUID eleccionId) {
        return jpaRepository
                .findByFuncionarioIdAndEleccionIdAndStatus(funcionarioId, eleccionId, TokenStatus.ISSUED.name())
                .map(this::toDomain);
    }

    @Override
    public Optional<VotingToken> findById(UUID tokenId) {
        return jpaRepository.findById(tokenId).map(this::toDomain);
    }

    @Override
    public List<VotingToken> findAllByFuncionarioId(Integer funcionarioId) {
        return jpaRepository.findAllByFuncionarioId(funcionarioId)
                .stream()
                .map(this::toDomain)
                .toList();
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
