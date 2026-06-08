package co.com.votapp.ws.auth.infrastructure.adapter.in.web;

import co.com.votapp.ws.auth.domain.Funcionario;
import co.com.votapp.ws.auth.domain.port.in.AuthenticateFuncionarioPort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para el módulo de autenticación.
 * Acepta credenciales y devuelve datos básicos del funcionario autenticado.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Login and identity verification for institutional employees")
public class AuthController {

    private final AuthenticateFuncionarioPort authenticatePort;

    public AuthController(AuthenticateFuncionarioPort authenticatePort) {
        this.authenticatePort = authenticatePort;
    }

    @PostMapping("/login")
    @Operation(
            summary = "Login",
            description = "Authenticates a funcionario by documento de identidad. "
                    + "Returns basic profile data if the account is active.",
            security = @SecurityRequirement(name = "basicAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authentication successful — returns funcionario profile"),
            @ApiResponse(responseCode = "401", description = "Authentication required — HTTP Basic credentials missing or invalid"),
            @ApiResponse(responseCode = "409", description = "Account inactive or credentials not found")
    })
    public ResponseEntity<FuncionarioResponse> login(@RequestBody LoginRequest request) {
        Funcionario funcionario = authenticatePort.authenticate(
                request.documentoIdentidad(),
                request.password()
        );
        return ResponseEntity.ok(new FuncionarioResponse(
                funcionario.getId(),
                funcionario.getNumeroEmpleado(),
                funcionario.getEmail(),
                funcionario.isPuedeVotar()
        ));
    }

    /** DTO de entrada (Record — obligatorio por arquitectura). */
    public record LoginRequest(String documentoIdentidad, String password) {}

    /** DTO de salida (Record — obligatorio por arquitectura). */
    public record FuncionarioResponse(Integer id,
                                      String numeroEmpleado,
                                      String email,
                                      boolean puedeVotar) {}
}
