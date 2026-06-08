package co.com.votapp.ws.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
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
        // When the browser receives WWW-Authenticate: Basic it shows its own native popup,
        // which intercepts Swagger UI's Authorization header before it reaches the server.
        // Replacing the entry point with one that returns 401 WITHOUT that header suppresses
        // the popup so Swagger can send credentials uninterrupted.
        AuthenticationEntryPoint suppressBrowserPopup =
                (request, response, ex) -> response.sendError(401, "Unauthorized");

        http
                // REST APIs are stateless — CSRF only matters for browser-based session auth
                .csrf(AbstractHttpConfigurer::disable)
                // No HTTP session — each request must carry credentials
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Public endpoints: vote casting (token IS the auth), ballot viewing, eligibility check.
                // Admin endpoints (elections CRUD, token issuance) require HTTP Basic auth.
                // SpringDoc paths are always permitted.
                .authorizeHttpRequests(auth ->
                        auth.requestMatchers(
                                        "/api/v1/votes/**",
                                        "/api/v1/elections/*/ballot",
                                        "/api/v1/voters/**"
                                ).permitAll()
                                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                                .requestMatchers("/api/**").authenticated()
                                .anyRequest().permitAll())
                .httpBasic(basic -> basic.authenticationEntryPoint(suppressBrowserPopup));
        return http.build();
    }
}
