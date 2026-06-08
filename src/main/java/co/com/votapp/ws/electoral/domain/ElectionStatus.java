package co.com.votapp.ws.electoral.domain;

/**
 * Lifecycle states for an election.
 *
 * <p>Valid transitions (enforced by domain, not DB):
 * <pre>
 *   PROGRAMADA → ACTIVA → FINALIZADA
 *   PROGRAMADA → CANCELADA
 *   ACTIVA     → SUSPENDIDA → ACTIVA  (resumable)
 *   ACTIVA     → CANCELADA
 * </pre>
 */
public enum ElectionStatus {
    PROGRAMADA,
    ACTIVA,
    FINALIZADA,
    CANCELADA,
    SUSPENDIDA
}
