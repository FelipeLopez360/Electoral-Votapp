package co.com.votapp.ws.voting.domain.usecase;

import co.com.votapp.ws.common.exception.DomainException;
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
 */
public class IssueVotingTokenUseCaseImpl implements IssueVotingTokenUseCase {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final VotingTokenRepository votingTokenRepository;
    private final VoterEligibilityRepositoryPort eligibilityRepository;
    private final ParticipacionRepositoryPort participacionRepository;

    public IssueVotingTokenUseCaseImpl(VotingTokenRepository votingTokenRepository,
                                       VoterEligibilityRepositoryPort eligibilityRepository,
                                       ParticipacionRepositoryPort participacionRepository) {
        this.votingTokenRepository = votingTokenRepository;
        this.eligibilityRepository = eligibilityRepository;
        this.participacionRepository = participacionRepository;
    }

    @Override
    public IssuedVotingToken issue(IssueVotingTokenCommand command) {
        if (!eligibilityRepository.isEligible(command.funcionarioId())) {
            throw new DomainException(
                    "El funcionario " + command.funcionarioId() + " no está habilitado para votar");
        }

        if (votingTokenRepository.existsIssuedTokenFor(command.eleccionId(), command.funcionarioId())) {
            throw new DomainException(
                    "El funcionario " + command.funcionarioId() +
                    " ya tiene un token emitido para esta elección");
        }

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
