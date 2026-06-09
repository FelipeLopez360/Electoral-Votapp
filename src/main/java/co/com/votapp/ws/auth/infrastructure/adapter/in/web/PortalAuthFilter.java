package co.com.votapp.ws.auth.infrastructure.adapter.in.web;

import co.com.votapp.ws.auth.domain.port.out.PortalSessionPort;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Security filter for portal endpoints ({@code /api/v1/portal/**}).
 *
 * <p>Reads the {@code Authorization: Bearer {sessionToken}} header, resolves the
 * funcionario ID from Redis via {@link PortalSessionPort}, and stores it as a
 * request attribute ({@code portalFuncionarioId}) for downstream controllers.
 *
 * <p>If the header is missing, malformed, or the session is invalid/expired,
 * returns 401 Unauthorized immediately without delegating to the filter chain.
 *
 * <p>Login endpoint ({@code POST /api/v1/portal/login}) is excluded — it must
 * be permitted before authentication.
 *
 * <p>NOT annotated with {@code @Component} — instantiated and wired by
 * {@link co.com.votapp.ws.common.config.SecurityConfig} as a Spring-managed bean
 * to avoid auto-discovery in unrelated {@code @WebMvcTest} slices.
 */
public class PortalAuthFilter extends OncePerRequestFilter {

    public static final String FUNCIONARIO_ID_ATTRIBUTE = "portalFuncionarioId";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String LOGIN_PATH = "/api/v1/portal/login";

    private final PortalSessionPort sessionPort;

    public PortalAuthFilter(PortalSessionPort sessionPort) {
        this.sessionPort = sessionPort;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Login endpoint is public — skip this filter entirely
        return LOGIN_PATH.equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Portal session token required");
            return;
        }

        String sessionToken = authHeader.substring(BEARER_PREFIX.length()).trim();
        Optional<Integer> funcionarioId = sessionPort.getFuncionarioIdFromSession(sessionToken);

        if (funcionarioId.isEmpty()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Portal session invalid or expired");
            return;
        }

        // Store resolved funcionarioId in request attributes for controllers
        request.setAttribute(FUNCIONARIO_ID_ATTRIBUTE, funcionarioId.get());
        filterChain.doFilter(request, response);
    }
}
