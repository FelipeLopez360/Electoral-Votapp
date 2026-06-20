package co.com.votapp.ws.auth.infrastructure.adapter.out.persistence;

import co.com.votapp.ws.auth.domain.Funcionario;
import co.com.votapp.ws.common.domain.model.PageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TDD RED cycle for the paginated {@code findAll(int page, int size, String search)}
 * method in {@link FuncionarioRepositoryAdapter}.
 *
 * <p>Written BEFORE the paginated adapter implementation.
 * Mirrors the pattern in {@link CensoRepositoryAdapterTest}.
 */
@DisplayName("FuncionarioRepositoryAdapter - Paginated findAll unit tests")
@ExtendWith(MockitoExtension.class)
class FuncionarioRepositoryAdapterPaginationTest {

    @Mock
    private FuncionarioJpaRepository jpaRepository;

    private FuncionarioRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new FuncionarioRepositoryAdapter(jpaRepository);
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private FuncionarioEntity entity(int id, String nombres) {
        FuncionarioEntity e = new FuncionarioEntity();
        e.setId(id);
        e.setNumeroEmpleado("EMP%06d".formatted(id));
        e.setDocumentoIdentidad("DOC" + id);
        e.setNombres(nombres);
        e.setApellidos("Apellido");
        e.setTipoDocumento("CC");
        e.setPasswordHash("$2a$hash");
        e.setPuedeVotar(true);
        e.setEstadoLaboral("ACTIVO");
        e.setDebeCambiarPassword(false);
        return e;
    }

    // ─── findAll(page, size, search) ──────────────────────────────────────────

    @Test
    @DisplayName("Should return PageResult mapped from Spring Page when funcionarios exist")
    void findAll_shouldReturnPageResult_whenFuncionariosExist() {
        // Given
        FuncionarioEntity e1 = entity(1, "Ana");
        FuncionarioEntity e2 = entity(2, "Luis");
        Page<FuncionarioEntity> page = new PageImpl<>(List.of(e1, e2), PageRequest.of(0, 8), 2L);
        when(jpaRepository.search(eq(""), any(Pageable.class))).thenReturn(page);

        // When
        PageResult<Funcionario> result = adapter.findAll(0, 8, null);

        // Then
        assertThat(result.page()).isEqualTo(0);
        assertThat(result.size()).isEqualTo(8);
        assertThat(result.totalElements()).isEqualTo(2L);
        assertThat(result.totalPages()).isEqualTo(1);
        assertThat(result.content()).hasSize(2);
        assertThat(result.content().get(0).getNombres()).isEqualTo("Ana");
        assertThat(result.content().get(1).getNombres()).isEqualTo("Luis");
    }

    @Test
    @DisplayName("Should pass search term to JPA repository when search is provided")
    void findAll_shouldPassSearchTerm_toJpaRepository() {
        // Given
        FuncionarioEntity e = entity(1, "Ana");
        Page<FuncionarioEntity> page = new PageImpl<>(List.of(e), PageRequest.of(0, 8), 1L);
        when(jpaRepository.search(eq("Ana"), any(Pageable.class))).thenReturn(page);

        // When
        adapter.findAll(0, 8, "Ana");

        // Then — verify search term was passed through
        ArgumentCaptor<String> searchCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(jpaRepository).search(searchCaptor.capture(), pageableCaptor.capture());
        assertThat(searchCaptor.getValue()).isEqualTo("Ana");
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(8);
    }

    @Test
    @DisplayName("Should return empty PageResult when no funcionarios match search")
    void findAll_shouldReturnEmptyPageResult_whenNoMatchesFound() {
        // Given
        Page<FuncionarioEntity> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 8), 0L);
        when(jpaRepository.search(eq("NoExiste"), any(Pageable.class))).thenReturn(emptyPage);

        // When
        PageResult<Funcionario> result = adapter.findAll(0, 8, "NoExiste");

        // Then
        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isEqualTo(0L);
        assertThat(result.totalPages()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should use empty string as search when null is passed")
    void findAll_shouldUseEmptyString_whenSearchIsNull() {
        // Given
        Page<FuncionarioEntity> page = new PageImpl<>(List.of(), PageRequest.of(0, 8), 0L);
        when(jpaRepository.search(eq(""), any(Pageable.class))).thenReturn(page);

        // When
        adapter.findAll(0, 8, null);

        // Then
        ArgumentCaptor<String> searchCaptor = ArgumentCaptor.forClass(String.class);
        verify(jpaRepository).search(searchCaptor.capture(), any(Pageable.class));
        assertThat(searchCaptor.getValue()).isEqualTo("");
    }

    @Test
    @DisplayName("Should construct PageRequest with correct page and size")
    void findAll_shouldBuildCorrectPageRequest_withPageAndSize() {
        // Given
        Page<FuncionarioEntity> page = new PageImpl<>(List.of(), PageRequest.of(2, 5), 0L);
        when(jpaRepository.search(any(), any(Pageable.class))).thenReturn(page);

        // When
        adapter.findAll(2, 5, "");

        // Then
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(jpaRepository).search(any(), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(5);
    }
}
