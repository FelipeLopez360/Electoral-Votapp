package co.com.votapp.ws.auth.domain.usecase;

import co.com.votapp.ws.auth.domain.Funcionario;
import co.com.votapp.ws.auth.domain.port.in.UpdateFuncionarioUseCase;
import co.com.votapp.ws.auth.domain.port.out.FuncionarioRepositoryPort;
import co.com.votapp.ws.common.exception.NotFoundException;

/**
 * Domain use case: updates editable fields of an existing funcionario.
 *
 * <p>Business rules enforced:
 * <ul>
 *   <li>The funcionario must exist (404 if not)</li>
 *   <li>null fields in the command are ignored (partial update semantics)</li>
 *   <li>passwordHash is never touched by this use case</li>
 * </ul>
 *
 * <p>This class has ZERO Spring or JPA imports — domain purity is enforced.
 */
public class UpdateFuncionarioUseCaseImpl implements UpdateFuncionarioUseCase {

    private final FuncionarioRepositoryPort funcionarioRepository;

    public UpdateFuncionarioUseCaseImpl(FuncionarioRepositoryPort funcionarioRepository) {
        this.funcionarioRepository = funcionarioRepository;
    }

    @Override
    public Funcionario update(Command command) {
        Funcionario existing = funcionarioRepository.findById(command.id())
                .orElseThrow(() -> new NotFoundException(
                        "Funcionario " + command.id() + " no encontrado"));

        // Apply only non-null fields (partial update)
        Funcionario updated = new Funcionario(
                existing.getId(),
                existing.getNumeroEmpleado(),
                existing.getDocumentoIdentidad(),
                command.nombres() != null       ? command.nombres()             : existing.getNombres(),
                command.apellidos() != null     ? command.apellidos()           : existing.getApellidos(),
                existing.getTipoDocumento(),
                command.email() != null         ? command.email()               : existing.getEmail(),
                command.telefono() != null      ? command.telefono()            : existing.getTelefono(),
                command.departamentoId() != null ? command.departamentoId()     : existing.getDepartamentoId(),
                command.cargoId() != null       ? command.cargoId()             : existing.getCargoId(),
                existing.getFechaIngreso(),
                command.puedeVotar() != null    ? command.puedeVotar()          : existing.isPuedeVotar(),
                command.estadoLaboral() != null ? command.estadoLaboral()       : existing.getEstadoLaboral(),
                command.debeCambiarPassword() != null
                        ? command.debeCambiarPassword()
                        : existing.isDebeCambiarPassword()
        );

        return funcionarioRepository.save(updated);
    }
}
