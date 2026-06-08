package co.com.votapp.ws.votereligibility.infrastructure.adapter.out.persistence;

import co.com.votapp.ws.auth.infrastructure.adapter.out.persistence.FuncionarioJpaRepository;
import co.com.votapp.ws.votereligibility.domain.port.out.VoterEligibilityRepositoryPort;
import org.springframework.stereotype.Component;

/**
 * Persistence adapter for voter eligibility checks.
 *
 * <p>Implements {@link VoterEligibilityRepositoryPort}. Reuses {@link FuncionarioJpaRepository}
 * from the auth infrastructure context — both map to the {@code funcionarios} table.
 *
 * <p>A funcionario is eligible when:
 * <ul>
 *   <li>{@code estado_laboral = 'ACTIVO'}</li>
 *   <li>{@code puede_votar = true}</li>
 * </ul>
 */
@Component
public class VoterEligibilityRepositoryAdapter implements VoterEligibilityRepositoryPort {

    private final FuncionarioJpaRepository funcionarioJpaRepository;

    public VoterEligibilityRepositoryAdapter(FuncionarioJpaRepository funcionarioJpaRepository) {
        this.funcionarioJpaRepository = funcionarioJpaRepository;
    }

    @Override
    public boolean isEligible(Long funcionarioId) {
        return funcionarioJpaRepository.findById(funcionarioId.intValue())
                .map(f -> "ACTIVO".equals(f.getEstadoLaboral()) && Boolean.TRUE.equals(f.getPuedeVotar()))
                .orElse(false);
    }
}
