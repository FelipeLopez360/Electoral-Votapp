package co.com.votapp.ws.voting.domain.usecase;

import co.com.votapp.ws.common.exception.DomainException;
import co.com.votapp.ws.electoral.domain.Election;
import co.com.votapp.ws.electoral.domain.ElectionStatus;
import co.com.votapp.ws.electoral.domain.port.out.ElectionRepositoryPort;
import co.com.votapp.ws.voting.application.command.IssueVotingTokenCommand;
import co.com.votapp.ws.voting.domain.IssuedVotingToken;
import co.com.votapp.ws.voting.domain.TokenStatus;
import co.com.votapp.ws.voting.domain.VotingToken;
import co.com.votapp.ws.voting.domain.port.in.IssueVotingTokenUseCase;
import co.com.votapp.ws.voting.domain.port.out.ParticipacionRepositoryPort;
import co.com.votapp.ws.voting.domain.port.out.VotingTokenRepository;
import co.com.votapp.ws.votereligibility.domain.port.out.VoterEligibilityRepositoryPort;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Use case implementation: issue a single-use voting token for an eligible funcionario.
 *
 * <p>Generates a cryptographically secure random rawToken (32 bytes, hex-encoded),
 * computes its SHA-256 hash, and persists the hash only.
 * Returns the ephemeral rawToken once — it is NEVER stored.
 * No Spring annotations — wired manually via DomainConfig.
 *
 * <p>Pre-conditions enforced (in order):
 * <ol>
 *   <li>Election must exist.</li>
 *   <li>Election must be ACTIVA.</li>
 *   <li>Funcionario must be eligible for this election (global AND census-aware).</li>
 *   <li>No existing ISSUED token for this funcionario+election pair.</li>
 *   <li>Funcionario has not already voted in this election.</li>
 * </ol>
 */
public class IssueVotingTokenUseCaseImpl implements IssueVotingTokenUseCase {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final VotingTokenRepository votingTokenRepository;
    private final VoterEligibilityRepositoryPort eligibilityRepository;
    private final ParticipacionRepositoryPort participacionRepository;
    private final ElectionRepositoryPort electionRepository;

    public IssueVotingTokenUseCaseImpl(VotingTokenRepository votingTokenRepository,
                                       VoterEligibilityRepositoryPort eligibilityRepository,
                                       ParticipacionRepositoryPort participacionRepository,
                                       ElectionRepositoryPort electionRepository) {
        this.votingTokenRepository = votingTokenRepository;
        this.eligibilityRepository = eligibilityRepository;
        this.participacionRepository = participacionRepository;
        this.electionRepository = electionRepository;
    }

    @Override
    public IssuedVotingToken issue(IssueVotingTokenCommand command) {
        // Guard 1: election must exist
        Election election = electionRepository.findById(command.eleccionId())
                .orElseThrow(() -> new DomainException(
                        "Elección no encontrada: " + command.eleccionId()));

        // Guard 2: election must be ACTIVA
        if (election.status() != ElectionStatus.ACTIVA) {
            throw new DomainException(
                    "La elección " + command.eleccionId() + " no está ACTIVA (estado actual: " + election.status() + ")");
        }

        // Guard 3: election-scoped eligibility (global + census-aware)
        if (!eligibilityRepository.isEligibleForElection(command.funcionarioId(), command.eleccionId())) {
            throw new DomainException(
                    "El funcionario " + command.funcionarioId() + " no está habilitado para votar en esta elección");
        }

        // Guard 4: no existing ISSUED token
        if (votingTokenRepository.existsIssuedTokenFor(command.eleccionId(), command.funcionarioId())) {
            throw new DomainException(
                    "El funcionario " + command.funcionarioId() +
                    " ya tiene un token emitido para esta elección");
        }

        // Guard 5: funcionario has not already voted
        if (participacionRepository.hasParticipated(command.eleccionId(), command.funcionarioId())) {
            throw new DomainException(
                    "Este funcionario ya votó en esta elección");
        }

        // Generate 32 cryptographically random bytes and hex-encode them
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        String rawToken = HexFormat.of().formatHex(randomBytes);

        String tokenHash = sha256Hex(rawToken);

        UUID tokenId = UUID.randomUUID();
        VotingToken token = new VotingToken(
                tokenId,
                command.eleccionId(),
                command.funcionarioId(),
                tokenHash,
                TokenStatus.ISSUED,
                Instant.now()
        );

        VotingToken saved = votingTokenRepository.saveIssued(token);

        return new IssuedVotingToken(rawToken, saved.id());
    }

    static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
