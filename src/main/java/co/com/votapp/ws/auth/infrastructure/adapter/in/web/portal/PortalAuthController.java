package co.com.votapp.ws.auth.infrastructure.adapter.in.web.portal;

import co.com.votapp.ws.auth.domain.Funcionario;
import co.com.votapp.ws.auth.domain.port.in.AuthenticateFuncionarioPort;
import co.com.votapp.ws.auth.domain.port.out.PortalSessionPort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST adapter for portal login.
 *
 * <p>Public endpoint — no session token required for login itself.
 * The {@link co.com.votapp.ws.auth.infrastructure.adapter.in.web.PortalAuthFilter}
 * skips this path via {@code shouldNotFilter()}.
 *
 * <p>On success, creates a Redis-backed opaque session token via {@link PortalSessionPort}
 * and returns it to the client. The client must send it as {@code Authorization: Bearer <token>}
 * on all subsequent portal requests.
 */
@RestController
@RequestMapping("/api/v1/portal")
@Tag(name = "Portal Auth", description = "Funcionario self-service portal authentication")
public class PortalAuthController {

    private final AuthenticateFuncionarioPort authenticatePort;
    private final PortalSessionPort sessionPort;

    public PortalAuthController(AuthenticateFuncionarioPort authenticatePort,
                                PortalSessionPort sessionPort) {
        this.authenticatePort = authenticatePort;
        this.sessionPort = sessionPort;
    }

    @PostMapping("/login")
    @Operation(
            summary = "Portal login",
            description = "Authenticates a funcionario using documento de identidad and password. "
                    + "Returns an opaque session token (Redis-backed, 30-min TTL). "
                    + "Send the token as 'Authorization: Bearer <token>' on subsequent portal requests."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful — sessionToken returned"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "423", description = "Account temporarily locked"),
            @ApiResponse(responseCode = "403", description = "Account inactive")
    })
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        // Exceptions (InvalidCredentialsException, AccountLockedException, AccountInactiveException)
        // bubble up to GlobalExceptionHandler which maps them to 401 / 423 / 403.
        Funcionario funcionario = authenticatePort.authenticate(
                request.documentoIdentidad(),
                request.password()
        );
        String sessionToken = sessionPort.createSession(funcionario.getId());
        return ResponseEntity.ok(new LoginResponse(
                sessionToken,
                funcionario.getId(),
                funcionario.getNombres() + " " + funcionario.getApellidos()
        ));
    }

    // ── DTOs ─────────────────────────────────────────────────────────────────

    /** Request body for portal login. */
    public record LoginRequest(String documentoIdentidad, String password) {}

    /** Response body returned on successful portal login. */
    public record LoginResponse(String sessionToken, Integer funcionarioId, String nombre) {}
}
