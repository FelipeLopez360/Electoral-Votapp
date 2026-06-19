package co.com.votapp.ws.voting.infrastructure.adapter.in.web;

import co.com.votapp.ws.auth.domain.port.out.PortalSessionPort;
import co.com.votapp.ws.common.config.SecurityConfig;
import co.com.votapp.ws.common.exception.GlobalExceptionHandler;
import co.com.votapp.ws.voting.domain.IssuedVotingToken;
import co.com.votapp.ws.voting.domain.port.in.IssueVotingTokenUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Spring MVC slice tests for {@link TokenController} — HTTP contract validation.
 *
 * <p>Validates that:
 * <ul>
 *   <li>Missing {@code eleccionId} → 400 Bad Request</li>
 *   <li>Missing {@code funcionarioId} → 400 Bad Request</li>
 *   <li>Malformed {@code eleccionId} (not a UUID) → 400 Bad Request</li>
 *   <li>Valid request → 201 Created with rawToken and tokenId</li>
 *   <li>Error responses use {@link GlobalExceptionHandler.ErrorResponse} shape ({@code $.message})</li>
 * </ul>
 *
 * <p>Named {@code *WebMvcTest.java} (not {@code *IT.java}) — no Testcontainers, runs under surefire.
 */
@DisplayName("TokenController - HTTP contract (WebMvcTest + GlobalExceptionHandler)")
@WebMvcTest(TokenController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class TokenControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IssueVotingTokenUseCase issueVotingTokenUseCase;

    // Required by SecurityConfig.portalAuthFilter bean (portal session support)
    @MockitoBean
    private PortalSessionPort portalSessionPort;

    private static final UUID ELECTION_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID TOKEN_ID    = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final String RAW_TOKEN = "super-secret-raw-token-xyz";

    // ── Task 1.1: Missing eleccionId → 400 ───────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 400 when eleccionId is missing from request body")
    void issueToken_shouldReturn400_whenEleccionIdIsMissing() throws Exception {
        // Given — body without eleccionId
        String body = """
                {
                  "funcionarioId": 42
                }
                """;

        // When & Then — error response must contain only $.message, never rawToken or tokenId
        mockMvc.perform(post("/api/v1/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.rawToken").doesNotExist())
                .andExpect(jsonPath("$.tokenId").doesNotExist());
    }

    // ── Task 1.1: Missing funcionarioId → 400 ────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 400 when funcionarioId is missing from request body")
    void issueToken_shouldReturn400_whenFuncionarioIdIsMissing() throws Exception {
        // Given — body without funcionarioId
        String body = """
                {
                  "eleccionId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
                }
                """;

        // When & Then — error response must contain only $.message, never rawToken or tokenId
        mockMvc.perform(post("/api/v1/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.rawToken").doesNotExist())
                .andExpect(jsonPath("$.tokenId").doesNotExist());
    }

    // ── Task 1.2: Malformed eleccionId → 400 ─────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 400 when eleccionId is not a valid UUID")
    void issueToken_shouldReturn400_whenEleccionIdIsMalformed() throws Exception {
        // Given — body with invalid UUID string
        String body = """
                {
                  "eleccionId": "not-a-uuid",
                  "funcionarioId": 42
                }
                """;

        // When & Then — UUID binding failure must produce 400, not 500; response must not leak token fields
        mockMvc.perform(post("/api/v1/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.rawToken").doesNotExist())
                .andExpect(jsonPath("$.tokenId").doesNotExist());
    }

    // ── Task 1.2: Valid payload → 201 Created ─────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 201 with rawToken and tokenId when request is valid")
    void issueToken_shouldReturn201WithTokenData_whenRequestIsValid() throws Exception {
        // Given
        when(issueVotingTokenUseCase.issue(any()))
                .thenReturn(new IssuedVotingToken(RAW_TOKEN, TOKEN_ID));

        String body = """
                {
                  "eleccionId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                  "funcionarioId": 42
                }
                """;

        // When & Then
        mockMvc.perform(post("/api/v1/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rawToken").value(RAW_TOKEN))
                .andExpect(jsonPath("$.tokenId").value(TOKEN_ID.toString()));
    }

    // ── Task 1.3: Error response uses GlobalExceptionHandler shape ─────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return error with $.message field — not Spring default error fields or token fields")
    void issueToken_shouldReturnErrorResponseShape_whenValidationFails() throws Exception {
        // Given — completely empty body to trigger binding failure
        String body = "{}";

        // When & Then — assert the error response shape is {message: "..."}, NOT {error: "...", timestamp: ...}
        // AND must not leak rawToken or tokenId in error responses (security contract)
        mockMvc.perform(post("/api/v1/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.status").doesNotExist())
                .andExpect(jsonPath("$.rawToken").doesNotExist())
                .andExpect(jsonPath("$.tokenId").doesNotExist());
    }
}
