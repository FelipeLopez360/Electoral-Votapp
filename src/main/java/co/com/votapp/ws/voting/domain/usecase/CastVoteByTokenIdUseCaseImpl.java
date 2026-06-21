package co.com.votapp.ws.voting.domain.usecase;

import co.com.votapp.ws.audit.domain.AuditoriaEvento;
import co.com.votapp.ws.audit.domain.port.in.RegisterAuditEventPort;
import co.com.votapp.ws.common.exception.DomainException;
import co.com.votapp.ws.electoral.domain.Candidate;
import co.com.votapp.ws.electoral.domain.Election;
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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Use case implementation: cast one or more anonymous votes by token UUID (portal flow).
 *
 * <p>Multi-candidate contract:
 * <ol>
 *   <li>Find token by tokenId</li>
 *   <li>Validate token is ISSUED</li>
 *   <li>Acquire Redis lock on tokenId (SETNX)</li>
 *   <li>[DB] Revalidate: election ACTIVA</li>
 *   <li>[Domain] Deduplicate candidate IDs (preserve order, first occurrence wins)</li>
 *   <li>[Domain] Validate count ≤ maxVotosPorElector (early exit — before expensive DB lookups)</li>
 *   <li>[DB] Validate membership: each distinct candidate exists in this election</li>
 *   <li>[Domain] Enforce blank vote mutual exclusivity (blank cannot appear with others)</li>
 *   <li>[DB] markUsed(tokenId) — exactly once</li>
 *   <li>[DB] Insert one anonymous Vote per distinct candidate</li>
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
    public void castVote(UUID tokenId, List<UUID> candidatoIds) {
        // Step 0: Guard — reject null or empty candidate list before any I/O or state mutation.
        // An empty list would silently consume the token (markUsed) without inserting any vote
        // rows, making the token permanently burned with no audit trail. Reject early.
        if (candidatoIds == null || candidatoIds.isEmpty()) {
            throw new DomainException(
                    "candidatoIds must contain at least one candidate ID");
        }

        // Step 1: Find token by tokenId
        VotingToken token = votingTokenRepository.findById(tokenId)
                .orElseThrow(() -> new DomainException("Token not found: " + tokenId));

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
            // Step 4: Revalidate election is still ACTIVA
            Election election = electionRepository.findById(eleccionId)
                    .orElseThrow(() -> new DomainException("Election not found: " + eleccionId));

            if (election.status() != ElectionStatus.ACTIVA) {
                throw new DomainException(
                        "Election is not ACTIVA. Current status: " + election.status());
            }

            // Step 5: Deduplicate candidate IDs (stable order, first occurrence wins)
            List<UUID> distinctIds = new ArrayList<>(new LinkedHashSet<>(candidatoIds));

            // Step 6: Validate selection count against maxVotosPorElector (cheap — before DB lookups)
            if (distinctIds.size() > election.maxVotosPorElector()) {
                throw new DomainException(
                        "Number of selected candidates (" + distinctIds.size() + ") exceeds maxVotosPorElector ("
                                + election.maxVotosPorElector() + ")");
            }

            // Step 7: Resolve and validate each candidate belongs to this election
            List<Candidate> resolvedCandidates = new ArrayList<>(distinctIds.size());
            for (UUID candidatoId : distinctIds) {
                Candidate candidate = candidateRepository.findByIdAndEleccionId(candidatoId, eleccionId)
                        .orElseThrow(() -> new DomainException(
                                "Candidate " + candidatoId + " not found in election " + eleccionId));
                resolvedCandidates.add(candidate);
            }

            // Step 8: Enforce blank vote mutual exclusivity
            boolean hasBlankVote = resolvedCandidates.stream().anyMatch(Candidate::esVotoEnBlanco);
            if (hasBlankVote && resolvedCandidates.size() > 1) {
                throw new DomainException(
                        "blank vote candidate cannot be combined with other candidates");
            }

            // Step 9: Atomically mark token as used (exactly once for multi-vote)
            boolean marked = votingTokenRepository.markUsed(tokenId, Instant.now(), null, null);
            if (!marked) {
                throw new DomainException("Token has already been used — concurrent vote detected");
            }

            // Step 10: Insert one anonymous vote row per distinct candidate
            Instant castAt = Instant.now();
            for (UUID candidatoId : distinctIds) {
                voteRepository.save(new Vote(tokenId, eleccionId, candidatoId, castAt));
            }

            // Step 11: Mark participation (funcionario_id stored here — not in votos)
            Integer funcionarioId = token.funcionarioId().intValue();
            participacionRepository.markParticipation(eleccionId, funcionarioId, Instant.now());

            // Step 12: Register audit event VOTE_ACCEPTED (no funcionario_id — anonymity preserved)
            AuditoriaEvento audit = new AuditoriaEvento(
                    "VOTE_ACCEPTED",
                    eleccionId,
                    null,   // funcionario_id intentionally omitted for anonymity
                    distinctIds.get(0),   // primary candidato for audit (first in deduplicated list)
                    Map.of("tokenId", tokenId.toString(), "source", "portal",
                            "candidateCount", String.valueOf(distinctIds.size())),
                    Instant.now()
            );
            auditPort.register(audit);

        } finally {
            // Step 13: ALWAYS release lock — whether success or failure
            tokenLockPort.release(tokenId);
        }
    }
}
