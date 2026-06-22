package co.com.votapp.ws.electoral.infrastructure.adapter.in.web;

import co.com.votapp.ws.auth.domain.port.out.PortalSessionPort;
import co.com.votapp.ws.common.config.SecurityConfig;
import co.com.votapp.ws.common.exception.GlobalExceptionHandler;
import co.com.votapp.ws.electoral.application.dto.LiveTrackingResponse;
import co.com.votapp.ws.electoral.application.service.DashboardLiveTrackingService;
import co.com.votapp.ws.electoral.application.service.DashboardMetricsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Spring MVC slice tests for the live-tracking endpoint in {@link DashboardController}.
 *
 * <p>TDD: RED phase — written before production code (task 3.1).
 *
 * <p>Verifies:
 * <ul>
 *   <li>200 + correct JSON shape when authenticated as ADMIN.</li>
 *   <li>200 + empty elections list when no active elections exist.</li>
 *   <li>401 when no authentication provided.</li>
 * </ul>
 *
 * <p>Named *WebMvcTest.java (not *IT.java) — runs under surefire, no Testcontainers.
 */
@DisplayName("DashboardController - GET /api/v1/dashboard/live-tracking (WebMvcTest)")
@WebMvcTest(DashboardController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class DashboardLiveTrackingControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardMetricsService dashboardMetricsService;

    @MockitoBean
    private DashboardLiveTrackingService dashboardLiveTrackingService;

    // Required by SecurityConfig.portalAuthFilter bean
    @MockitoBean
    private PortalSessionPort portalSessionPort;

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 200 with correct JSON shape when authenticated")
    void getLiveTracking_shouldReturn200WithJsonShape_whenAuthenticated() throws Exception {
        // Given
        UUID eleccionId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        LiveTrackingResponse response = new LiveTrackingResponse(List.of(
                new LiveTrackingResponse.LiveTrackingItem(eleccionId, "Election Alpha", 42L, 200L)
        ));
        when(dashboardLiveTrackingService.getLiveTracking()).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/v1/dashboard/live-tracking")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.elections").isArray())
                .andExpect(jsonPath("$.elections.length()").value(1))
                .andExpect(jsonPath("$.elections[0].id").value(eleccionId.toString()))
                .andExpect(jsonPath("$.elections[0].title").value("Election Alpha"))
                .andExpect(jsonPath("$.elections[0].totalCastVotes").value(42))
                .andExpect(jsonPath("$.elections[0].totalEligibleVoters").value(200));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 200 with empty elections list when no active elections exist")
    void getLiveTracking_shouldReturn200WithEmptyList_whenNoActiveElections() throws Exception {
        // Given
        when(dashboardLiveTrackingService.getLiveTracking())
                .thenReturn(new LiveTrackingResponse(List.of()));

        // When & Then
        mockMvc.perform(get("/api/v1/dashboard/live-tracking")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.elections").isArray())
                .andExpect(jsonPath("$.elections.length()").value(0));
    }

    @Test
    @DisplayName("Should return 401 when no authentication is provided")
    void getLiveTracking_shouldReturn401_whenNotAuthenticated() throws Exception {
        // When & Then — no @WithMockUser
        mockMvc.perform(get("/api/v1/dashboard/live-tracking")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
