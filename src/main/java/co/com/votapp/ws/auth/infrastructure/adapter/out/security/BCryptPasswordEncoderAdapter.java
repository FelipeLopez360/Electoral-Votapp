package co.com.votapp.ws.auth.infrastructure.adapter.out.security;

import co.com.votapp.ws.auth.domain.port.out.PasswordEncoderPort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Infrastructure adapter that fulfils {@link PasswordEncoderPort} using
 * Spring Security's {@link BCryptPasswordEncoder}.
 *
 * <p>This adapter is the ONLY place in the codebase that imports
 * {@code org.springframework.security.crypto.*}. The domain use cases
 * interact with the {@link PasswordEncoderPort} interface only, keeping
 * domain code Spring-free.
 */
@Component
public class BCryptPasswordEncoderAdapter implements PasswordEncoderPort {

    private final BCryptPasswordEncoder encoder;

    public BCryptPasswordEncoderAdapter() {
        this.encoder = new BCryptPasswordEncoder();
    }

    @Override
    public String encode(String rawPassword) {
        return encoder.encode(rawPassword);
    }
}
