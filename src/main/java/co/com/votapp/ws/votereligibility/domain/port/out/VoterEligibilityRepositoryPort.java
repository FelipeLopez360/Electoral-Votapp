package co.com.votapp.ws.votereligibility.domain.port.out;

import java.util.UUID;

/**
 * Output port for voter eligibility checks.
 *
 * <p>A funcionario is globally eligible if and only if:
 * <ul>
 *   <li>{@code estado_laboral = 'ACTIVO'}</li>
 *   <li>{@code puede_votar = true}</li>
 * </ul>
 *
 * <p>Election-scoped eligibility additionally checks that the funcionario
 * is included in the election's census (if the census is non-empty).
 * When the census is empty, the check falls back to global eligibility only
 * to preserve backward compatibility.
 */
public interface VoterEligibilityRepositoryPort {

    /**
     * Global eligibility check (backward-compatible).
     *
     * @param funcionarioId the funcionario's primary key
     * @return {@code true} if the funcionario is ACTIVO and puede_votar=true
     */
    boolean isEligible(Long funcionarioId);

    /**
     * Election-scoped eligibility check.
     *
     * <p>Returns {@code true} when ALL of the following hold:
     * <ol>
     *   <li>The funcionario is globally eligible (ACTIVO + puede_votar=true).</li>
     *   <li>Either the census for this election is empty (backward-compat fallback),
     *       OR the funcionario has an explicit entry in the census.</li>
     * </ol>
     *
     * @param funcionarioId the funcionario's primary key
     * @param eleccionId    the election's UUID
     * @return {@code true} if the funcionario may receive a token for this election
     */
    boolean isEligibleForElection(Long funcionarioId, UUID eleccionId);
}
