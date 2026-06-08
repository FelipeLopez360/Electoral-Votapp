package co.com.votapp.ws.votereligibility.domain.port.out;

/**
 * Output port for voter eligibility checks.
 *
 * <p>A funcionario is eligible if and only if:
 * - estado_laboral = 'ACTIVO'
 * - puede_votar = true
 */
public interface VoterEligibilityRepositoryPort {
    boolean isEligible(Long funcionarioId);
}
