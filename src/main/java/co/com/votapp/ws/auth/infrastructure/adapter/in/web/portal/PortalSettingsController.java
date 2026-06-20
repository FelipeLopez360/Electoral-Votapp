package co.com.votapp.ws.auth.infrastructure.adapter.in.web.portal;

import co.com.votapp.ws.auth.domain.Funcionario;
import co.com.votapp.ws.auth.domain.port.in.ChangePasswordUseCase;
import co.com.votapp.ws.auth.domain.port.in.LogoutUseCase;
import co.com.votapp.ws.auth.domain.port.in.UpdateProfileUseCase;
import co.com.votapp.ws.auth.domain.port.out.FuncionarioRepositoryPort;
import co.com.votapp.ws.common.exception.NotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST adapter for portal self-service settings.
 *
 * <p>All endpoints under {@code /api/v1/portal/**} are protected by
 * {@link co.com.votapp.ws.auth.infrastructure.adapter.in.web.PortalAuthFilter},
 * which resolves the funcionario ID from the Bearer session token and stores it
 * as a request attribute ({@code portalFuncionarioId}).
 *
 * <p>DTOs are inline {@code record}s — no separate DTO files per portal convention
 * (matches {@link PortalAuthController} style).
 *
 * <p>No application-service layer — these are single-step flows that call use cases directly
 * (per design decision #2).
 */
@RestController
@RequestMapping("/api/v1/portal")
@Tag(name = "Portal Settings", description = "Funcionario self-service: password, profile, logout")
public class PortalSettingsController {

    private static final String FUNCIONARIO_ID_ATTR = "portalFuncionarioId";
    private static final String BEARER_PREFIX = "Bearer ";

    private final ChangePasswordUseCase changePasswordUseCase;
    private final UpdateProfileUseCase updateProfileUseCase;
    private final LogoutUseCase logoutUseCase;
    private final FuncionarioRepositoryPort funcionarioRepository;

    public PortalSettingsController(ChangePasswordUseCase changePasswordUseCase,
                                    UpdateProfileUseCase updateProfileUseCase,
                                    LogoutUseCase logoutUseCase,
                                    FuncionarioRepositoryPort funcionarioRepository) {
        this.changePasswordUseCase = changePasswordUseCase;
        this.updateProfileUseCase = updateProfileUseCase;
        this.logoutUseCase = logoutUseCase;
        this.funcionarioRepository = funcionarioRepository;
    }

    // ─── PUT /api/v1/portal/password ─────────────────────────────────────────

    @PutMapping("/password")
    @Operation(
            summary = "Change password",
            description = "Changes the authenticated funcionario's password. "
                    + "Requires the correct current password. "
                    + "New password must meet the strength policy (min 8 chars, 1 upper, 1 lower, 1 digit)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Password changed successfully"),
            @ApiResponse(responseCode = "401", description = "Current password is incorrect"),
            @ApiResponse(responseCode = "409", description = "New password does not meet strength policy")
    })
    public ResponseEntity<Void> changePassword(@RequestBody PasswordChangeRequest request,
                                                HttpServletRequest httpRequest) {
        // Validate confirmNewPassword matches newPassword (client-side typo prevention)
        if (!request.newPassword().equals(request.confirmNewPassword())) {
            return ResponseEntity.badRequest().build();
        }
        String documentoIdentidad = resolveDocumento(httpRequest);
        changePasswordUseCase.change(documentoIdentidad, request.currentPassword(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    // ─── GET /api/v1/portal/me ────────────────────────────────────────────────

    @GetMapping("/me")
    @Operation(
            summary = "Get profile",
            description = "Returns the authenticated funcionario's profile data."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile returned successfully")
    })
    public ResponseEntity<ProfileResponse> getMe(HttpServletRequest httpRequest) {
        Integer funcionarioId = (Integer) httpRequest.getAttribute(FUNCIONARIO_ID_ATTR);
        Funcionario funcionario = funcionarioRepository.findById(funcionarioId)
                .orElseThrow(() -> new NotFoundException("Funcionario not found"));
        return ResponseEntity.ok(ProfileResponse.from(funcionario));
    }

    // ─── PUT /api/v1/portal/me ────────────────────────────────────────────────

    @PutMapping("/me")
    @Operation(
            summary = "Update profile",
            description = "Updates the authenticated funcionario's email and phone number. "
                    + "Other fields (nombres, apellidos, documentoIdentidad, numeroEmpleado) are read-only."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile updated successfully")
    })
    public ResponseEntity<ProfileResponse> updateMe(@RequestBody ProfileUpdateRequest request,
                                                    HttpServletRequest httpRequest) {
        String documentoIdentidad = resolveDocumento(httpRequest);
        Funcionario updated = updateProfileUseCase.update(documentoIdentidad, request.email(), request.telefono());
        return ResponseEntity.ok(ProfileResponse.from(updated));
    }

    // ─── POST /api/v1/portal/logout ───────────────────────────────────────────

    @PostMapping("/logout")
    @Operation(
            summary = "Logout",
            description = "Invalidates the current portal session. "
                    + "The raw Bearer token is read from the Authorization header."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Session invalidated successfully")
    })
    public ResponseEntity<Void> logout(HttpServletRequest httpRequest) {
        String authHeader = httpRequest.getHeader("Authorization");
        String sessionToken = (authHeader != null && authHeader.startsWith(BEARER_PREFIX))
                ? authHeader.substring(BEARER_PREFIX.length()).trim()
                : "";
        logoutUseCase.logout(sessionToken);
        return ResponseEntity.noContent().build();
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    /**
     * Resolves the {@code documentoIdentidad} for the authenticated funcionario
     * by reading the {@code portalFuncionarioId} request attribute (set by {@link
     * co.com.votapp.ws.auth.infrastructure.adapter.in.web.PortalAuthFilter})
     * and loading the domain object.
     */
    private String resolveDocumento(HttpServletRequest httpRequest) {
        Integer funcionarioId = (Integer) httpRequest.getAttribute(FUNCIONARIO_ID_ATTR);
        return funcionarioRepository.findById(funcionarioId)
                .orElseThrow(() -> new NotFoundException("Funcionario not found"))
                .getDocumentoIdentidad();
    }

    // ─── Inline DTOs (portal convention — no separate DTO files) ─────────────

    /** Request body for PUT /password. */
    public record PasswordChangeRequest(String currentPassword, String newPassword, String confirmNewPassword) {}

    /** Request body for PUT /me. */
    public record ProfileUpdateRequest(String email, String telefono) {}

    /** Response body for GET /me and PUT /me. */
    public record ProfileResponse(
            String nombres,
            String apellidos,
            String documentoIdentidad,
            String email,
            String telefono,
            String numeroEmpleado
    ) {
        public static ProfileResponse from(Funcionario f) {
            return new ProfileResponse(
                    f.getNombres(),
                    f.getApellidos(),
                    f.getDocumentoIdentidad(),
                    f.getEmail(),
                    f.getTelefono(),
                    f.getNumeroEmpleado()
            );
        }
    }
}
