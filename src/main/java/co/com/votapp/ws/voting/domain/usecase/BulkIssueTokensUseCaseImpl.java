package co.com.votapp.ws.voting.domain.usecase;

import co.com.votapp.ws.auth.domain.Funcionario;
import co.com.votapp.ws.auth.domain.port.out.FuncionarioRepositoryPort;
import co.com.votapp.ws.electoral.domain.port.out.CensoRepositoryPort;
import co.com.votapp.ws.voting.domain.TokenStatus;
import co.com.votapp.ws.voting.domain.VotingToken;
import co.com.votapp.ws.voting.domain.port.in.BulkIssueTokensUseCase;
import co.com.votapp.ws.voting.domain.port.out.VotingTokenRepository;
import co.com.votapp.ws.votereligibility.domain.port.out.VoterEligibilityRepositoryPort;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of {@link BulkIssueTokensUseCase}.
 *
 * <p>Issues ISSUED-status voting tokens in bulk for all eligible funcionarios when an
 * election is activated. rawToken is generated, hashed (SHA-256), and immediately discarded.
 *
 * <p>ZERO Spring annotations — wired manually via {@code DomainConfig}.
 *
 * <h3>Algorithm</h3>
 * <ol>
 *   <li>Read census for the election. If empty, fall back to ALL globally eligible funcionarios.</li>
 *   <li>For each candidate: skip if ineligible or already has ISSUED token.</li>
 *   <li>For remaining: generate rawToken → SHA-256 → discard rawToken.</li>
 *   <li>Batch-save via {@code saveAllIssued} (ON CONFLICT DO NOTHING for idempotency).</li>
 * </ol>
 *
 * <h3>Integer → Long boundary</h3>
 * <p>Census and Funcionario use {@code Integer} ids (matching the DB column).
 * {@code VotingToken.funcionarioId}, {@code existsIssuedTokenFor}, and
 * {@code isEligibleForElection} all use {@code Long}. Conversion happens here at the
 * use-case boundary — persisted domain types are NOT changed.
 */
public class BulkIssueTokensUseCaseImpl implements BulkIssueTokensUseCase {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final CensoRepositoryPort censoRepository;
    private final VotingTokenRepository votingTokenRepository;
    private final VoterEligibilityRepositoryPort eligibilityRepository;
    private final FuncionarioRepositoryPort funcionarioRepository;

    public BulkIssueTokensUseCaseImpl(CensoRepositoryPort censoRepository,
                                       VotingTokenRepository votingTokenRepository,
                                       VoterEligibilityRepositoryPort eligibilityRepository,
                                       FuncionarioRepositoryPort funcionarioRepository) {
        this.censoRepository = censoRepository;
        this.votingTokenRepository = votingTokenRepository;
        this.eligibilityRepository = eligibilityRepository;
        this.funcionarioRepository = funcionarioRepository;
    }

    @Override
    public BulkIssueResult issueForElection(UUID eleccionId) {
        // Step 1: resolve candidate list
        List<Integer> candidateIds = censoRepository.findAllFuncionarioIdsByEleccionId(eleccionId);

        if (candidateIds.isEmpty()) {
            // Fallback: all globally eligible funcionarios (ACTIVO + puede_votar=true)
            List<Funcionario> globalEligible =
                    funcionarioRepository.findEligibleByFilters(null, "ACTIVO", true);
            candidateIds = globalEligible.stream()
                    .map(Funcionario::getId)
                    .toList();
        }

        int total = candidateIds.size();
        if (total == 0) {
            return new BulkIssueResult(0, 0, 0);
        }

        // Step 2: filter and build tokens
        List<VotingToken> tokensToSave = new ArrayList<>();
        int skipped = 0;

        for (Integer funcionarioId : candidateIds) {
            Long funcionarioIdLong = funcionarioId.longValue();

            // Skip if globally or census-scoped ineligible
            if (!eligibilityRepository.isEligibleForElection(funcionarioIdLong, eleccionId)) {
                skipped++;
                continue;
            }

            // Skip if already has ISSUED token (idempotency pre-check)
            if (votingTokenRepository.existsIssuedTokenFor(eleccionId, funcionarioIdLong)) {
                skipped++;
                continue;
            }

            // Step 3: generate rawToken, hash, discard rawToken
            byte[] randomBytes = new byte[32];
            SECURE_RANDOM.nextBytes(randomBytes);
            String rawToken = HexFormat.of().formatHex(randomBytes);
            String tokenHash = sha256Hex(rawToken);
            // rawToken is now eligible for GC — never stored or returned

            VotingToken token = new VotingToken(
                    UUID.randomUUID(),
                    eleccionId,
                    funcionarioIdLong,
                    tokenHash,
                    TokenStatus.ISSUED,
                    Instant.now()
            );
            tokensToSave.add(token);
        }

        // Step 4: batch-save with ON CONFLICT DO NOTHING.
        // saveAllIssued returns ONLY tokens that were actually inserted (skipping DB-level conflicts).
        // Use the returned list size as the authoritative issued count so retries report 0, not N.
        int issued = 0;
        if (!tokensToSave.isEmpty()) {
            List<VotingToken> actuallyInserted = votingTokenRepository.saveAllIssued(tokensToSave);
            issued = actuallyInserted.size();
            // Any tokens in tokensToSave but not in actuallyInserted were DB-level duplicates;
            // they are added to skipped so the total always holds: issued + skipped == total
            skipped += (tokensToSave.size() - issued);
        }

        return new BulkIssueResult(issued, skipped, total);
    }

    /**
     * Compute SHA-256 hex digest of a UTF-8 string.
     * Mirrors {@code IssueVotingTokenUseCaseImpl.sha256Hex}.
     */
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
