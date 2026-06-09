package co.com.votapp.ws.voting.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link VotingTokenEntity}.
 *
 * <p>Provides derived and JPQL query methods needed by {@link VotingTokenRepositoryAdapter}.
 */
public interface VotingTokenJpaRepository extends JpaRepository<VotingTokenEntity, UUID> {

    /**
     * Find an ISSUED token by its hash within a specific election.
     * Used by CastVoteUseCase to validate a token before casting.
     */
    Optional<VotingTokenEntity> findByTokenHashAndEleccionIdAndStatus(
            String tokenHash, UUID eleccionId, String status);

    /**
     * Find any ISSUED token by its hash alone (election unknown at call time).
     * Used by GetBallotUseCase to derive the election from the token.
     */
    Optional<VotingTokenEntity> findByTokenHashAndStatus(String tokenHash, String status);

    /**
     * Check whether a funcionario already holds an ISSUED token for the given election.
     * Used to enforce the one-token-per-funcionario-per-election invariant.
     */
    boolean existsByEleccionIdAndFuncionarioIdAndStatus(
            UUID eleccionId, Integer funcionarioId, String status);

    /**
     * Atomically marks a token as USED, but only when its current status is 'ISSUED'.
     *
     * @return number of rows updated (1 on success, 0 if already USED/INVALIDATED)
     */
    @Modifying
    @Query("""
            UPDATE VotingTokenEntity t
            SET t.status = 'USED', t.usedAt = :usedAt, t.usedIp = :usedIp, t.userAgent = :userAgent
            WHERE t.id = :tokenId AND t.status = 'ISSUED'
            """)
    int markUsed(@Param("tokenId") UUID tokenId,
                 @Param("usedAt") Instant usedAt,
                 @Param("usedIp") String usedIp,
                 @Param("userAgent") String userAgent);

    // ─── Portal voting support ─────────────────────────────────────────────────

    /**
     * Find the single ISSUED token for a funcionario in a specific election.
     * Portal can resolve the tokenId without knowing the rawToken.
     */
    Optional<VotingTokenEntity> findByFuncionarioIdAndEleccionIdAndStatus(
            Integer funcionarioId, UUID eleccionId, String status);
}
