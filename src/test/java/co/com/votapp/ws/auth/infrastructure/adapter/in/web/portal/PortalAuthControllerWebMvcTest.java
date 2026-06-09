package co.com.votapp.ws.auth.infrastructure.adapter.in.web.portal;

import co.com.votapp.ws.auth.domain.Funcionario;
import co.com.votapp.ws.auth.domain.exception.AccountInactiveException;
import co.com.votapp.ws.auth.domain.exception.AccountLockedException;
import co.com.votapp.ws.auth.domain.exception.InvalidCredentialsException;
import co.com.votapp.ws.auth.domain.port.in.AuthenticateFuncionarioPort;
import co.com.votapp.ws.auth.domain.port.out.PortalSessionPort;
import co.com.votapp.ws.common.config.SecurityConfig;
import co.com.votapp.ws.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Task 3.1 + 3.2 — WebMvc slice test for {@link PortalAuthController}.
 *
 * <p>RED phase: tests fail until the controller, filter chain, and exception handlers exist.
 */
@DisplayName("PortalAuthController - POST /api/v1/portal/login")
@WebMvcTest(PortalAuthController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class PortalAuthControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticateFuncionarioPort authenticatePort;

    @MockitoBean
    private PortalSessionPort sessionPort;

    // ── Helper ────────────────────────────────────────────────────────────────

    private static Funcionario activeFuncionario() {
        return new Funcionario(42, "EMP001", "12345678",
                "Juan", "Pérez", "CC", "juan@test.co",
                null, null, null, null, true, "ACTIVO", false);
    }

    private static final String LOGIN_BODY = """
            {
              "documentoIdentidad": "12345678",
              "password": "TestPass123!"
            }
            """;

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return 200 with sessionToken and funcionario info on successful login")
    void login_shouldReturn200WithSession_whenCredentialsAreValid() throws Exception {
        // Given
        when(authenticatePort.authenticate("12345678", "TestPass123!"))
                .thenReturn(activeFuncionario());
        when(sessionPort.createSession(42)).thenReturn("test-session-uuid-123");

        // When & Then
        mockMvc.perform(post("/api/v1/portal/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionToken").value("test-session-uuid-123"))
                .andExpect(jsonPath("$.funcionarioId").value(42))
                .andExpect(jsonPath("$.nombre").exists());
    }

    // ── Auth errors ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return 401 when credentials are invalid")
    void login_shouldReturn401_whenCredentialsAreInvalid() throws Exception {
        // Given
        when(authenticatePort.authenticate(anyString(), anyString()))
                .thenThrow(new InvalidCredentialsException());

        // When & Then
        mockMvc.perform(post("/api/v1/portal/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Credenciales inválidas"));
    }

    @Test
    @DisplayName("Should return 423 Locked when account is temporarily locked")
    void login_shouldReturn423_whenAccountIsLocked() throws Exception {
        // Given
        when(authenticatePort.authenticate(anyString(), anyString()))
                .thenThrow(new AccountLockedException());

        // When & Then
        mockMvc.perform(post("/api/v1/portal/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.message").value("Cuenta bloqueada temporalmente. Intentá de nuevo más tarde."));
    }

    @Test
    @DisplayName("Should return 403 Forbidden when account is inactive")
    void login_shouldReturn403_whenAccountIsInactive() throws Exception {
        // Given
        when(authenticatePort.authenticate(anyString(), anyString()))
                .thenThrow(new AccountInactiveException());

        // When & Then
        mockMvc.perform(post("/api/v1/portal/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Funcionario inactivo"));
    }
}
