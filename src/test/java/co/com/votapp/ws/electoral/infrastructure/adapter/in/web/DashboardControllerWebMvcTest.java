package co.com.votapp.ws.electoral.infrastructure.adapter.in.web;

import co.com.votapp.ws.auth.domain.port.out.PortalSessionPort;
import co.com.votapp.ws.common.config.SecurityConfig;
import co.com.votapp.ws.common.exception.GlobalExceptionHandler;
import co.com.votapp.ws.electoral.application.dto.DashboardMetricsResponse;
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

import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Spring MVC slice tests for {@link DashboardController}.
 *
 * <p>TDD: RED phase — written before production code (task 3.1).
 *
 * <p>Verifies:
 * <ul>
 *   <li>200 + JSON shape when authenticated as ADMIN.</li>
 *   <li>401 when no authentication provided.</li>
 * </ul>
 *
 * <p>Named *WebMvcTest.java (not *IT.java) — runs under surefire, no Testcontainers.
 */
@DisplayName("DashboardController - GET /api/v1/dashboard/metrics (WebMvcTest)")
@WebMvcTest(DashboardController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class DashboardControllerWebMvcTest {

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
    void getMetrics_shouldReturn200WithJsonShape_whenAuthenticated() throws Exception {
        // Given
        DashboardMetricsResponse response = buildSampleResponse();
        when(dashboardMetricsService.getMetrics()).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/v1/dashboard/metrics")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.funcionarios").exists())
                .andExpect(jsonPath("$.funcionarios.total").value(120))
                .andExpect(jsonPath("$.funcionarios.byStatus.ACTIVO").value(100))
                .andExpect(jsonPath("$.funcionarios.byStatus.INACTIVO").value(20))
                .andExpect(jsonPath("$.activeVoters").value(95))
                .andExpect(jsonPath("$.elections").exists())
                .andExpect(jsonPath("$.elections.total").value(8))
                .andExpect(jsonPath("$.elections.byStatus.PROGRAMADA").value(2))
                .andExpect(jsonPath("$.elections.byStatus.ACTIVA").value(1))
                .andExpect(jsonPath("$.elections.byStatus.FINALIZADA").value(4))
                .andExpect(jsonPath("$.elections.byStatus.CANCELADA").value(1))
                .andExpect(jsonPath("$.elections.byStatus.SUSPENDIDA").value(0));
    }

    @Test
    @DisplayName("Should return 401 when no authentication is provided")
    void getMetrics_shouldReturn401_whenNotAuthenticated() throws Exception {
        // When & Then — no @WithMockUser
        mockMvc.perform(get("/api/v1/dashboard/metrics")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private static DashboardMetricsResponse buildSampleResponse() {
        DashboardMetricsResponse.FuncionarioBlock funcionarios =
                new DashboardMetricsResponse.FuncionarioBlock(
                        120L,
                        Map.of("ACTIVO", 100L, "INACTIVO", 20L)
                );

        DashboardMetricsResponse.ElectionBlock elections =
                new DashboardMetricsResponse.ElectionBlock(
                        8L,
                        Map.of(
                                "PROGRAMADA", 2L,
                                "ACTIVA", 1L,
                                "FINALIZADA", 4L,
                                "CANCELADA", 1L,
                                "SUSPENDIDA", 0L
                        )
                );

        return new DashboardMetricsResponse(funcionarios, 95L, elections);
    }
}
