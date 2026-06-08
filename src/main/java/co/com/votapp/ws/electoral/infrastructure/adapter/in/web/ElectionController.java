package co.com.votapp.ws.electoral.infrastructure.adapter.in.web;

import co.com.votapp.ws.electoral.application.command.AddCandidateCommand;
import co.com.votapp.ws.electoral.application.command.CreateElectionCommand;
import co.com.votapp.ws.electoral.domain.Ballot;
import co.com.votapp.ws.electoral.domain.Candidate;
import co.com.votapp.ws.electoral.domain.Election;
import co.com.votapp.ws.electoral.domain.port.in.ActivateElectionUseCase;
import co.com.votapp.ws.electoral.domain.port.in.AddCandidateUseCase;
import co.com.votapp.ws.electoral.domain.port.in.CreateElectionUseCase;
import co.com.votapp.ws.electoral.domain.port.in.FinalizeElectionUseCase;
import co.com.votapp.ws.electoral.domain.port.in.GetBallotUseCase;
import co.com.votapp.ws.electoral.domain.port.out.ElectionRepositoryPort;
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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
    private final AddCandidateUseCase addCandidateUseCase;
    private final ElectionRepositoryPort electionRepository;

    public ElectionController(
            CreateElectionUseCase createElectionUseCase,
            ActivateElectionUseCase activateElectionUseCase,
            FinalizeElectionUseCase finalizeElectionUseCase,
            GetBallotUseCase getBallotUseCase,
            AddCandidateUseCase addCandidateUseCase,
            ElectionRepositoryPort electionRepository) {
        this.createElectionUseCase = createElectionUseCase;
        this.activateElectionUseCase = activateElectionUseCase;
        this.finalizeElectionUseCase = finalizeElectionUseCase;
        this.getBallotUseCase = getBallotUseCase;
        this.addCandidateUseCase = addCandidateUseCase;
        this.electionRepository = electionRepository;
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
        // Accept both "2025-11-01T08:00:00" (local) and "2025-11-01T08:00:00Z" (UTC)
        LocalDateTime start = parseDateTime(request.fechaInicio());
        LocalDateTime end = parseDateTime(request.fechaFin());
        CreateElectionCommand command = new CreateElectionCommand(
                request.codigo(),
                request.nombre(),
                null,
                start,
                end
        );
        Election election = createElectionUseCase.create(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(ElectionResponse.from(election));
    }

    @GetMapping
    @Operation(
            summary = "List all elections",
            description = "Returns all elections ordered by creation date. Requires admin credentials.",
            security = @SecurityRequirement(name = "basicAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of elections"),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<List<ElectionResponse>> listElections() {
        List<Election> elections = electionRepository.findAll();
        List<ElectionResponse> response = elections.stream()
                .map(ElectionResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{eleccionId}/candidates")
    @Operation(
            summary = "Add a candidate to an election",
            description = "Adds a candidate to a PROGRAMADA or ACTIVA election. "
                    + "Requires admin credentials.",
            security = @SecurityRequirement(name = "basicAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Candidate created"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "409", description = "Duplicate order number or election not accepting candidates")
    })
    public ResponseEntity<CandidateResponse> addCandidate(
            @PathVariable UUID eleccionId,
            @RequestBody AddCandidateRequest request) {
        AddCandidateCommand command = new AddCandidateCommand(
                eleccionId,
                request.nombre(),
                request.descripcion() != null ? request.descripcion() : "",
                request.numeroOrden()
        );
        Candidate candidate = addCandidateUseCase.addCandidate(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                new CandidateResponse(candidate.id().toString(), candidate.nombre(), candidate.numeroOrden()));
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

    // ── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Parse an ISO-8601 datetime string as LocalDateTime.
     * Handles both {@code "2025-11-01T08:00:00"} and {@code "2025-11-01T08:00:00Z"}.
     */
    private static LocalDateTime parseDateTime(String value) {
        if (value == null) return null;
        if (value.endsWith("Z") || value.endsWith("z")) {
            return LocalDateTime.ofInstant(Instant.parse(value), ZoneId.systemDefault());
        }
        return LocalDateTime.parse(value);
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

    /** Request body for adding a candidate. */
    public record AddCandidateRequest(
            String nombre,
            String descripcion,
            int numeroOrden
    ) {}

    /** Response projection for a created candidate. */
    public record CandidateResponse(
            String id,
            String nombre,
            int numeroOrden
    ) {}
}
