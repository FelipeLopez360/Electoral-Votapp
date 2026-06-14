package co.com.votapp.ws.auth.domain.usecase;

import co.com.votapp.ws.auth.domain.exception.InvalidCredentialsException;
import co.com.votapp.ws.auth.domain.exception.WeakPasswordException;
import co.com.votapp.ws.auth.domain.port.in.ChangePasswordUseCase;
import co.com.votapp.ws.auth.domain.port.out.FuncionarioRepositoryPort;
import co.com.votapp.ws.auth.domain.port.out.PasswordEncoderPort;

/**
 * Implementation of {@link ChangePasswordUseCase}.
 *
 * <p>Business flow:
 * <ol>
 *   <li>Fetch the stored BCrypt hash (fails with InvalidCredentials if not found)</li>
 *   <li>Verify the provided current password against the stored hash</li>
 *   <li>Validate the new password strength (min 8 chars, 1 upper, 1 lower, 1 digit)</li>
 *   <li>Encode the new password and delegate persistence to the repository port</li>
 * </ol>
 *
 * <p>No Spring annotations — wired manually via {@code DomainConfig}.
 */
public class ChangePasswordUseCaseImpl implements ChangePasswordUseCase {

    private final FuncionarioRepositoryPort repositoryPort;
    private final PasswordEncoderPort passwordEncoderPort;

    public ChangePasswordUseCaseImpl(FuncionarioRepositoryPort repositoryPort,
                                     PasswordEncoderPort passwordEncoderPort) {
        this.repositoryPort = repositoryPort;
        this.passwordEncoderPort = passwordEncoderPort;
    }

    @Override
    public void change(String documentoIdentidad, String currentPassword, String newPassword) {
        // Step 1: Fetch stored hash
        String storedHash = repositoryPort.findPasswordHashByDocumentoIdentidad(documentoIdentidad)
                .orElseThrow(InvalidCredentialsException::new);

        // Step 2: Verify current password
        if (!passwordEncoderPort.matches(currentPassword, storedHash)) {
            throw new InvalidCredentialsException();
        }

        // Step 2b: New password must differ from current
        if (currentPassword.equals(newPassword)) {
            throw new WeakPasswordException();
        }

        // Step 3: Validate new password strength
        validateStrength(newPassword);

        // Step 4: Encode and persist
        String newHash = passwordEncoderPort.encode(newPassword);
        repositoryPort.updatePasswordHash(documentoIdentidad, newHash);
    }

    /**
     * Validates password strength: minimum 8 characters, at least one uppercase letter,
     * one lowercase letter, and one digit.
     *
     * @param password the plain-text password to validate
     * @throws WeakPasswordException if any strength rule is violated
     */
    private void validateStrength(String password) {
        if (password == null || password.length() < 8) {
            throw new WeakPasswordException();
        }
        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
        }
        if (!hasUpper || !hasLower || !hasDigit) {
            throw new WeakPasswordException();
        }
    }
}
