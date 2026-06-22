package co.com.votapp.ws.fileupload.infrastructure.adapter.in.web;

import co.com.votapp.ws.auth.domain.port.out.PortalSessionPort;
import co.com.votapp.ws.common.config.SecurityConfig;
import co.com.votapp.ws.common.exception.GlobalExceptionHandler;
import co.com.votapp.ws.common.exception.ValidationException;
import co.com.votapp.ws.fileupload.application.FileUploadAppService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Spring MVC slice tests for {@link FileUploadController}.
 *
 * <p>TDD RED cycle: written BEFORE production controller and app service exist.
 * Named *IT.java — runs under failsafe (uses Spring context via @WebMvcTest).
 *
 * <p>Verifies:
 * <ul>
 *   <li>201 + JSON {@code {url: "..."}} on valid multipart JPEG upload.</li>
 *   <li>400 when the file is too large (delegated via ValidationException).</li>
 *   <li>400 when the content type is unsupported (delegated via ValidationException).</li>
 *   <li>401 when no authentication is provided.</li>
 * </ul>
 */
@DisplayName("FileUploadController - POST /api/v1/files/upload (WebMvcTest)")
@WebMvcTest(FileUploadController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class FileUploadControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FileUploadAppService fileUploadAppService;

    // Required by SecurityConfig.portalAuthFilter bean
    @MockitoBean
    private PortalSessionPort portalSessionPort;

    // ── 201 success ───────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 201 with url when valid JPEG is uploaded")
    void upload_shouldReturn201WithUrl_whenValidJpegProvided() throws Exception {
        // Given
        byte[] jpegBytes = new byte[1024];
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", jpegBytes);
        when(fileUploadAppService.upload(any())).thenReturn("/files/uuid-photo.jpg");

        // When & Then
        mockMvc.perform(multipart("/api/v1/files/upload")
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url").value("/files/uuid-photo.jpg"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 201 with url when valid PNG is uploaded")
    void upload_shouldReturn201WithUrl_whenValidPngProvided() throws Exception {
        // Given
        byte[] pngBytes = new byte[2048];
        MockMultipartFile file = new MockMultipartFile(
                "file", "logo.png", "image/png", pngBytes);
        when(fileUploadAppService.upload(any())).thenReturn("/files/uuid-logo.png");

        // When & Then
        mockMvc.perform(multipart("/api/v1/files/upload")
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url").value("/files/uuid-logo.png"));
    }

    // ── 400 oversized ─────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 400 when ValidationException thrown for oversized file")
    void upload_shouldReturn400_whenFileTooLarge() throws Exception {
        // Given
        byte[] oversized = new byte[1024];
        MockMultipartFile file = new MockMultipartFile(
                "file", "big.jpg", "image/jpeg", oversized);
        when(fileUploadAppService.upload(any()))
                .thenThrow(new ValidationException("File size exceeds maximum allowed size"));

        // When & Then
        mockMvc.perform(multipart("/api/v1/files/upload")
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    // ── 400 invalid content type ──────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 400 when ValidationException thrown for invalid content type")
    void upload_shouldReturn400_whenContentTypeNotSupported() throws Exception {
        // Given
        byte[] pdfBytes = new byte[512];
        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", pdfBytes);
        when(fileUploadAppService.upload(any()))
                .thenThrow(new ValidationException("Unsupported content type"));

        // When & Then
        mockMvc.perform(multipart("/api/v1/files/upload")
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    // ── 401 unauthenticated ───────────────────────────────────────────────────

    @Test
    @DisplayName("Should return 401 when no authentication is provided")
    void upload_shouldReturn401_whenNotAuthenticated() throws Exception {
        // Given — no @WithMockUser
        byte[] bytes = new byte[512];
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", bytes);

        // When & Then
        mockMvc.perform(multipart("/api/v1/files/upload")
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isUnauthorized());
    }
}
