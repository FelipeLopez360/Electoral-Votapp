package co.com.votapp.ws.auth.infrastructure.adapter.out.persistence;

import co.com.votapp.ws.auth.domain.Funcionario;
import co.com.votapp.ws.auth.domain.port.out.FuncionarioRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador de persistencia para el módulo auth.
 *
 * <p>Implementa {@link FuncionarioRepositoryPort} usando Spring Data JPA.
 * Convierte entre {@link FuncionarioEntity} (infra) y {@link Funcionario} (dominio).
 *
 * <h3>Password hash handling</h3>
 * <p>The {@link Funcionario} domain object never carries {@code passwordHash}
 * (it is a hashed credential, not a business attribute). The persistence
 * adapter handles two flows:
 * <ul>
 *   <li><b>Create</b>: the caller ({@code CreateFuncionarioUseCaseImpl}) passes the
 *       encoded password hash through a dedicated save overload
 *       {@link #saveWithHash(Funcionario, String)}.</li>
 *   <li><b>Update</b>: {@link #save(Funcionario)} reads the existing entity first
 *       and preserves its {@code passwordHash} unchanged.</li>
 * </ul>
 */
@Component
public class FuncionarioRepositoryAdapter implements FuncionarioRepositoryPort {

    private final FuncionarioJpaRepository jpaRepository;

    public FuncionarioRepositoryAdapter(FuncionarioJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<Funcionario> findAll(String search) {
        return jpaRepository.search(search == null ? "" : search)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<Funcionario> findById(Integer id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Funcionario> findByDocumentoIdentidad(String documentoIdentidad) {
        return jpaRepository.findByDocumentoIdentidad(documentoIdentidad)
                .map(this::toDomain);
    }

    /**
     * Updates an existing funcionario, preserving its {@code passwordHash}.
     * Used by {@code UpdateFuncionarioUseCaseImpl}.
     */
    @Override
    public Funcionario save(Funcionario funcionario) {
        // Load existing entity to preserve passwordHash and auto-generated fields
        FuncionarioEntity entity = funcionario.getId() != null
                ? jpaRepository.findById(funcionario.getId())
                        .orElse(new FuncionarioEntity())
                : new FuncionarioEntity();
        applyDomainToEntity(funcionario, entity);
        FuncionarioEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    /**
     * Creates a new funcionario and sets its {@code passwordHash}.
     * Auto-generates {@code numeroEmpleado} when the domain object carries {@code null}
     * (the domain intentionally does not require it as input — it is an infrastructure detail).
     *
     * <p>The final value is derived from the DB-generated id ({@code EMP000123}), so it is
     * guaranteed unique and stays within the schema limit ({@code VARCHAR(20)}).
     *
     * <p>Used by {@code CreateFuncionarioUseCaseImpl}.
     */
    public Funcionario saveWithHash(Funcionario funcionario, String passwordHash) {
        FuncionarioEntity entity = new FuncionarioEntity();
        applyDomainToEntity(funcionario, entity);
        entity.setPasswordHash(passwordHash);
        boolean numeroEmpleadoWasMissing = entity.getNumeroEmpleado() == null;

        // The schema requires numero_empleado on insert, so use a temporary unique value
        // before replacing it with the stable id-based employee number.
        if (numeroEmpleadoWasMissing) {
            entity.setNumeroEmpleado(generateTemporaryNumeroEmpleado());
        }

        FuncionarioEntity saved = jpaRepository.save(entity);
        if (numeroEmpleadoWasMissing) {
            saved.setNumeroEmpleado(generateNumeroEmpleado(saved.getId()));
            saved = jpaRepository.save(saved);
        }
        return toDomain(saved);
    }

    private String generateTemporaryNumeroEmpleado() {
        return "TMP" + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 12)
                .toUpperCase();
    }

    private String generateNumeroEmpleado(Integer id) {
        return "EMP%06d".formatted(id);
    }

    @Override
    public boolean existsByDocumentoIdentidad(String documentoIdentidad) {
        return jpaRepository.existsByDocumentoIdentidad(documentoIdentidad);
    }

    // ─── Mapping ─────────────────────────────────────────────────────────────

    private Funcionario toDomain(FuncionarioEntity entity) {
        return new Funcionario(
                entity.getId(),
                entity.getNumeroEmpleado(),
                entity.getDocumentoIdentidad(),
                entity.getNombres(),
                entity.getApellidos(),
                entity.getTipoDocumento(),
                entity.getEmail(),
                entity.getTelefono(),
                entity.getDepartamentoId(),
                entity.getCargoId(),
                entity.getFechaIngreso(),
                Boolean.TRUE.equals(entity.getPuedeVotar()),
                entity.getEstadoLaboral(),
                Boolean.TRUE.equals(entity.getDebeCambiarPassword())
        );
    }

    private void applyDomainToEntity(Funcionario funcionario, FuncionarioEntity entity) {
        entity.setId(funcionario.getId());
        entity.setNumeroEmpleado(funcionario.getNumeroEmpleado());
        entity.setDocumentoIdentidad(funcionario.getDocumentoIdentidad());
        entity.setNombres(funcionario.getNombres());
        entity.setApellidos(funcionario.getApellidos());
        entity.setTipoDocumento(funcionario.getTipoDocumento());
        entity.setEmail(funcionario.getEmail());
        entity.setTelefono(funcionario.getTelefono());
        entity.setDepartamentoId(funcionario.getDepartamentoId());
        entity.setCargoId(funcionario.getCargoId());
        entity.setFechaIngreso(funcionario.getFechaIngreso());
        entity.setPuedeVotar(funcionario.isPuedeVotar());
        entity.setEstadoLaboral(funcionario.getEstadoLaboral());
        entity.setDebeCambiarPassword(funcionario.isDebeCambiarPassword());
        // passwordHash is intentionally NOT set here — managed by callers
    }
}
