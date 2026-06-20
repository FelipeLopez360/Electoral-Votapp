package co.com.votapp.ws.votereligibility.domain.port.out;

import java.util.UUID;

/**
 * Output port for voter eligibility checks.
 *
 * <p>Election-scoped eligibility checks that the funcionario:
 * <ul>
 *   <li>Has {@code estado_laboral = 'ACTIVO'} and {@code puede_votar = true}</li>
 *   <li>Is included in the election's census (if non-empty), or the census is empty
 *       (backward-compat fallback)</li>
 * </ul>
 */
public interface VoterEligibilityRepositoryPort {

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
