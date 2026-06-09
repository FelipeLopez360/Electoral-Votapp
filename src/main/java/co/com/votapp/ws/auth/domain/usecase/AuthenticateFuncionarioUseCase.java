package co.com.votapp.ws.auth.domain.usecase;

import co.com.votapp.ws.auth.domain.Funcionario;
import co.com.votapp.ws.auth.domain.exception.AccountInactiveException;
import co.com.votapp.ws.auth.domain.exception.AccountLockedException;
import co.com.votapp.ws.auth.domain.exception.InvalidCredentialsException;
import co.com.votapp.ws.auth.domain.port.in.AuthenticateFuncionarioPort;
import co.com.votapp.ws.auth.domain.port.out.FuncionarioRepositoryPort;
import co.com.votapp.ws.auth.domain.port.out.PasswordEncoderPort;

import java.time.LocalDateTime;

/**
 * Caso de uso: autentica a un funcionario verificando sus credenciales con BCrypt
 * y aplicando la política de bloqueo por intentos fallidos.
 *
 * <p><b>Flujo completo:</b>
 * <ol>
 *   <li>Check if account is currently locked ({@code bloqueado_hasta > now})</li>
 *   <li>Find funcionario by documento; if not found → throw InvalidCredentialsException</li>
 *   <li>Check if active; if not → throw AccountInactiveException</li>
 *   <li>Fetch password hash separately (never on the Funcionario domain object)</li>
 *   <li>BCrypt verify; if fails → increment failed attempts, lock if >= 3, throw InvalidCredentialsException</li>
 *   <li>On success: reset failed attempts, update ultimo_acceso, return Funcionario</li>
 * </ol>
 *
 * <p>No Spring annotations — wired manually via {@code DomainConfig}.
 */
public class AuthenticateFuncionarioUseCase implements AuthenticateFuncionarioPort {

    private static final int MAX_FAILED_ATTEMPTS = 3;
    private static final int LOCKOUT_MINUTES = 15;

    private final FuncionarioRepositoryPort repositoryPort;
    private final PasswordEncoderPort passwordEncoderPort;

    public AuthenticateFuncionarioUseCase(FuncionarioRepositoryPort repositoryPort,
                                          PasswordEncoderPort passwordEncoderPort) {
        this.repositoryPort = repositoryPort;
        this.passwordEncoderPort = passwordEncoderPort;
    }

    @Override
    public Funcionario authenticate(String documentoIdentidad, String rawPassword) {
        // Step 1: Check if account is currently locked
        repositoryPort.findBloqueadoHasta(documentoIdentidad)
                .filter(lockedUntil -> lockedUntil.isAfter(LocalDateTime.now()))
                .ifPresent(lockedUntil -> {
                    throw new AccountLockedException();
                });

        // Step 2: Find funcionario by documento
        Funcionario funcionario = repositoryPort.findByDocumentoIdentidad(documentoIdentidad)
                .orElseThrow(InvalidCredentialsException::new);

        // Step 3: Check if account is active
        if (!funcionario.isActivo()) {
            throw new AccountInactiveException();
        }

        // Step 4: Fetch stored hash (separate from domain object)
        String storedHash = repositoryPort.findPasswordHashByDocumentoIdentidad(documentoIdentidad)
                .orElseThrow(InvalidCredentialsException::new);

        // Step 5: BCrypt verify
        if (!passwordEncoderPort.matches(rawPassword, storedHash)) {
            repositoryPort.incrementFailedAttempts(documentoIdentidad);
            int attempts = repositoryPort.findFailedAttempts(documentoIdentidad);
            if (attempts >= MAX_FAILED_ATTEMPTS) {
                repositoryPort.lockAccount(documentoIdentidad, LocalDateTime.now().plusMinutes(LOCKOUT_MINUTES));
            }
            throw new InvalidCredentialsException();
        }

        // Step 6: Successful login — reset counters and record access
        repositoryPort.resetFailedAttempts(documentoIdentidad);
        repositoryPort.updateUltimoAcceso(documentoIdentidad, LocalDateTime.now());

        return funcionario;
    }
}
