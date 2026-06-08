package co.com.votapp.ws.votereligibility.infrastructure.adapter.in.web;

import co.com.votapp.ws.votereligibility.domain.port.out.VoterEligibilityRepositoryPort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST adapter for voter eligibility checks.
 *
 * <p>Public endpoint — no authentication required. The kiosk or token-issuance UI
 * calls this before displaying the "issue token" form to the admin.
 *
 * <p>Design note: no dedicated input port is created for this use case because
 * the query is a single read with no business logic beyond the DB predicate
 * (estado_laboral = ACTIVO AND puede_votar = true). The controller injects
 * the output port directly — a justified exception to the usual input-port rule
 * for read-only, no-side-effect queries in hexagonal architecture.
 */
@RestController
@RequestMapping("/api/v1/voters")
@Tag(name = "Voters", description = "Voter eligibility checks")
public class VoterEligibilityController {

    private final VoterEligibilityRepositoryPort eligibilityRepository;

    public VoterEligibilityController(VoterEligibilityRepositoryPort eligibilityRepository) {
        this.eligibilityRepository = eligibilityRepository;
    }

    @GetMapping("/{funcionarioId}/eligibility")
    @Operation(
            summary = "Check voter eligibility",
            description = "Returns whether the given funcionario is eligible to vote "
                    + "(estado_laboral = ACTIVO AND puede_votar = true). Public endpoint."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Eligibility result returned"),
            @ApiResponse(responseCode = "400", description = "Invalid funcionarioId")
    })
    public ResponseEntity<EligibilityResponse> checkEligibility(@PathVariable Long funcionarioId) {
        boolean eligible = eligibilityRepository.isEligible(funcionarioId);
        return ResponseEntity.ok(new EligibilityResponse(eligible));
    }

    // ── Response record ──────────────────────────────────────────────────────

    /** Eligibility result. */
    public record EligibilityResponse(boolean elegible) {}
}
