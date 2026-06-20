package co.com.votapp.ws.auth.infrastructure.adapter.in.web;

import co.com.votapp.ws.auth.application.dto.FuncionarioResponse;
import co.com.votapp.ws.auth.domain.Funcionario;
import co.com.votapp.ws.auth.domain.port.in.CreateFuncionarioUseCase;
import co.com.votapp.ws.auth.domain.port.in.UpdateFuncionarioUseCase;
import co.com.votapp.ws.auth.domain.port.out.FuncionarioRepositoryPort;
import co.com.votapp.ws.common.domain.model.PageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TDD RED cycle for the paginated list endpoint in {@link FuncionarioController}.
 *
 * <p>Written BEFORE the controller and port changes. Tests cover default pagination
 * (page=0, size=8), explicit parameters, search pass-through, and clamping.
 */
@DisplayName("FuncionarioController - Paginated list endpoint")
@ExtendWith(MockitoExtension.class)
class FuncionarioControllerPaginationTest {

    @Mock private FuncionarioRepositoryPort funcionarioRepository;
    @Mock private CreateFuncionarioUseCase createFuncionarioUseCase;
    @Mock private UpdateFuncionarioUseCase updateFuncionarioUseCase;

    private FuncionarioController controller;

    private static final Funcionario SAMPLE = new Funcionario(
            1, "EMP000001", "10000001", "Ana", "García", "CC",
            "ana@test.com", null, 1, 2, null, true, "ACTIVO", false
    );

    @BeforeEach
    void setUp() {
        controller = new FuncionarioController(
                funcionarioRepository, createFuncionarioUseCase, updateFuncionarioUseCase
        );
    }

    private PageResult<Funcionario> pageOf(Funcionario... items) {
        return new PageResult<>(List.of(items), 0, 8, items.length, 1);
    }

    // ─── Default pagination ────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return 200 with PageResult defaulting to page=0 and size=8")
    void list_shouldReturn200WithPageResult_whenDefaultParams() {
        // Given
        when(funcionarioRepository.findAll(eq(0), eq(8), eq(null)))
                .thenReturn(pageOf(SAMPLE));

        // When
        ResponseEntity<PageResult<FuncionarioResponse>> response = controller.list(null, null, null);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        PageResult<FuncionarioResponse> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.page()).isEqualTo(0);
        assertThat(body.size()).isEqualTo(8);
        assertThat(body.content()).hasSize(1);
        assertThat(body.content().get(0).nombres()).isEqualTo("Ana");
    }

    @Test
    @DisplayName("Should pass explicit page and size to repository")
    void list_shouldPassExplicitPageAndSize_toRepository() {
        // Given
        PageResult<Funcionario> pageResult = new PageResult<>(List.of(), 1, 5, 0L, 0);
        when(funcionarioRepository.findAll(eq(1), eq(5), eq(null))).thenReturn(pageResult);

        // When
        controller.list(1, 5, null);

        // Then
        ArgumentCaptor<Integer> pageCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> sizeCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(funcionarioRepository).findAll(pageCaptor.capture(), sizeCaptor.capture(), any());
        assertThat(pageCaptor.getValue()).isEqualTo(1);
        assertThat(sizeCaptor.getValue()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should clamp size to 8 when size=0 (floor clamping)")
    void list_shouldClampSizeToMin_whenSizeIsZero() {
        // Given
        when(funcionarioRepository.findAll(eq(0), eq(1), any())).thenReturn(pageOf());

        // When
        controller.list(0, 0, null);

        // Then
        ArgumentCaptor<Integer> sizeCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(funcionarioRepository).findAll(anyInt(), sizeCaptor.capture(), any());
        assertThat(sizeCaptor.getValue()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("Should clamp size to 100 when size exceeds 100")
    void list_shouldClampSizeToMax_whenSizeExceeds100() {
        // Given
        when(funcionarioRepository.findAll(eq(0), eq(100), any())).thenReturn(pageOf());

        // When
        controller.list(0, 200, null);

        // Then
        ArgumentCaptor<Integer> sizeCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(funcionarioRepository).findAll(anyInt(), sizeCaptor.capture(), any());
        assertThat(sizeCaptor.getValue()).isEqualTo(100);
    }

    @Test
    @DisplayName("Should clamp page to 0 when page is negative")
    void list_shouldClampPageToZero_whenPageIsNegative() {
        // Given
        when(funcionarioRepository.findAll(eq(0), anyInt(), any())).thenReturn(pageOf());

        // When
        controller.list(-5, 8, null);

        // Then
        ArgumentCaptor<Integer> pageCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(funcionarioRepository).findAll(pageCaptor.capture(), anyInt(), any());
        assertThat(pageCaptor.getValue()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should pass search term to repository and return filtered PageResult")
    void list_shouldPassSearchTerm_andReturnFilteredPageResult() {
        // Given
        PageResult<Funcionario> filtered = new PageResult<>(List.of(SAMPLE), 0, 8, 1L, 1);
        when(funcionarioRepository.findAll(eq(0), eq(8), eq("Ana"))).thenReturn(filtered);

        // When
        ResponseEntity<PageResult<FuncionarioResponse>> response = controller.list(null, null, "Ana");

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        PageResult<FuncionarioResponse> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.totalElements()).isEqualTo(1L);
        assertThat(body.content().get(0).nombres()).isEqualTo("Ana");
        verify(funcionarioRepository).findAll(eq(0), eq(8), eq("Ana"));
    }

    @Test
    @DisplayName("Should return empty PageResult when no funcionarios match search")
    void list_shouldReturnEmptyPageResult_whenNoMatchesFound() {
        // Given
        PageResult<Funcionario> empty = PageResult.empty(0, 8);
        when(funcionarioRepository.findAll(eq(0), eq(8), eq("ZZZZZ"))).thenReturn(empty);

        // When
        ResponseEntity<PageResult<FuncionarioResponse>> response = controller.list(null, null, "ZZZZZ");

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().content()).isEmpty();
        assertThat(response.getBody().totalElements()).isEqualTo(0L);
    }
}
