package co.com.votapp.ws.voting.domain.usecase;

import co.com.votapp.ws.audit.domain.AuditoriaEvento;
import co.com.votapp.ws.audit.domain.port.in.RegisterAuditEventPort;
import co.com.votapp.ws.common.exception.DomainException;
import co.com.votapp.ws.electoral.domain.ElectionStatus;
import co.com.votapp.ws.electoral.domain.port.out.CandidateRepositoryPort;
import co.com.votapp.ws.electoral.domain.port.out.ElectionRepositoryPort;
import co.com.votapp.ws.voting.domain.TokenStatus;
import co.com.votapp.ws.voting.domain.Vote;
import co.com.votapp.ws.voting.domain.VotingToken;
import co.com.votapp.ws.voting.domain.port.in.CastVoteByTokenIdPort;
import co.com.votapp.ws.voting.domain.port.out.ParticipacionRepositoryPort;
import co.com.votapp.ws.voting.domain.port.out.TokenLockPort;
import co.com.votapp.ws.voting.domain.port.out.VoteRepositoryPort;
import co.com.votapp.ws.voting.domain.port.out.VotingTokenRepository;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Use case implementation: cast an anonymous vote by token UUID (portal flow).
 *
 * <p>This is the portal-specific variant of {@link CastVoteUseCaseImpl}.
 * The ONLY difference is token resolution: by UUID instead of rawToken hash.
 * The full atomic flow is identical:
 * <ol>
 *   <li>Find token by tokenId</li>
 *   <li>Validate token is ISSUED and not expired</li>
 *   <li>Acquire Redis lock on tokenId (SETNX)</li>
 *   <li>[DB] Revalidate: election ACTIVA, candidate valid</li>
 *   <li>[DB] markUsed(tokenId)</li>
 *   <li>[DB] Insert anonymous vote (NO funcionario_id)</li>
 *   <li>[DB] Mark participation</li>
 *   <li>Register VOTE_ACCEPTED audit event (synchronous, no funcionario_id)</li>
 *   <li>Release Redis lock</li>
 * </ol>
 *
 * <p>Lock is ALWAYS released on any failure after acquisition.
 * No Spring annotations — wired manually via DomainConfig.
 */
public class CastVoteByTokenIdUseCaseImpl implements CastVoteByTokenIdPort {

    private final VotingTokenRepository votingTokenRepository;
    private final TokenLockPort tokenLockPort;
    private final ElectionRepositoryPort electionRepository;
    private final CandidateRepositoryPort candidateRepository;
    private final VoteRepositoryPort voteRepository;
    private final ParticipacionRepositoryPort participacionRepository;
    private final RegisterAuditEventPort auditPort;

    public CastVoteByTokenIdUseCaseImpl(VotingTokenRepository votingTokenRepository,
                                        TokenLockPort tokenLockPort,
                                        ElectionRepositoryPort electionRepository,
                                        CandidateRepositoryPort candidateRepository,
                                        VoteRepositoryPort voteRepository,
                                        ParticipacionRepositoryPort participacionRepository,
                                        RegisterAuditEventPort auditPort) {
        this.votingTokenRepository = votingTokenRepository;
        this.tokenLockPort = tokenLockPort;
        this.electionRepository = electionRepository;
        this.candidateRepository = candidateRepository;
        this.voteRepository = voteRepository;
        this.participacionRepository = participacionRepository;
        this.auditPort = auditPort;
    }

    @Override
    public void castVote(UUID tokenId, UUID candidatoId) {
        // Step 1: Find token by tokenId
        VotingToken token = votingTokenRepository.findById(tokenId)
                .orElseThrow(() -> new DomainException(
                        "Token not found: " + tokenId));

        // Step 2: Validate token is ISSUED
        if (token.status() != TokenStatus.ISSUED) {
            throw new DomainException(
                    "Token is not ISSUED. Current status: " + token.status());
        }

        UUID eleccionId = token.eleccionId();

        // Step 3: Acquire Redis lock BEFORE any DB work
        boolean locked = tokenLockPort.acquire(tokenId);
        if (!locked) {
            throw new DomainException("Token is currently being processed — concurrent use detected");
        }

        try {
            // Step 4a: Revalidate election is still ACTIVA
            var election = electionRepository.findById(eleccionId)
                    .orElseThrow(() -> new DomainException("Election not found: " + eleccionId));

            if (election.status() != ElectionStatus.ACTIVA) {
                throw new DomainException(
                        "Election is not ACTIVA. Current status: " + election.status());
            }

            // Step 4b: Revalidate candidate exists in this election
            candidateRepository.findByIdAndEleccionId(candidatoId, eleccionId)
                    .orElseThrow(() -> new DomainException(
                            "Candidate " + candidatoId + " not found in election " + eleccionId));

            // Step 5: Atomically mark token as used (returns false if already USED)
            boolean marked = votingTokenRepository.markUsed(tokenId, Instant.now(), null, null);
            if (!marked) {
                throw new DomainException("Token has already been used — concurrent vote detected");
            }

            // Step 6: Insert anonymous vote (NO funcionario_id)
            Vote vote = new Vote(tokenId, eleccionId, candidatoId, Instant.now());
            voteRepository.save(vote);

            // Step 7: Mark participation (funcionario_id stored here — not in votos)
            Integer funcionarioId = token.funcionarioId().intValue();
            participacionRepository.markParticipation(eleccionId, funcionarioId, Instant.now());

            // Step 8: Register audit event VOTE_ACCEPTED (no funcionario_id — anonymity preserved)
            AuditoriaEvento audit = new AuditoriaEvento(
                    "VOTE_ACCEPTED",
                    eleccionId,
                    null,   // funcionario_id intentionally omitted for anonymity
                    candidatoId,
                    Map.of("tokenId", tokenId.toString(), "source", "portal"),
                    Instant.now()
            );
            auditPort.register(audit);

        } finally {
            // Step 9: ALWAYS release lock — whether success or failure
            tokenLockPort.release(tokenId);
        }
    }
}
