package co.com.votapp.ws.electoral.infrastructure.adapter.in.web;

import co.com.votapp.ws.electoral.application.dto.DashboardMetricsResponse;
import co.com.votapp.ws.electoral.application.dto.LiveTrackingResponse;
import co.com.votapp.ws.electoral.application.service.DashboardLiveTrackingService;
import co.com.votapp.ws.electoral.application.service.DashboardMetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST adapter that exposes the admin dashboard metrics endpoint.
 *
 * <p>{@code GET /api/v1/dashboard/metrics} is protected by the existing admin HTTP Basic
 * filter chain (Order 2 in {@link co.com.votapp.ws.common.config.SecurityConfig}) — no
 * additional security configuration is required.
 *
 * <p>This controller delegates directly to {@link DashboardMetricsService} (an application-layer
 * {@code @Service}). There is no domain use case for a pure-read aggregation with zero
 * business rules — this mirrors the {@code ElectionController} pattern for list/detail queries.
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard", description = "Admin operations dashboard aggregate metrics")
public class DashboardController {

    private final DashboardMetricsService dashboardMetricsService;
    private final DashboardLiveTrackingService dashboardLiveTrackingService;

    public DashboardController(DashboardMetricsService dashboardMetricsService,
                               DashboardLiveTrackingService dashboardLiveTrackingService) {
        this.dashboardMetricsService = dashboardMetricsService;
        this.dashboardLiveTrackingService = dashboardLiveTrackingService;
    }

    @GetMapping("/metrics")
    @Operation(
            summary = "Get admin dashboard metrics",
            description = "Returns aggregated counts for the admin home panel: "
                    + "total funcionarios grouped by labor status, active eligible voters count, "
                    + "and total elections grouped by status. "
                    + "All five ElectionStatus values are always present (zero-filled if no data). "
                    + "Requires admin credentials (HTTP Basic).",
            security = @SecurityRequirement(name = "basicAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dashboard metrics returned"),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<DashboardMetricsResponse> getMetrics() {
        return ResponseEntity.ok(dashboardMetricsService.getMetrics());
    }

    @GetMapping("/live-tracking")
    @Operation(
            summary = "Get live participation tracking per active election",
            description = "Returns real-time participation progress for all elections currently "
                    + "in ACTIVA status. Each item includes the election ID, title, total cast votes, "
                    + "and total eligible voters (census-first; falls back to global active voters "
                    + "when no custom census is configured). "
                    + "Requires admin credentials (HTTP Basic).",
            security = @SecurityRequirement(name = "basicAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Live tracking data returned (empty list if no active elections)"),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<LiveTrackingResponse> getLiveTracking() {
        return ResponseEntity.ok(dashboardLiveTrackingService.getLiveTracking());
    }
}
