package co.com.votapp.ws.auth.domain.usecase;

import co.com.votapp.ws.auth.domain.Funcionario;
import co.com.votapp.ws.auth.domain.port.in.UpdateProfileUseCase;
import co.com.votapp.ws.auth.domain.port.out.FuncionarioRepositoryPort;
import co.com.votapp.ws.common.exception.NotFoundException;

/**
 * Implementation of {@link UpdateProfileUseCase}.
 *
 * <p>Loads the existing {@link Funcionario}, reconstructs it with only
 * {@code email} and {@code telefono} changed, then delegates to
 * {@link FuncionarioRepositoryPort#save(Funcionario)}. All other fields are
 * preserved verbatim — no restricted data can be modified via this use case.
 *
 * <p>No Spring annotations — wired manually via {@code DomainConfig}.
 */
public class UpdateProfileUseCaseImpl implements UpdateProfileUseCase {

    private final FuncionarioRepositoryPort repositoryPort;

    public UpdateProfileUseCaseImpl(FuncionarioRepositoryPort repositoryPort) {
        this.repositoryPort = repositoryPort;
    }

    @Override
    public Funcionario update(String documentoIdentidad, String email, String telefono) {
        Funcionario existing = repositoryPort.findByDocumentoIdentidad(documentoIdentidad)
                .orElseThrow(() -> new NotFoundException(
                        "Funcionario not found for documento: " + documentoIdentidad));

        // Rebuild with only email and telefono changed — all other fields copied verbatim
        Funcionario updated = new Funcionario(
                existing.getId(),
                existing.getNumeroEmpleado(),
                existing.getDocumentoIdentidad(),
                existing.getNombres(),
                existing.getApellidos(),
                existing.getTipoDocumento(),
                email,
                telefono,
                existing.getDepartamentoId(),
                existing.getCargoId(),
                existing.getFechaIngreso(),
                existing.isPuedeVotar(),
                existing.getEstadoLaboral(),
                existing.isDebeCambiarPassword()
        );

        return repositoryPort.save(updated);
    }
}
