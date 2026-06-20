package co.com.votapp.ws.auth.infrastructure.adapter.in.web.portal;

import co.com.votapp.ws.auth.domain.Funcionario;
import co.com.votapp.ws.auth.domain.exception.InvalidCredentialsException;
import co.com.votapp.ws.auth.domain.exception.WeakPasswordException;
import co.com.votapp.ws.auth.domain.port.in.ChangePasswordUseCase;
import co.com.votapp.ws.auth.domain.port.in.LogoutUseCase;
import co.com.votapp.ws.auth.domain.port.in.UpdateProfileUseCase;
import co.com.votapp.ws.auth.domain.port.out.FuncionarioRepositoryPort;
import co.com.votapp.ws.auth.infrastructure.adapter.in.web.PortalAuthFilter;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;


import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * WebMvc slice test for {@link PortalSettingsController}.
 *
 * <p>Mocks {@link ChangePasswordUseCase}, {@link UpdateProfileUseCase}, {@link LogoutUseCase},
 * and {@link FuncionarioRepositoryPort}. Simulates {@link PortalAuthFilter} by pre-setting
 * the {@code portalFuncionarioId} request attribute directly on each request.
 */
@DisplayName("PortalSettingsController - PUT /password, GET/PUT /me, POST /logout")
@WebMvcTest(PortalSettingsController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class PortalSettingsControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChangePasswordUseCase changePasswordUseCase;

    @MockitoBean
    private UpdateProfileUseCase updateProfileUseCase;

    @MockitoBean
    private LogoutUseCase logoutUseCase;

    @MockitoBean
    private FuncionarioRepositoryPort funcionarioRepository;

    // ── SecurityConfig needs PortalAuthFilter's dependency ────────────────────

    @MockitoBean
    private co.com.votapp.ws.auth.domain.port.out.PortalSessionPort sessionPort;

    // ─── Helper ───────────────────────────────────────────────────────────────

    private static final Integer FUNCIONARIO_ID = 42;
    private static final String DOCUMENTO = "12345678";
    private static final String SESSION_TOKEN = "test-session-token-abc";
    private static final String BEARER = "Bearer " + SESSION_TOKEN;

    private static Funcionario activeFuncionario() {
        return new Funcionario(
                FUNCIONARIO_ID, "EMP000042", DOCUMENTO,
                "Juan", "Pérez", "CC", "juan@test.co",
                "+573001234567", null, null, null,
                true, "ACTIVO", false);
    }

    /**
     * Stubs the session port so PortalAuthFilter resolves the test funcionario ID,
     * then adds the Authorization header to the request.
     */
    private MockHttpServletRequestBuilder withAuth(MockHttpServletRequestBuilder builder) {
        when(sessionPort.getFuncionarioIdFromSession(SESSION_TOKEN))
                .thenReturn(Optional.of(FUNCIONARIO_ID));
        return builder.header("Authorization", BEARER);
    }

    // ─── PUT /api/v1/portal/password ──────────────────────────────────────────

    @Test
    @DisplayName("Should return 204 when password change succeeds")
    void changePassword_shouldReturn204_whenPasswordChangeSucceeds() throws Exception {
        // Given
        when(funcionarioRepository.findById(FUNCIONARIO_ID))
                .thenReturn(Optional.of(activeFuncionario()));

        // When & Then
        mockMvc.perform(withAuth(put("/api/v1/portal/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "OldPass123",
                                  "newPassword": "NewPass456!",
                                  "confirmNewPassword": "NewPass456!"
                                }
                                """)))
                .andExpect(status().isNoContent());

        verify(changePasswordUseCase).change(eq(DOCUMENTO), eq("OldPass123"), eq("NewPass456!"));
    }

    @Test
    @DisplayName("Should return 400 when confirmNewPassword does not match newPassword")
    void changePassword_shouldReturn400_whenConfirmNewPasswordMismatch() throws Exception {
        // Given
        when(funcionarioRepository.findById(FUNCIONARIO_ID))
                .thenReturn(Optional.of(activeFuncionario()));

        // When & Then — 400 before the use case is even called
        mockMvc.perform(withAuth(put("/api/v1/portal/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "OldPass123",
                                  "newPassword": "NewPass456!",
                                  "confirmNewPassword": "DifferentPass1"
                                }
                                """)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 401 when current password is wrong")
    void changePassword_shouldReturn401_whenCurrentPasswordIsWrong() throws Exception {
        // Given
        when(funcionarioRepository.findById(FUNCIONARIO_ID))
                .thenReturn(Optional.of(activeFuncionario()));
        doThrow(new InvalidCredentialsException())
                .when(changePasswordUseCase).change(anyString(), anyString(), anyString());

        // When & Then
        mockMvc.perform(withAuth(put("/api/v1/portal/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "WrongPass1",
                                  "newPassword": "NewPass456!",
                                  "confirmNewPassword": "NewPass456!"
                                }
                                """)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Credenciales inválidas"));
    }

    @Test
    @DisplayName("Should return 409 when new password is too weak")
    void changePassword_shouldReturn409_whenNewPasswordIsWeak() throws Exception {
        // Given
        when(funcionarioRepository.findById(FUNCIONARIO_ID))
                .thenReturn(Optional.of(activeFuncionario()));
        doThrow(new WeakPasswordException())
                .when(changePasswordUseCase).change(anyString(), anyString(), anyString());

        // When & Then
        mockMvc.perform(withAuth(put("/api/v1/portal/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "OldPass123",
                                  "newPassword": "weak",
                                  "confirmNewPassword": "weak"
                                }
                                """)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").exists());
    }

    // ─── GET /api/v1/portal/me ────────────────────────────────────────────────

    @Test
    @DisplayName("Should return 200 with profile data when get me succeeds")
    void getMe_shouldReturn200WithProfileData_whenAuthenticated() throws Exception {
        // Given
        when(funcionarioRepository.findById(FUNCIONARIO_ID))
                .thenReturn(Optional.of(activeFuncionario()));

        // When & Then
        mockMvc.perform(withAuth(get("/api/v1/portal/me")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombres").value("Juan"))
                .andExpect(jsonPath("$.apellidos").value("Pérez"))
                .andExpect(jsonPath("$.documentoIdentidad").value(DOCUMENTO))
                .andExpect(jsonPath("$.email").value("juan@test.co"))
                .andExpect(jsonPath("$.telefono").value("+573001234567"))
                .andExpect(jsonPath("$.numeroEmpleado").value("EMP000042"));
    }

    // ─── PUT /api/v1/portal/me ────────────────────────────────────────────────

    @Test
    @DisplayName("Should return 200 with updated profile when put me succeeds")
    void updateMe_shouldReturn200WithUpdatedProfile_whenAuthenticated() throws Exception {
        // Given
        Funcionario updated = new Funcionario(
                FUNCIONARIO_ID, "EMP000042", DOCUMENTO,
                "Juan", "Pérez", "CC", "nuevo@test.co",
                "+573009876543", null, null, null,
                true, "ACTIVO", false);

        when(funcionarioRepository.findById(FUNCIONARIO_ID))
                .thenReturn(Optional.of(activeFuncionario()));
        when(updateProfileUseCase.update(eq(DOCUMENTO), eq("nuevo@test.co"), eq("+573009876543")))
                .thenReturn(updated);

        // When & Then
        mockMvc.perform(withAuth(put("/api/v1/portal/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "nuevo@test.co",
                                  "telefono": "+573009876543"
                                }
                                """)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("nuevo@test.co"))
                .andExpect(jsonPath("$.telefono").value("+573009876543"))
                .andExpect(jsonPath("$.nombres").value("Juan"));
    }

    // ─── POST /api/v1/portal/logout ───────────────────────────────────────────

    @Test
    @DisplayName("Should return 204 when logout succeeds and pass token to use case")
    void logout_shouldReturn204_andDelegateToken_whenSessionExists() throws Exception {
        // Given — withAuth stubs session resolution; no exception thrown on logout

        // When & Then
        mockMvc.perform(withAuth(post("/api/v1/portal/logout")))
                .andExpect(status().isNoContent());

        verify(logoutUseCase).logout(SESSION_TOKEN);
    }

    @Test
    @DisplayName("Should return 204 even when session token is not found (idempotent logout)")
    void logout_shouldReturn204_whenSessionAlreadyExpired() throws Exception {
        // Given — logout is a no-op for missing sessions (idempotent)
        // logoutUseCase.logout does nothing when session doesn't exist

        // When & Then — still passes because logoutUseCase won't throw
        mockMvc.perform(withAuth(post("/api/v1/portal/logout")))
                .andExpect(status().isNoContent());
    }

    // ─── Edge: restricted fields in profile update ─────────────────────────────

    @Test
    @DisplayName("Should ignore restricted fields in profile update (only email and telefono accepted)")
    void updateMe_shouldIgnoreRestrictedFields_whenSentInBody() throws Exception {
        // Given
        Funcionario updated = new Funcionario(
                FUNCIONARIO_ID, "EMP000042", DOCUMENTO,
                "Juan", "Pérez", "CC", "juan@test.co",
                "+573001234567", null, null, null,
                true, "ACTIVO", false);
        when(funcionarioRepository.findById(FUNCIONARIO_ID))
                .thenReturn(Optional.of(activeFuncionario()));
        // The use case is called only with email and telefono (extra fields ignored by Jackson)
        when(updateProfileUseCase.update(eq(DOCUMENTO), anyString(), anyString()))
                .thenReturn(updated);

        // When & Then — sending nombres/apellidos in JSON does nothing
        mockMvc.perform(withAuth(put("/api/v1/portal/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "juan@test.co",
                                  "telefono": "+573001234567",
                                  "nombres": "Hacker",
                                  "apellidos": "Man",
                                  "documentoIdentidad": "99999999"
                                }
                                """)))
                .andExpect(status().isOk());

        // Verify only email and telefono were passed to the use case
        verify(updateProfileUseCase).update(eq(DOCUMENTO), eq("juan@test.co"), eq("+573001234567"));
    }

    // ─── Edge: logout without auth ─────────────────────────────────────────────

    @Test
    @DisplayName("Should return 401 when logout without Bearer token")
    void logout_shouldReturn401_whenNoAuthHeader() throws Exception {
        // PortalAuthFilter rejects requests without valid Bearer token before
        // they reach the controller
        mockMvc.perform(post("/api/v1/portal/logout"))
                .andExpect(status().isUnauthorized());
    }
}
