package co.com.votapp.ws.auth.domain.port.out;

import java.util.Optional;

/**
 * Output port for portal session management.
 *
 * <p>Abstracts the session store (Redis) behind a domain-friendly contract.
 * The domain use cases interact with this port exclusively — no Redis imports
 * reach the domain layer.
 *
 * <p>Sessions are opaque bearer tokens (UUID strings) stored with a 30-minute TTL.
 * The resolved value is always the authenticated funcionario's integer ID.
 */
public interface PortalSessionPort {

    /**
     * Creates a new portal session for the given funcionario.
     *
     * <p>Generates a UUID session token, stores {@code funcionarioId} in the session store
     * under key {@code portal:session:{token}}, and sets a 30-minute TTL.
     *
     * @param funcionarioId the authenticated funcionario's database ID
     * @return the opaque session token (UUID string) to be returned in the response
     */
    String createSession(Integer funcionarioId);

    /**
     * Resolves the funcionario ID from an existing session token.
     *
     * @param sessionToken the opaque bearer token received from the client
     * @return the funcionario ID if the session is active, or empty if the session has expired or is invalid
     */
    Optional<Integer> getFuncionarioIdFromSession(String sessionToken);

    /**
     * Invalidates the session associated with the given token.
     *
     * <p>Called on explicit logout. Deletes the Redis key immediately so the token
     * cannot be reused even before its TTL expires.
     *
     * @param sessionToken the session token to invalidate
     */
    void removeSession(String sessionToken);
}
