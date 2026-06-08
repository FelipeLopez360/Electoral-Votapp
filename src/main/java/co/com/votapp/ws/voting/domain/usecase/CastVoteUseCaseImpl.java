package co.com.votapp.ws.voting.domain.usecase;

import co.com.votapp.ws.audit.domain.AuditoriaEvento;
import co.com.votapp.ws.audit.domain.port.in.RegisterAuditEventPort;
import co.com.votapp.ws.common.exception.DomainException;
import co.com.votapp.ws.electoral.domain.ElectionStatus;
import co.com.votapp.ws.electoral.domain.port.out.CandidateRepositoryPort;
import co.com.votapp.ws.electoral.domain.port.out.ElectionRepositoryPort;
import co.com.votapp.ws.voting.application.command.CastVoteCommand;
import co.com.votapp.ws.voting.domain.Vote;
import co.com.votapp.ws.voting.domain.VotingToken;
import co.com.votapp.ws.voting.domain.port.in.CastVoteUseCase;
import co.com.votapp.ws.voting.domain.port.out.ParticipacionRepositoryPort;
import co.com.votapp.ws.voting.domain.port.out.TokenLockPort;
import co.com.votapp.ws.voting.domain.port.out.VoteRepositoryPort;
import co.com.votapp.ws.voting.domain.port.out.VotingTokenRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

/**
 * Use case implementation: cast an anonymous vote atomically.
 *
 * <p>Full atomic flow per PR 2 spec:
 * <ol>
 *   <li>Hash rawToken → SHA-256</li>
 *   <li>Find valid ISSUED token by hash</li>
 *   <li>Acquire Redis lock on tokenId (SETNX)</li>
 *   <li>[DB] Revalidate: election ACTIVA, candidate valid</li>
 *   <li>[DB] markUsed(tokenId)</li>
 *   <li>[DB] Insert anonymous vote</li>
 *   <li>[DB] Mark participation (funcionario voted, no candidate reference)</li>
 *   <li>Register VOTE_ACCEPTED audit event (synchronous, no funcionario_id)</li>
 *   <li>Release Redis lock</li>
 * </ol>
 *
 * <p>Lock is ALWAYS released on any failure after acquisition.
 * No Spring annotations — wired manually via DomainConfig.
 */
public class CastVoteUseCaseImpl implements CastVoteUseCase {

    private final VotingTokenRepository votingTokenRepository;
    private final TokenLockPort tokenLockPort;
    private final ElectionRepositoryPort electionRepository;
    private final CandidateRepositoryPort candidateRepository;
    private final VoteRepositoryPort voteRepository;
    private final ParticipacionRepositoryPort participacionRepository;
    private final RegisterAuditEventPort auditPort;

    public CastVoteUseCaseImpl(VotingTokenRepository votingTokenRepository,
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
    public void cast(CastVoteCommand command) {
        // Step 1: Hash rawToken
        String tokenHash = sha256Hex(command.rawToken());

        // Step 2: Find ISSUED token by hash (election is derived from token record)
        VotingToken token = votingTokenRepository.findIssuedByHash(tokenHash)
                .orElseThrow(() -> new DomainException(
                        "Token is invalid or has already been used"));

        UUID tokenId = token.id();
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
            candidateRepository.findByIdAndEleccionId(command.candidatoId(), eleccionId)
                    .orElseThrow(() -> new DomainException(
                            "Candidate " + command.candidatoId() + " not found in election " + eleccionId));

            // Step 5: Atomically mark token as used (returns false if already USED)
            boolean marked = votingTokenRepository.markUsed(tokenId, Instant.now(), null, null);
            if (!marked) {
                throw new DomainException("Token has already been used — concurrent vote detected");
            }

            // Step 6: Insert anonymous vote (NO funcionario_id)
            Vote vote = new Vote(tokenId, eleccionId, command.candidatoId(), Instant.now());
            voteRepository.save(vote);

            // Step 7: Mark participation (funcionario_id is stored here, NOT in votos)
            Integer funcionarioId = token.funcionarioId().intValue();
            participacionRepository.markParticipation(eleccionId, funcionarioId, Instant.now());

            // Step 8: Register audit event VOTE_ACCEPTED (no funcionario_id — anonymity preserved)
            AuditoriaEvento audit = new AuditoriaEvento(
                    "VOTE_ACCEPTED",
                    eleccionId,
                    null,   // funcionario_id intentionally omitted for anonymity
                    command.candidatoId(),
                    Map.of("tokenId", tokenId.toString()),
                    Instant.now()
            );
            auditPort.register(audit);

        } finally {
            // Step 9: ALWAYS release lock — whether success or failure
            tokenLockPort.release(tokenId);
        }
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
