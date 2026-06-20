package co.com.votapp.ws.votereligibility.infrastructure.adapter.out.persistence;

import co.com.votapp.ws.auth.infrastructure.adapter.out.persistence.FuncionarioJpaRepository;
import co.com.votapp.ws.electoral.infrastructure.adapter.out.persistence.CensoJpaRepository;
import co.com.votapp.ws.votereligibility.domain.port.out.VoterEligibilityRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Persistence adapter for voter eligibility checks.
 *
 * <p>Implements {@link VoterEligibilityRepositoryPort}. Reuses {@link FuncionarioJpaRepository}
 * from the auth infrastructure context — both map to the {@code funcionarios} table.
 * Also reuses {@link CensoJpaRepository} from the electoral infrastructure context —
 * this cross-context adapter read is an established pattern.
 *
 * <p>Election-scoped eligibility requires:
 * <ul>
 *   <li>Funcionario is globally eligible (ACTIVO + puede_votar=true).</li>
 *   <li>Census is empty for the election (backward-compat fallback), OR</li>
 *   <li>Funcionario has an explicit census entry for the election.</li>
 * </ul>
 */
@Component
public class VoterEligibilityRepositoryAdapter implements VoterEligibilityRepositoryPort {

    private final FuncionarioJpaRepository funcionarioJpaRepository;
    private final CensoJpaRepository censoJpaRepository;

    public VoterEligibilityRepositoryAdapter(FuncionarioJpaRepository funcionarioJpaRepository,
                                              CensoJpaRepository censoJpaRepository) {
        this.funcionarioJpaRepository = funcionarioJpaRepository;
        this.censoJpaRepository = censoJpaRepository;
    }

    /**
     * Election-scoped eligibility check.
     *
     * <p>Logic:
     * <ol>
     *   <li>Check global eligibility (ACTIVO + puede_votar=true). If false → reject.</li>
     *   <li>If the census for this election is EMPTY → allow (backward-compat fallback).</li>
     *   <li>Otherwise → allow only if the funcionario has an explicit census entry.</li>
     * </ol>
     */
    @Override
    public boolean isEligibleForElection(Long funcionarioId, UUID eleccionId) {
        if (!isGloballyEligible(funcionarioId)) {
            return false;
        }

        long censoCount = censoJpaRepository.countByEleccionId(eleccionId);
        if (censoCount == 0) {
            // Census is empty → backward-compat fallback: global eligibility is sufficient
            return true;
        }

        return censoJpaRepository.existsByEleccionIdAndFuncionarioId(eleccionId, funcionarioId.intValue());
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private boolean isGloballyEligible(Long funcionarioId) {
        return funcionarioJpaRepository.findById(funcionarioId.intValue())
                .map(f -> "ACTIVO".equals(f.getEstadoLaboral()) && Boolean.TRUE.equals(f.getPuedeVotar()))
                .orElse(false);
    }
}
