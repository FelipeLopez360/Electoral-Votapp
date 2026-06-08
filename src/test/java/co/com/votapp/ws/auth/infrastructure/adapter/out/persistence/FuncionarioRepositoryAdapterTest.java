package co.com.votapp.ws.auth.infrastructure.adapter.out.persistence;

import co.com.votapp.ws.auth.domain.Funcionario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link FuncionarioRepositoryAdapter}.
 *
 * <p>Tests the adapter's own logic (auto-generation of numeroEmpleado, mapping)
 * by mocking the JPA repository. No Spring context required.
 */
@DisplayName("FuncionarioRepositoryAdapter - Unit tests (adapter logic, no Spring context)")
@ExtendWith(MockitoExtension.class)
class FuncionarioRepositoryAdapterTest {

    @Mock
    private FuncionarioJpaRepository jpaRepository;

    private FuncionarioRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new FuncionarioRepositoryAdapter(jpaRepository);
    }

    // ─── Helper: builds a saved entity simulating what JPA returns after persist ────

    private FuncionarioEntity savedEntity(String documento, String numeroEmpleado) {
        FuncionarioEntity e = new FuncionarioEntity();
        e.setId(99);
        e.setNumeroEmpleado(numeroEmpleado);
        e.setDocumentoIdentidad(documento);
        e.setNombres("Test");
        e.setApellidos("User");
        e.setTipoDocumento("CC");
        e.setPasswordHash("$2a$hash");
        e.setPuedeVotar(true);
        e.setEstadoLaboral("ACTIVO");
        e.setDebeCambiarPassword(true);
        return e;
    }

    // ─── ROOT CAUSE FIX: auto-generation of numeroEmpleado ───────────────────────

    /**
     * ROOT CAUSE REGRESSION TEST.
     *
     * <p>When the domain passes null for numeroEmpleado (as CreateFuncionarioUseCaseImpl always does),
     * saveWithHash uses a two-step save:
     * <ol>
     *   <li>INSERT with a temporary UUID-based value (satisfies NOT NULL on first flush)</li>
     *   <li>UPDATE with the stable EMP%06d value derived from the DB-generated id</li>
     * </ol>
     * Before the fix this scenario caused HTTP 500 (NULL → NOT NULL constraint violation).
     */
    @Test
    @DisplayName("Should auto-generate numeroEmpleado when domain provides null — root cause fix (two-step save)")
    void saveWithHash_shouldAutoGenerateNumeroEmpleado_whenDomainPassesNull() {
        // Given — domain object with null numeroEmpleado (exact state from use case)
        Funcionario domainFuncionario = new Funcionario(
                null,          // id
                null,          // numeroEmpleado — NULL as the create use case intentionally passes
                "10000999",
                "Pedro",
                "Ramirez",
                "CC",
                "pedro@test.com",
                "3001234567",
                1,
                4,
                null,
                true,
                "ACTIVO",
                true
        );

        // First save: INSERT with temp UUID — returns entity with DB-generated id=1
        // Second save: UPDATE with EMP000001 — returns updated entity
        when(jpaRepository.save(any(FuncionarioEntity.class)))
                .thenAnswer(invocation -> {
                    FuncionarioEntity entity = invocation.getArgument(0);
                    if (entity.getId() == null) entity.setId(1);  // simulate DB id generation
                    return entity;
                });

        // When
        Funcionario saved = adapter.saveWithHash(domainFuncionario, "$2a$bcrypt-hash");

        // Then — two saves happened (TMP insert, then EMP update)
        ArgumentCaptor<FuncionarioEntity> captor = ArgumentCaptor.forClass(FuncionarioEntity.class);
        verify(jpaRepository, times(2)).save(captor.capture());

        List<FuncionarioEntity> allSaves = captor.getAllValues();
        // First call: temporary UUID-based number
        assertThat(allSaves.get(0).getNumeroEmpleado())
                .as("First save must use a temporary non-null value to satisfy the NOT NULL constraint")
                .isNotNull()
                .isNotBlank();
        // Second call: stable EMP-based number
        assertThat(allSaves.get(1).getNumeroEmpleado())
                .as("Second save must use the stable EMP%06d format derived from the DB id")
                .isNotNull()
                .startsWith("EMP");

        // Returned domain object carries the final stable value
        assertThat(saved.getNumeroEmpleado())
                .isNotNull()
                .startsWith("EMP");
    }

    @Test
    @DisplayName("Should preserve existing numeroEmpleado when domain provides a value — single save only")
    void saveWithHash_shouldPreserveNumeroEmpleado_whenDomainProvidesOne() {
        // Given — domain object already has an employee number (e.g. migration/seed scenario)
        Funcionario domainFuncionario = new Funcionario(
                null,
                "EMP-PREEXISTING-001",  // already set — adapter must NOT overwrite
                "20001111",
                "María",
                "López",
                "CC",
                "maria@test.com",
                null, null, null, null,
                true, "ACTIVO", true
        );

        when(jpaRepository.save(any(FuncionarioEntity.class)))
                .thenAnswer(invocation -> {
                    FuncionarioEntity e = invocation.getArgument(0);
                    e.setId(2);
                    return e;
                });

        // When
        adapter.saveWithHash(domainFuncionario, "$2a$hash");

        // Then — only ONE save (no two-step needed), and employee number is untouched
        ArgumentCaptor<FuncionarioEntity> captor = ArgumentCaptor.forClass(FuncionarioEntity.class);
        verify(jpaRepository, times(1)).save(captor.capture());

        assertThat(captor.getValue().getNumeroEmpleado())
                .isEqualTo("EMP-PREEXISTING-001");
    }

    @Test
    @DisplayName("Should use EMP%06d format for final stable numeroEmpleado — e.g. EMP000001 for id=1")
    void saveWithHash_generatedNumeroEmpleado_shouldFollowEmpFormat() {
        // Given
        Funcionario funcionario = new Funcionario(
                null, null, "10000999",
                "Test", "User", "CC", "t@test.com",
                null, null, null, null, true, "ACTIVO", true
        );

        when(jpaRepository.save(any(FuncionarioEntity.class)))
                .thenAnswer(invocation -> {
                    FuncionarioEntity e = invocation.getArgument(0);
                    if (e.getId() == null) e.setId(42);  // DB gives id=42
                    return e;
                });

        // When
        adapter.saveWithHash(funcionario, "$2a$hash");

        // Then — second save carries EMP000042
        ArgumentCaptor<FuncionarioEntity> captor = ArgumentCaptor.forClass(FuncionarioEntity.class);
        verify(jpaRepository, times(2)).save(captor.capture());

        assertThat(captor.getAllValues().get(1).getNumeroEmpleado())
                .isEqualTo("EMP000042");
    }
}
