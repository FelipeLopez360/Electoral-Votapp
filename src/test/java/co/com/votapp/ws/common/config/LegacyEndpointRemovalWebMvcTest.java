package co.com.votapp.ws.common.config;

import co.com.votapp.ws.auth.domain.port.out.PortalSessionPort;
import co.com.votapp.ws.common.exception.GlobalExceptionHandler;
import co.com.votapp.ws.electoral.application.service.CreateElectionWithCandidatesAppService;
import co.com.votapp.ws.electoral.application.service.ElectionTransitionAppService;
import co.com.votapp.ws.electoral.domain.port.in.AddCandidateUseCase;
import co.com.votapp.ws.electoral.domain.port.in.CreateElectionUseCase;
import co.com.votapp.ws.electoral.domain.port.in.FinalizeElectionUseCase;
import co.com.votapp.ws.electoral.domain.port.out.CandidateRepositoryPort;
import co.com.votapp.ws.electoral.domain.port.out.ElectionRepositoryPort;
import co.com.votapp.ws.electoral.infrastructure.adapter.in.web.ElectionController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Committed behavioral tests for the four legacy backend endpoints removed by
 * change {@code remove-admin-legacy-token-vote}.
 *
 * <p>These tests prove the HTTP contract enforced by the updated {@link SecurityConfig}:
 * <ul>
 *   <li>Unauthenticated requests to removed routes → <strong>401 Unauthorized</strong>
 *       (Spring Security blocks at the admin chain before MVC dispatch)</li>
 *   <li>Authenticated requests to removed routes → <strong>404 Not Found</strong>
 *       (security passes, Spring MVC dispatcher finds no handler)</li>
 * </ul>
 *
 * <p>Using {@link ElectionController} as the anchor controller for the {@code @WebMvcTest}
 * slice. The deleted routes ({@code /api/v1/tokens}, {@code /api/v1/votes},
 * {@code /api/v1/voters/**}, {@code /api/v1/elections/{id}/ballot}) do not exist in
 * any loaded controller — the behavior comes entirely from Spring Security + MVC dispatch.
 *
 * <p>Named {@code *WebMvcTest.java} (not {@code *IT.java}) to run under surefire
 * ({@code ./mvnw test}) — no Testcontainers, no Docker required.
 *
 * <p>TDD RED phase: this file was committed BEFORE the deletion was in place to document
 * the expected contract. It was always expected to pass once {@link SecurityConfig} and
 * the controller deletions landed (Phase 6.1 + Phase 4.1-4.3).
 */
@DisplayName("Legacy endpoint removal - HTTP contract (401 unauth / 404 auth)")
@WebMvcTest(ElectionController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class LegacyEndpointRemovalWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    // ── Required MockitoBean stubs for ElectionController's constructor ────────

    @MockitoBean
    private CreateElectionUseCase createElectionUseCase;

    @MockitoBean
    private CreateElectionWithCandidatesAppService createElectionWithCandidatesAppService;

    @MockitoBean
    private ElectionTransitionAppService electionTransitionAppService;

    @MockitoBean
    private FinalizeElectionUseCase finalizeElectionUseCase;

    @MockitoBean
    private AddCandidateUseCase addCandidateUseCase;

    @MockitoBean
    private ElectionRepositoryPort electionRepository;

    @MockitoBean
    private CandidateRepositoryPort candidateRepository;

    // Required by SecurityConfig.portalAuthFilter bean (portal chain)
    @MockitoBean
    private PortalSessionPort portalSessionPort;

    private static final UUID ELECTION_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    // ── POST /api/v1/tokens (admin manual token issuance — DELETED) ─────────────

    @Test
    @DisplayName("Should return 401 when unauthenticated POST /api/v1/tokens (deleted endpoint)")
    void tokens_shouldReturn401_whenUnauthenticated() throws Exception {
        // When & Then — no credentials: admin chain requires .authenticated() → 401
        mockMvc.perform(post("/api/v1/tokens")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 404 when authenticated POST /api/v1/tokens (deleted endpoint, no handler)")
    void tokens_shouldReturn404_whenAuthenticated() throws Exception {
        // When & Then — credentials valid: security passes, no controller handles /api/v1/tokens → 404
        mockMvc.perform(post("/api/v1/tokens")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    // ── POST /api/v1/votes (raw-token vote casting — DELETED) ──────────────────

    @Test
    @DisplayName("Should return 401 when unauthenticated POST /api/v1/votes (deleted endpoint)")
    void votes_shouldReturn401_whenUnauthenticated() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/votes")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 404 when authenticated POST /api/v1/votes (deleted endpoint, no handler)")
    void votes_shouldReturn404_whenAuthenticated() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/votes")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    // ── GET /api/v1/voters/{id}/eligibility (voter eligibility check — DELETED) ─

    @Test
    @DisplayName("Should return 401 when unauthenticated GET /api/v1/voters/{id}/eligibility (deleted endpoint)")
    void voterEligibility_shouldReturn401_whenUnauthenticated() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/voters/42/eligibility"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 404 when authenticated GET /api/v1/voters/{id}/eligibility (deleted endpoint, no handler)")
    void voterEligibility_shouldReturn404_whenAuthenticated() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/voters/42/eligibility"))
                .andExpect(status().isNotFound());
    }

    // ── GET /api/v1/elections/{id}/ballot (ballot retrieval — DELETED) ─────────

    @Test
    @DisplayName("Should return 401 when unauthenticated GET /api/v1/elections/{id}/ballot (deleted endpoint)")
    void electionsBallot_shouldReturn401_whenUnauthenticated() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/elections/{id}/ballot", ELECTION_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 404 when authenticated GET /api/v1/elections/{id}/ballot (deleted endpoint, no handler)")
    void electionsBallot_shouldReturn404_whenAuthenticated() throws Exception {
        // When & Then — /api/v1/elections/{id}/ballot was removed from ElectionController
        mockMvc.perform(get("/api/v1/elections/{id}/ballot", ELECTION_ID))
                .andExpect(status().isNotFound());
    }
}
