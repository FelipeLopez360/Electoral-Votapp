package co.com.votapp.ws.voting.domain.port.in;

import java.util.UUID;

/**
 * Input port for bulk voting token issuance.
 *
 * <p>Triggered during election activation. Issues ISSUED-status tokens for all
 * census members (or all globally eligible funcionarios when census is empty).
 * Raw tokens are generated per funcionario and immediately discarded after hashing —
 * they are NEVER returned, stored, or logged.
 *
 * <p>Implementations must be pure Java — ZERO Spring or JPA imports.
 * Wired manually via {@code DomainConfig}.
 */
public interface BulkIssueTokensUseCase {

    /**
     * Issue voting tokens in bulk for all eligible funcionarios in the given election.
     *
     * <p>Algorithm:
     * <ol>
     *   <li>Read census for the election via {@code CensoRepositoryPort.findAllFuncionarioIdsByEleccionId}.</li>
     *   <li>If census is empty, fall back to ALL globally eligible funcionarios
     *       ({@code FuncionarioRepositoryPort.findEligibleByFilters(null, "ACTIVO", true)}).</li>
     *   <li>For each candidate: skip if ineligible ({@code isEligibleForElection}) or already has ISSUED token
     *       ({@code existsIssuedTokenFor}).</li>
     *   <li>For remaining candidates: generate rawToken (32 random bytes, hex), hash → SHA-256, discard rawToken.</li>
     *   <li>Batch-persist via {@code VotingTokenRepository.saveAllIssued} (ON CONFLICT DO NOTHING).</li>
     * </ol>
     *
     * @param eleccionId the election UUID for which to issue tokens
     * @return summary with issued count, skipped count, and total census size
     */
    BulkIssueResult issueForElection(UUID eleccionId);

    /**
     * Immutable result of a bulk issuance operation.
     *
     * @param issued  number of new tokens persisted
     * @param skipped number of funcionarios skipped (ineligible or already had token)
     * @param total   total number of candidates considered (census size or global fallback size)
     */
    record BulkIssueResult(int issued, int skipped, int total) {}
}
