package co.com.votapp.ws.auth.infrastructure.adapter.in.web;

import co.com.votapp.ws.auth.domain.Funcionario;
import co.com.votapp.ws.auth.domain.port.in.CreateFuncionarioUseCase;
import co.com.votapp.ws.auth.domain.port.in.UpdateFuncionarioUseCase;
import co.com.votapp.ws.auth.domain.port.out.FuncionarioRepositoryPort;
import co.com.votapp.ws.auth.domain.port.out.PortalSessionPort;
import co.com.votapp.ws.common.exception.DomainException;
import co.com.votapp.ws.common.exception.GlobalExceptionHandler;
import co.com.votapp.ws.common.exception.NotFoundException;
import co.com.votapp.ws.common.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import co.com.votapp.ws.common.config.SecurityConfig;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Spring MVC slice tests for {@link FuncionarioController}.
 *
 * <p>Uses {@code @WebMvcTest} to load the real Spring MVC + Security stack including
 * {@link GlobalExceptionHandler}. This proves the actual HTTP status codes (404, 400, 409)
 * that the verify report required as runtime contract evidence.
 *
 * <p>Complements {@link FuncionarioControllerTest} (pure Mockito, no Spring context).
 * This class focuses on the error-status contract and Spring MVC wire-up.
 *
 * <p>Named *WebMvcTest.java (not *IT.java) to run under surefire ({@code ./mvnw test})
 * since it uses no Testcontainers and requires no external infrastructure.
 */
@DisplayName("FuncionarioController - HTTP contract (WebMvcTest + GlobalExceptionHandler)")
@WebMvcTest(FuncionarioController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class FuncionarioControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FuncionarioRepositoryPort funcionarioRepository;
    @MockitoBean
    private CreateFuncionarioUseCase createFuncionarioUseCase;
    @MockitoBean
    private UpdateFuncionarioUseCase updateFuncionarioUseCase;
    // Required by SecurityConfig.portalAuthFilter bean (portal session support)
    @MockitoBean
    private PortalSessionPort portalSessionPort;

    // ── Fixture ───────────────────────────────────────────────────────────────

    private static Funcionario sampleFuncionario(int id) {
        return new Funcionario(
                id, "EMP-00" + id, "1000000" + id,
                "Ana", "García", "CC", "ana@test.com",
                null, 1, 2, null, true, "ACTIVO", true
        );
    }

    // ── GET /api/v1/funcionarios/{id} → 404 when not found ────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return HTTP 404 when GET detail for non-existent funcionario")
    void get_shouldReturn404_whenFuncionarioDoesNotExist() throws Exception {
        // Given
        when(funcionarioRepository.findById(999)).thenReturn(Optional.empty());

        // When & Then — GlobalExceptionHandler must translate NotFoundException → 404
        mockMvc.perform(get("/api/v1/funcionarios/999"))
                .andExpect(status().isNotFound());
    }

    // ── POST /api/v1/funcionarios → 400 when validation fails ─────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return HTTP 400 when POST create fails validation")
    void create_shouldReturn400_whenValidationFails() throws Exception {
        // Given
        when(createFuncionarioUseCase.create(any()))
                .thenThrow(new ValidationException("nombres es obligatorio"));

        String body = """
                {
                  "nombres": "",
                  "apellidos": "García",
                  "documento_identidad": "10000001",
                  "estado_laboral": "ACTIVO",
                  "puede_votar": true
                }
                """;

        // When & Then — GlobalExceptionHandler must translate ValidationException → 400
        mockMvc.perform(post("/api/v1/funcionarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("nombres es obligatorio"));
    }

    // ── POST /api/v1/funcionarios → 409 when documento is duplicate ───────────

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return HTTP 409 when POST create finds duplicate documentoIdentidad")
    void create_shouldReturn409_whenDocumentoIsDuplicate() throws Exception {
        // Given
        when(createFuncionarioUseCase.create(any()))
                .thenThrow(new DomainException("10000001 ya existe"));

        String body = """
                {
                  "nombres": "Ana",
                  "apellidos": "García",
                  "documento_identidad": "10000001",
                  "estado_laboral": "ACTIVO",
                  "puede_votar": true
                }
                """;

        // When & Then — GlobalExceptionHandler must translate DomainException → 409
        mockMvc.perform(post("/api/v1/funcionarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("10000001 ya existe"));
    }

    // ── PUT /api/v1/funcionarios/{id} → 404 when not found ───────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return HTTP 404 when PUT update targets non-existent funcionario")
    void update_shouldReturn404_whenFuncionarioDoesNotExist() throws Exception {
        // Given
        when(updateFuncionarioUseCase.update(any()))
                .thenThrow(new NotFoundException("Funcionario 999 no encontrado"));

        String body = """
                {
                  "nombres": "Nuevo Nombre"
                }
                """;

        // When & Then — GlobalExceptionHandler must translate NotFoundException → 404
        mockMvc.perform(put("/api/v1/funcionarios/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Funcionario 999 no encontrado"));
    }

    // ── PUT /api/v1/funcionarios/{id} → debeCambiarPassword=false in response ─

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return HTTP 200 with debeCambiarPassword=false when admin clears flag")
    void update_shouldReturn200WithDebeCambiarPasswordFalse_whenAdminClearsFlag() throws Exception {
        // Given — use case returns a funcionario with debeCambiarPassword=false
        Funcionario passwordCleared = new Funcionario(
                3, "EMP-003", "30000003",
                "Luis", "Torres", "CC", "luis@test.com",
                null, 1, 2, null, true, "ACTIVO", false  // debeCambiarPassword=false
        );
        when(updateFuncionarioUseCase.update(any())).thenReturn(passwordCleared);

        String body = """
                {
                  "debe_cambiar_password": false
                }
                """;

        // When & Then — 200 response; debeCambiarPassword=false is a primitive boolean
        // so it IS serialised in the response (NON_NULL only suppresses null, not false)
        mockMvc.perform(put("/api/v1/funcionarios/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.debeCambiarPassword").value(false));
    }

    // ── POST /api/v1/funcionarios → 201 success with temporaryPassword ────────

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return HTTP 201 with temporaryPassword when create succeeds")
    void create_shouldReturn201WithTemporaryPassword_whenValid() throws Exception {
        // Given
        Funcionario saved = sampleFuncionario(10);
        CreateFuncionarioUseCase.Result result = new CreateFuncionarioUseCase.Result(saved, "Temp@12345!");
        when(createFuncionarioUseCase.create(any())).thenReturn(result);

        String body = """
                {
                  "nombres": "Ana",
                  "apellidos": "García",
                  "documento_identidad": "10000001",
                  "estado_laboral": "ACTIVO",
                  "puede_votar": true
                }
                """;

        // When & Then
        mockMvc.perform(post("/api/v1/funcionarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.temporaryPassword").value("Temp@12345!"));
    }
}
