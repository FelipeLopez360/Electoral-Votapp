package co.com.votapp.ws.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for the Votapp REST API.
 *
 * <p>The API is stateless (no session, no cookies for auth), so CSRF protection is disabled.
 * All requests to {@code /api/**} are permitted with HTTP Basic auth by default.
 * Future phases will replace this with JWT bearer token authentication.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                // REST APIs are stateless — CSRF only matters for browser-based session auth
                .csrf(AbstractHttpConfigurer::disable)
                // No HTTP session — each request must carry credentials
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Require authentication for all API endpoints; HTTP Basic is the default.
                // SpringDoc paths are explicitly permitted so docs are accessible without credentials.
                .authorizeHttpRequests(auth ->
                        auth.requestMatchers("/api/**").authenticated()
                                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                                .anyRequest().permitAll())
                .httpBasic(httpBasic -> {});
        return http.build();
    }
}
