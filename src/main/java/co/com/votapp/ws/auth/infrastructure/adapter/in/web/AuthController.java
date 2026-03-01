package co.com.votapp.ws.auth.infrastructure.adapter.in.web;

import co.com.votapp.ws.auth.application.port.in.AuthenticateFuncionarioPort;
import co.com.votapp.ws.auth.domain.Funcionario;
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
public class AuthController {

    private final AuthenticateFuncionarioPort authenticatePort;

    public AuthController(AuthenticateFuncionarioPort authenticatePort) {
        this.authenticatePort = authenticatePort;
    }

    @PostMapping("/login")
    public ResponseEntity<FuncionarioResponse> login(@RequestBody LoginRequest request) {
        Funcionario funcionario = authenticatePort.authenticate(
                request.documentoIdentidad(),
                request.password()
        );
        return ResponseEntity.ok(new FuncionarioResponse(
                funcionario.getUuid().toString(),
                funcionario.getNumeroEmpleado(),
                funcionario.getEmail(),
                funcionario.isPuedeVotar()
        ));
    }

    /** DTO de entrada (Record — obligatorio por arquitectura). */
    public record LoginRequest(String documentoIdentidad, String password) {}

    /** DTO de salida (Record — obligatorio por arquitectura). */
    public record FuncionarioResponse(String uuid,
                                      String numeroEmpleado,
                                      String email,
                                      boolean puedeVotar) {}
}
