package co.com.votapp.ws.common.config;

import co.com.votapp.ws.auth.domain.port.out.PortalSessionPort;
import co.com.votapp.ws.auth.infrastructure.adapter.in.web.PortalAuthFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * Security configuration for the Votapp REST API.
 *
 * <p>TWO filter chains (ordered):
 * <ol>
 *   <li><b>Portal chain</b> (Order 1) — matches {@code /api/v1/portal/**}. Stateless,
 *       no HTTP Basic. Login is public. All other portal routes require a valid Redis
 *       session token validated by {@link PortalAuthFilter}.</li>
 *   <li><b>Admin chain</b> (Order 2) — catches all remaining {@code /api/**} requests.
 *       HTTP Basic auth unchanged from prior implementation.</li>
 * </ol>
 *
 * <p>Both chains are STATELESS and have CSRF disabled (REST APIs).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Exposes {@link PortalAuthFilter} as a managed bean.
     *
     * <p>NOT annotated with {@code @Component} to avoid auto-discovery in {@code @WebMvcTest} slices.
     * The {@code FilterRegistrationBean} below disables its auto-registration as a global servlet filter
     * — we only want it inside the portal {@link SecurityFilterChain}, not running on every request.
     */
    @Bean
    public PortalAuthFilter portalAuthFilter(PortalSessionPort sessionPort) {
        return new PortalAuthFilter(sessionPort);
    }

    /**
     * Prevents Spring Boot from auto-registering {@link PortalAuthFilter} as a global servlet filter.
     *
     * <p>The filter is added to the PORTAL {@link SecurityFilterChain} only.
     * Without this, it would intercept ALL requests including admin endpoints.
     */
    @Bean
    public FilterRegistrationBean<PortalAuthFilter> portalAuthFilterRegistration(PortalAuthFilter filter) {
        FilterRegistrationBean<PortalAuthFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    /**
     * Portal security chain — matches {@code /api/v1/portal/**} (Order 1, evaluated first).
     *
     * <p>Login is permitted without authentication. All other portal routes require the
     * {@link PortalAuthFilter} to resolve a valid session token from Redis.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain portalSecurityFilterChain(HttpSecurity http,
                                                          PortalAuthFilter portalAuthFilter) throws Exception {
        http
                .securityMatcher("/api/v1/portal/**")
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth ->
                        auth
                                // All portal routes are permitted at the Spring Security level.
                                // The PortalAuthFilter handles actual authentication for non-login endpoints.
                                // Login itself is skipped by shouldNotFilter() on the filter.
                                .anyRequest().permitAll())
                // Insert the portal filter BEFORE Spring's default auth filter
                .addFilterBefore(portalAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Admin security chain — catches all remaining {@code /api/**} requests (Order 2).
     *
     * <p>HTTP Basic auth as before. Portal paths are matched by Order 1 chain and never reach here.
     */
    @Bean
    @Order(2)
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
                // CORS: allow SPA origin (localhost:5173 in dev)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
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
                                // Allow CORS preflight (OPTIONS) without auth
                                .requestMatchers(this::isPreFlight).permitAll()
                                .requestMatchers("/api/**").authenticated()
                                .anyRequest().permitAll())
                .httpBasic(basic -> basic.authenticationEntryPoint(suppressBrowserPopup));
        return http.build();
    }

    /** CORS: allow the SPA (Vite dev server or deployed frontend) to call the API. */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /** Detect CORS preflight requests. */
    private boolean isPreFlight(HttpServletRequest request) {
        return "OPTIONS".equalsIgnoreCase(request.getMethod());
    }
}
