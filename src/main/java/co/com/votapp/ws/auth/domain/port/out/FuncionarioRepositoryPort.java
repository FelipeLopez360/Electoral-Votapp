package co.com.votapp.ws.auth.domain.port.out;

import co.com.votapp.ws.auth.domain.Funcionario;

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

    /** Returns all funcionarios. If {@code search} is blank or null, returns all. */
    List<Funcionario> findAll(String search);

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
}
