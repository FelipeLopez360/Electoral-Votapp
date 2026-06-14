package co.com.votapp.ws.auth.domain.port.in;

/**
 * Input port for invalidating a portal session on explicit logout.
 *
 * <p>Delegates to {@link co.com.votapp.ws.auth.domain.port.out.PortalSessionPort#removeSession(String)}
 * so the domain stays decoupled from the Redis implementation detail.
 */
public interface LogoutUseCase {

    /**
     * Invalidates the session identified by {@code sessionToken}.
     *
     * <p>The token is deleted from the session store immediately, preventing reuse
     * even before its TTL expires. If the token does not exist (already expired or invalid),
     * the operation is a no-op.
     *
     * @param sessionToken the opaque bearer token received from the Authorization header
     */
    void logout(String sessionToken);
}
