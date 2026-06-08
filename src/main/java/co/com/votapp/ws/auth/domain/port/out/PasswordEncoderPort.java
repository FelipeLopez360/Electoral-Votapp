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
}
