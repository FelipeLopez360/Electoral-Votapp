package co.com.votapp.ws.auth.domain.usecase;

import co.com.votapp.ws.auth.domain.Funcionario;
import co.com.votapp.ws.auth.domain.port.in.CreateFuncionarioUseCase;
import co.com.votapp.ws.auth.domain.port.out.FuncionarioRepositoryPort;
import co.com.votapp.ws.auth.domain.port.out.PasswordEncoderPort;
import co.com.votapp.ws.common.exception.DomainException;
import co.com.votapp.ws.common.exception.ValidationException;

import java.security.SecureRandom;

/**
 * Domain use case: creates a new funcionario with an auto-generated temporary password.
 *
 * <p>Business rules enforced:
 * <ul>
 *   <li>nombres and documentoIdentidad must not be blank (400)</li>
 *   <li>documentoIdentidad must be unique (409)</li>
 *   <li>A random 12-character alphanumeric password is generated via {@link SecureRandom}</li>
 *   <li>The raw password is encoded via {@link PasswordEncoderPort} (keeps Spring out of domain)</li>
 *   <li>debeCambiarPassword is always {@code true} for new funcionarios</li>
 * </ul>
 *
 * <p>This class has ZERO Spring or JPA imports — domain purity is enforced.
 */
public class CreateFuncionarioUseCaseImpl implements CreateFuncionarioUseCase {

    private static final String ALNUM = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int PASSWORD_LENGTH = 12;

    private final FuncionarioRepositoryPort funcionarioRepository;
    private final PasswordEncoderPort passwordEncoder;

    public CreateFuncionarioUseCaseImpl(FuncionarioRepositoryPort funcionarioRepository,
                                         PasswordEncoderPort passwordEncoder) {
        this.funcionarioRepository = funcionarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Result create(Command command) {
        validateRequired(command);

        if (funcionarioRepository.existsByDocumentoIdentidad(command.documentoIdentidad())) {
            throw new DomainException(
                    "Ya existe un funcionario con el documento " + command.documentoIdentidad());
        }

        String rawPassword = generatePassword();
        String hashedPassword = passwordEncoder.encode(rawPassword);

        Funcionario funcionario = new Funcionario(
                null,                                                      // id: DB generates
                null,                                                      // numeroEmpleado: not set at create time
                command.documentoIdentidad(),
                command.nombres(),
                command.apellidos(),
                command.tipoDocumento() != null ? command.tipoDocumento() : "CC",
                command.email(),
                command.telefono(),
                command.departamentoId(),
                command.cargoId(),
                null,                                                      // fechaIngreso
                command.puedeVotar(),
                command.estadoLaboral() != null ? command.estadoLaboral() : "ACTIVO",
                true                                                       // debeCambiarPassword always true on create
        );

        Funcionario saved = funcionarioRepository.saveWithHash(funcionario, hashedPassword);
        return new Result(saved, rawPassword);
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    private void validateRequired(Command command) {
        if (command.nombres() == null || command.nombres().isBlank()) {
            throw new ValidationException("El campo 'nombres' es requerido");
        }
        if (command.apellidos() == null || command.apellidos().isBlank()) {
            throw new ValidationException("El campo 'apellidos' es requerido");
        }
        if (command.documentoIdentidad() == null || command.documentoIdentidad().isBlank()) {
            throw new ValidationException("El campo 'documentoIdentidad' es requerido");
        }
    }

    private String generatePassword() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(PASSWORD_LENGTH);
        for (int i = 0; i < PASSWORD_LENGTH; i++) {
            sb.append(ALNUM.charAt(random.nextInt(ALNUM.length())));
        }
        return sb.toString();
    }
}
