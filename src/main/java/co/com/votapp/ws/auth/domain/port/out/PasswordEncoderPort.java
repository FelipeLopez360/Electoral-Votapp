package co.com.votapp.ws.auth.domain.port.out;

/**
 * Output port for password hashing.
 *
 * <p>Keeps the domain free from {@code org.springframework.security.*} imports.
 * The implementation ({@code BCryptPasswordEncoderAdapter}) lives in the
 * infrastructure layer.
 */
public interface PasswordEncoderPort {

    /**
     * Encodes (hashes) the given raw password.
     *
     * @param rawPassword the plain-text password — must never be persisted
     * @return the encoded hash safe to store
     */
    String encode(String rawPassword);

    /**
     * Verifies whether the given raw password matches the stored encoded hash.
     *
     * <p>Delegates to BCrypt's constant-time comparison — domain never touches
     * the crypto directly.
     *
     * @param rawPassword    the plain-text password provided at login
     * @param encodedPassword the BCrypt hash stored in the DB
     * @return {@code true} if the raw password matches the hash; {@code false} otherwise
     */
    boolean matches(String rawPassword, String encodedPassword);
}
