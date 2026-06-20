package co.com.votapp.ws.auth.domain.port.out;

import co.com.votapp.ws.auth.domain.Funcionario;
import co.com.votapp.ws.common.domain.model.PageResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida (driven) para acceder a la persistencia de funcionarios.
 *
 * <p>Reads (findAll, findById, findByDocumentoIdentidad) are called directly
 * from controllers for list/detail queries with no business rule.
 * Commands (save, saveWithHash, existsByDocumentoIdentidad) are used by domain use cases.
 *
 * <h3>Password hash note</h3>
 * <p>{@link Funcionario} never carries {@code passwordHash} — it is a hashed
 * credential, not a business attribute. The {@link #saveWithHash(Funcionario, String)}
 * overload lets the create use case pass the BCrypt hash (already encoded via
 * {@link PasswordEncoderPort}) down to persistence without exposing it in the domain object.
 */
public interface FuncionarioRepositoryPort {

    /**
     * Returns a paginated list of funcionarios.
     * If {@code search} is blank or null, returns all. Ordered by default JPA ordering.
     *
     * @param page   zero-based page index (floored to 0 if negative)
     * @param size   page size (clamped to 1..100; default 8 when caller applies defaults)
     * @param search optional filter on nombres, apellidos, documentoIdentidad (null = match all)
     */
    PageResult<Funcionario> findAll(int page, int size, String search);

    Optional<Funcionario> findById(Integer id);

    Optional<Funcionario> findByDocumentoIdentidad(String documentoIdentidad);

    /**
     * Persists a funcionario update. The existing {@code passwordHash} in the DB is preserved.
     * Used by update flows.
     */
    Funcionario save(Funcionario funcionario);

    /**
     * Persists a new funcionario with the provided password hash.
     * Used by create flows where a fresh hash is generated.
     *
     * @param funcionario   the new funcionario (id must be null)
     * @param passwordHash  the BCrypt-encoded hash to store
     */
    Funcionario saveWithHash(Funcionario funcionario, String passwordHash);

    boolean existsByDocumentoIdentidad(String documentoIdentidad);

    // ─── Portal auth: lockout support ────────────────────────────────────────

    /**
     * Returns only the BCrypt password hash for the given documento.
     * The hash is intentionally separated from the domain {@link Funcionario} object
     * to prevent accidental exposure.
     *
     * @param documentoIdentidad the funcionario's document identifier
     * @return the stored BCrypt hash, or empty if no funcionario found
     */
    Optional<String> findPasswordHashByDocumentoIdentidad(String documentoIdentidad);

    /**
     * Increments the {@code intentos_fallidos} counter by 1 for the given funcionario.
     * Called on every failed login attempt.
     */
    void incrementFailedAttempts(String documentoIdentidad);

    /**
     * Resets {@code intentos_fallidos} to 0 for the given funcionario.
     * Called after a successful login.
     */
    void resetFailedAttempts(String documentoIdentidad);

    /**
     * Sets {@code bloqueado_hasta} to the given timestamp.
     * Triggered when failed attempts reach the maximum threshold (3).
     *
     * @param documentoIdentidad the funcionario's document identifier
     * @param lockedUntil        the timestamp until which the account is locked
     */
    void lockAccount(String documentoIdentidad, LocalDateTime lockedUntil);

    /**
     * Returns the {@code bloqueado_hasta} timestamp for the given funcionario,
     * or empty if the account is not locked.
     */
    Optional<LocalDateTime> findBloqueadoHasta(String documentoIdentidad);

    /**
     * Returns the current {@code intentos_fallidos} count for the given funcionario.
     */
    int findFailedAttempts(String documentoIdentidad);

    /**
     * Updates the {@code ultimo_acceso} timestamp to now for the given funcionario.
     * Called after a successful login.
     */
    void updateUltimoAcceso(String documentoIdentidad, LocalDateTime accessTime);

    /**
     * Updates the BCrypt password hash for the given funcionario.
     * Called by {@code ChangePasswordUseCaseImpl} after validating and encoding the new password.
     *
     * @param documentoIdentidad the funcionario's document identifier
     * @param newPasswordHash    the new BCrypt-encoded hash to store
     */
    void updatePasswordHash(String documentoIdentidad, String newPasswordHash);

    // ─── Census bulk-add queries (used by electoral context) ─────────────────

    /**
     * Returns all globally eligible funcionarios from a given department.
     *
     * <p>Eligible means: {@code estadoLaboral = 'ACTIVO'} AND {@code puede_votar = true}.
     * Used by {@code ManageCensoUseCaseImpl.addByDepartamento()} to build the bulk-add list.
     *
     * @param departamentoId the department to query
     * @return list of eligible funcionarios
     */
    List<Funcionario> findEligibleByDepartamento(Integer departamentoId);

    /**
     * Returns globally eligible funcionarios matching flexible filter criteria.
     *
     * <p>All parameters are applied as AND conditions when non-null.
     * Used by {@code ManageCensoUseCaseImpl.addByFilters()}.
     *
     * @param departamentoId optional department filter (null = all departments)
     * @param estadoLaboral  optional labor status filter (null = any status)
     * @param puedeVotar     optional voting eligibility filter (null = any)
     * @return list of matching funcionarios
     */
    List<Funcionario> findEligibleByFilters(Integer departamentoId, String estadoLaboral, Boolean puedeVotar);
}
