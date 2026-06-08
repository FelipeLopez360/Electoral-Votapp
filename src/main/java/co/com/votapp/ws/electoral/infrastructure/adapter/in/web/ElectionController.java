package co.com.votapp.ws.electoral.infrastructure.adapter.in.web;

import co.com.votapp.ws.electoral.application.command.CreateElectionCommand;
import co.com.votapp.ws.electoral.domain.Ballot;
import co.com.votapp.ws.electoral.domain.Election;
import co.com.votapp.ws.electoral.domain.port.in.ActivateElectionUseCase;
import co.com.votapp.ws.electoral.domain.port.in.CreateElectionUseCase;
import co.com.votapp.ws.electoral.domain.port.in.FinalizeElectionUseCase;
import co.com.votapp.ws.electoral.domain.port.in.GetBallotUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * REST adapter for election lifecycle management.
 *
 * <p>Admin endpoints (create, activate, finalize) require HTTP Basic auth.
 * The ballot endpoint is public — the rawToken authenticates the voter.
 */
@RestController
@RequestMapping("/api/v1/elections")
@Tag(name = "Elections", description = "Election lifecycle management (MVP)")
public class ElectionController {

    private final CreateElectionUseCase createElectionUseCase;
    private final ActivateElectionUseCase activateElectionUseCase;
    private final FinalizeElectionUseCase finalizeElectionUseCase;
    private final GetBallotUseCase getBallotUseCase;

    public ElectionController(
            CreateElectionUseCase createElectionUseCase,
            ActivateElectionUseCase activateElectionUseCase,
            FinalizeElectionUseCase finalizeElectionUseCase,
            GetBallotUseCase getBallotUseCase) {
        this.createElectionUseCase = createElectionUseCase;
        this.activateElectionUseCase = activateElectionUseCase;
        this.finalizeElectionUseCase = finalizeElectionUseCase;
        this.getBallotUseCase = getBallotUseCase;
    }

    @PostMapping
    @Operation(
            summary = "Create a new election",
            description = "Creates an election in PROGRAMADA state. Requires admin credentials.",
            security = @SecurityRequirement(name = "basicAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Election created"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "409", description = "Business rule violation")
    })
    public ResponseEntity<ElectionResponse> createElection(@RequestBody CreateElectionRequest request) {
        CreateElectionCommand command = new CreateElectionCommand(
                request.codigo(),
                request.nombre(),
                null,
                LocalDateTime.parse(request.fechaInicio()),
                LocalDateTime.parse(request.fechaFin())
        );
        Election election = createElectionUseCase.create(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(ElectionResponse.from(election));
    }

    @PostMapping("/{id}/activate")
    @Operation(
            summary = "Activate an election",
            description = "Transitions an election from PROGRAMADA to ACTIVA. Creates blank-vote candidate automatically.",
            security = @SecurityRequirement(name = "basicAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Election activated"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "409", description = "Invalid state transition")
    })
    public ResponseEntity<Void> activateElection(@PathVariable String id) {
        activateElectionUseCase.activate(UUID.fromString(id));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/finalize")
    @Operation(
            summary = "Finalize an election",
            description = "Transitions an election from ACTIVA to FINALIZADA. No more votes accepted after this.",
            security = @SecurityRequirement(name = "basicAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Election finalized"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "409", description = "Invalid state transition")
    })
    public ResponseEntity<Void> finalizeElection(@PathVariable String id) {
        finalizeElectionUseCase.finalize(UUID.fromString(id));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/ballot")
    @Operation(
            summary = "Get ballot for an election",
            description = "Returns the ordered list of candidates for a given election. "
                    + "Public endpoint — the voter's rawToken is passed as a query parameter. "
                    + "The use case validates the token belongs to this election and is in ISSUED state."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ballot returned"),
            @ApiResponse(responseCode = "409", description = "Token invalid or election not ACTIVA")
    })
    public ResponseEntity<BallotResponse> getBallot(
            @PathVariable String id,
            @RequestParam String token) {
        // The use case hashes rawToken, validates it is ISSUED, checks the election is ACTIVA,
        // then returns the ordered candidates. The {id} path param is informational for routing.
        Ballot ballot = getBallotUseCase.getBallot(token);
        return ResponseEntity.ok(BallotResponse.from(ballot));
    }

    // ── Request / Response records ──────────────────────────────────────────

    /**
     * Request body for election creation.
     * Dates are ISO-8601 LocalDateTime strings (e.g. "2025-11-01T08:00:00").
     */
    public record CreateElectionRequest(
            String codigo,
            String nombre,
            String fechaInicio,
            String fechaFin
    ) {}

    /** Response projection for a created / queried election. */
    public record ElectionResponse(
            String id,
            String codigo,
            String nombre,
            String estado,
            String fechaInicio,
            String fechaFin
    ) {
        public static ElectionResponse from(Election election) {
            return new ElectionResponse(
                    election.id().toString(),
                    election.codigo(),
                    election.nombre(),
                    election.status().name(),
                    election.fechaInicio().toString(),
                    election.fechaFin().toString()
            );
        }
    }

    /** Response projection for the ballot (election + ordered candidates). */
    public record BallotResponse(
            String eleccionId,
            String eleccionNombre,
            List<CandidateOptionResponse> candidates
    ) {
        public static BallotResponse from(Ballot ballot) {
            List<CandidateOptionResponse> candidates = ballot.candidates().stream()
                    .map(c -> new CandidateOptionResponse(
                            c.id().toString(),
                            c.nombre(),
                            c.esVotoEnBlanco()
                    ))
                    .toList();
            return new BallotResponse(
                    ballot.eleccionId().toString(),
                    ballot.eleccionNombre(),
                    candidates
            );
        }
    }

    /** Candidate option within a ballot. */
    public record CandidateOptionResponse(
            String id,
            String nombre,
            boolean esVotoEnBlanco
    ) {}
}
