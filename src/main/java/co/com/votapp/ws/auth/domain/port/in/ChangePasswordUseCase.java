package co.com.votapp.ws.auth.domain.port.in;

/**
 * Input port for changing a funcionario's password.
 *
 * <p>Verifies the current password, enforces the strength policy on the new one,
 * persists the new hash, and clears {@code debeCambiarPassword}.
 */
public interface ChangePasswordUseCase {

    /**
     * Changes the password for the funcionario identified by {@code documentoIdentidad}.
     *
     * @param documentoIdentidad the funcionario's document identifier (resolved from session by the controller)
     * @param currentPassword    the plain-text current password for verification
     * @param newPassword        the plain-text new password — must meet the strength policy
     * @throws co.com.votapp.ws.auth.domain.exception.InvalidCredentialsException if {@code currentPassword} does not match the stored hash
     * @throws co.com.votapp.ws.auth.domain.exception.WeakPasswordException       if {@code newPassword} does not meet the strength rules
     */
    void change(String documentoIdentidad, String currentPassword, String newPassword);
}
