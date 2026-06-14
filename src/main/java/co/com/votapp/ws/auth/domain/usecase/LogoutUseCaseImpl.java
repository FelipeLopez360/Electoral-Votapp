package co.com.votapp.ws.auth.domain.usecase;

import co.com.votapp.ws.auth.domain.port.in.LogoutUseCase;
import co.com.votapp.ws.auth.domain.port.out.PortalSessionPort;

/**
 * Implementation of {@link LogoutUseCase}.
 *
 * <p>A minimal delegation: receives the raw session token from the controller
 * and forwards it to {@link PortalSessionPort#removeSession(String)} to invalidate
 * the Redis entry immediately.
 *
 * <p>No Spring annotations — wired manually via {@code DomainConfig}.
 */
public class LogoutUseCaseImpl implements LogoutUseCase {

    private final PortalSessionPort sessionPort;

    public LogoutUseCaseImpl(PortalSessionPort sessionPort) {
        this.sessionPort = sessionPort;
    }

    @Override
    public void logout(String sessionToken) {
        sessionPort.removeSession(sessionToken);
    }
}
