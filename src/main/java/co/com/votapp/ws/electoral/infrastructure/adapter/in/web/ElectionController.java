package co.com.votapp.ws.electoral.infrastructure.adapter.in.web;

import co.com.votapp.ws.common.domain.model.PageResult;
import co.com.votapp.ws.electoral.application.command.AddCandidateCommand;
import co.com.votapp.ws.electoral.application.command.CreateElectionCommand;
import co.com.votapp.ws.electoral.application.service.CreateElectionWithCandidatesAppService;
import co.com.votapp.ws.electoral.application.service.ElectionTransitionAppService;
import co.com.votapp.ws.electoral.domain.Candidate;
import co.com.votapp.ws.electoral.domain.Election;
import co.com.votapp.ws.electoral.domain.ElectionStatus;
import co.com.votapp.ws.electoral.domain.port.in.AddCandidateUseCase;
import co.com.votapp.ws.electoral.domain.port.in.CreateElectionUseCase;
import co.com.votapp.ws.electoral.domain.port.in.FinalizeElectionUseCase;
import co.com.votapp.ws.electoral.domain.port.out.CandidateRepositoryPort;
import co.com.votapp.ws.electoral.domain.port.out.ElectionRepositoryPort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * REST adapter for election lifecycle management.
 *
 * <p>All admin endpoints (create, activate, finalize) require HTTP Basic auth.
 *
 * <p>V5 changes: {@code AddCandidateRequest} and {@code CandidateResponse} now use
 * {@code funcionarioId} instead of {@code numeroOrden}. {@code afiliacionPolitica} removed
 * from all DTOs. {@code CandidateFullRequest} also updated. List candidates endpoint
 * now calls {@code findByEleccionIdOrderByNombre}.
 */
@RestController
@RequestMapping("/api/v1/elections")
@Tag(name = "Elections", description = "Election lifecycle management (MVP)")
public class ElectionController {

    private final CreateElectionUseCase createElectionUseCase;
    private final CreateElectionWithCandidatesAppService createElectionWithCandidatesAppService;
    private final ElectionTransitionAppService electionTransitionAppService;
    private final FinalizeElectionUseCase finalizeElectionUseCase;
    private final AddCandidateUseCase addCandidateUseCase;
    private final ElectionRepositoryPort electionRepository;
    private final CandidateRepositoryPort candidateRepository;

    public ElectionController(
            CreateElectionUseCase createElectionUseCase,
            CreateElectionWithCandidatesAppService createElectionWithCandidatesAppService,
            ElectionTransitionAppService electionTransitionAppService,
            FinalizeElectionUseCase finalizeElectionUseCase,
            AddCandidateUseCase addCandidateUseCase,
            ElectionRepositoryPort electionRepository,
            CandidateRepositoryPort candidateRepository) {
        this.createElectionUseCase = createElectionUseCase;
        this.createElectionWithCandidatesAppService = createElectionWithCandidatesAppService;
        this.electionTransitionAppService = electionTransitionAppService;
        this.finalizeElectionUseCase = finalizeElectionUseCase;
        this.addCandidateUseCase = addCandidateUseCase;
        this.electionRepository = electionRepository;
        this.candidateRepository = candidateRepository;
    }

    @PostMapping
    @Operation(
            summary = "Create a new election",
            description = "Creates an election in PROGRAMADA state. Requires admin credentials. "
                    + "Accepts optional ballot config fields (permiteVotoBlanco, maxVotosPorElector); "
                    + "defaults are true and 1 when omitted.",
            security = @SecurityRequirement(name = "basicAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Election created"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "409", description = "Business rule violation")
    })
    public ResponseEntity<?> createElection(@RequestBody CreateElectionRequest request) {
        LocalDateTime start = parseDateTime(request.fechaInicio());
        LocalDateTime end = parseDateTime(request.fechaFin());

        if (start != null && start.toLocalDate().isBefore(java.time.LocalDate.now())) {
            return ResponseEntity.badRequest().body("La fecha de inicio no puede ser anterior al día de hoy");
        }
        if (end != null && start != null && !end.isAfter(start)) {
            return ResponseEntity.badRequest().body("La fecha de fin debe ser posterior a la fecha de inicio");
        }

        CreateElectionCommand command = new CreateElectionCommand(
                request.codigo(),
                request.nombre(),
                null,
                start,
                end,
                request.effectivePermiteVotoBlanco(),
                request.effectiveMaxVotosPorElector()
        );
        Election election = createElectionUseCase.create(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(ElectionResponse.from(election));
    }

    @PostMapping("/full")
    @Operation(
            summary = "Create election with candidates (comprehensive submit)",
            description = "Creates an election in PROGRAMADA state along with all its candidates "
                    + "in a single atomic transaction. Designed for the admin wizard final Review step. "
                    + "Requires admin credentials.",
            security = @SecurityRequirement(name = "basicAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Election and candidates created atomically"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "409", description = "Business rule violation (e.g. duplicate codigo)")
    })
    public ResponseEntity<ElectionResponse> createElectionFull(
            @RequestBody CreateElectionFullRequest request) {
        LocalDateTime start = parseDateTime(request.fechaInicio());
        LocalDateTime end = parseDateTime(request.fechaFin());

        if (start != null && start.toLocalDate().isBefore(java.time.LocalDate.now())) {
            return ResponseEntity.badRequest().build();
        }
        if (end != null && start != null && !end.isAfter(start)) {
            return ResponseEntity.badRequest().build();
        }

        CreateElectionCommand electionCommand = new CreateElectionCommand(
                request.codigo(),
                request.nombre(),
                null,
                start,
                end,
                request.permiteVotoBlanco(),
                request.maxVotosPorElector()
        );

        List<CreateElectionWithCandidatesAppService.CandidateCreationData> candidatesData =
                request.candidatos() == null ? Collections.emptyList() :
                request.candidatos().stream()
                        .map(c -> new CreateElectionWithCandidatesAppService.CandidateCreationData(
                                c.nombre(),
                                c.funcionarioId(),
                                c.fotoUrl(),
                                c.biografia(),
                                c.propuestas()
                        ))
                        .toList();

        Election election = createElectionWithCandidatesAppService
                .createWithCandidates(electionCommand, candidatesData);
        return ResponseEntity.status(HttpStatus.CREATED).body(ElectionResponse.from(election));
    }

    @GetMapping
    @Operation(
            summary = "List elections with pagination and search",
            description = "Returns a paginated list of elections ordered by creation date descending. "
                    + "Optional search filters by nombre or codigo (case-insensitive). "
                    + "Defaults to page=0, size=8. Requires admin credentials.",
            security = @SecurityRequirement(name = "basicAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated list of elections"),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<PageResult<ElectionResponse>> listElections(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String search) {
        int effectivePage = Math.max(0, page != null ? page : 0);
        int effectiveSize = Math.min(100, Math.max(1, size != null ? size : 8));
        PageResult<ElectionResponse> response = electionRepository
                .findAll(effectivePage, effectiveSize, search)
                .map(ElectionResponse::from);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get election by ID",
            description = "Returns a single election with all its fields.",
            security = @SecurityRequirement(name = "basicAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Election found"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "404", description = "Election not found")
    })
    public ResponseEntity<ElectionResponse> getElection(@PathVariable UUID id) {
        Election election = electionRepository.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException("Election not found: " + id));
        return ResponseEntity.ok(ElectionResponse.from(election));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update an election",
            description = "Updates nombre, fechaInicio and fechaFin of a PROGRAMADA election.",
            security = @SecurityRequirement(name = "basicAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Election updated"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "409", description = "Election is not PROGRAMADA")
    })
    public ResponseEntity<?> updateElection(
            @PathVariable UUID id,
            @RequestBody UpdateElectionRequest request) {
        Election existing = electionRepository.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException("Election not found: " + id));
        if (existing.status() != ElectionStatus.PROGRAMADA) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        LocalDateTime start = parseDateTime(request.fechaInicio());
        LocalDateTime end = parseDateTime(request.fechaFin());

        if (start != null && start.toLocalDate().isBefore(java.time.LocalDate.now())) {
            return ResponseEntity.badRequest().body("La fecha de inicio no puede ser anterior al día de hoy");
        }

        LocalDateTime resolvedStart = start != null ? start : existing.fechaInicio();
        LocalDateTime resolvedEnd = end != null ? end : existing.fechaFin();
        if (!resolvedEnd.isAfter(resolvedStart)) {
            return ResponseEntity.badRequest().body("La fecha de fin debe ser posterior a la fecha de inicio");
        }

        Election updated = new Election(
                existing.id(), existing.codigo(), request.nombre() != null ? request.nombre() : existing.nombre(),
                existing.status(),
                resolvedStart, resolvedEnd,
                existing.permiteVotoBlanco(),
                existing.maxVotosPorElector()
        );
        return ResponseEntity.ok(ElectionResponse.from(electionRepository.save(updated)));
    }

    @GetMapping("/{id}/candidates")
    @Operation(
            summary = "List candidates for an election",
            description = "Returns the ordered list of candidates (alphabetical, admin endpoint).",
            security = @SecurityRequirement(name = "basicAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Candidate list"),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<List<CandidateResponse>> listCandidates(@PathVariable UUID id) {
        List<Candidate> candidates = candidateRepository.findByEleccionIdOrderByNombre(id);
        List<CandidateResponse> response = candidates.stream()
                .map(c -> new CandidateResponse(
                        c.id().toString(), c.nombre(), c.funcionarioId(),
                        c.fotoUrl(), c.biografia(), c.propuestas()))
                .toList();
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}/candidates/{candidateId}")
    @Operation(
            summary = "Remove a candidate",
            description = "Deletes a candidate from a PROGRAMADA election. Cannot remove blank vote candidate.",
            security = @SecurityRequirement(name = "basicAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Candidate removed"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "404", description = "Candidate not found"),
            @ApiResponse(responseCode = "409", description = "Election is not PROGRAMADA or candidate is blank vote")
    })
    public ResponseEntity<Void> removeCandidate(
            @PathVariable UUID id,
            @PathVariable UUID candidateId) {
        Election existing = electionRepository.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException("Election not found: " + id));
        if (existing.status() != ElectionStatus.PROGRAMADA) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        Candidate candidate = candidateRepository.findByIdAndEleccionId(candidateId, id)
                .orElseThrow(() -> new java.util.NoSuchElementException("Candidate not found: " + candidateId));
        if (candidate.esVotoEnBlanco()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        candidateRepository.deleteById(candidateId);
        return ResponseEntity.noContent().build();
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
            @ApiResponse(responseCode = "409", description = "Duplicate funcionario or election not accepting candidates")
    })
    public ResponseEntity<CandidateResponse> addCandidate(
            @PathVariable UUID eleccionId,
            @RequestBody AddCandidateRequest request) {
        AddCandidateCommand command = new AddCandidateCommand(
                eleccionId,
                request.nombre(),
                request.descripcion() != null ? request.descripcion() : "",
                request.funcionarioId(),
                request.fotoUrl(),
                request.biografia(),
                request.propuestas()
        );
        Candidate candidate = addCandidateUseCase.addCandidate(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                new CandidateResponse(
                        candidate.id().toString(), candidate.nombre(), candidate.funcionarioId(),
                        candidate.fotoUrl(), candidate.biografia(), candidate.propuestas()));
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
        electionTransitionAppService.activate(UUID.fromString(id));
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
     * Request body for election creation via the legacy {@code POST /api/v1/elections} endpoint.
     *
     * <p>Dates are ISO-8601 LocalDateTime strings (e.g. "2025-11-01T08:00:00").
     *
     * <p>Ballot configuration fields are OPTIONAL in JSON. When omitted, Jackson binds them
     * as {@code null} (wrapper types), and the controller applies the historical defaults:
     * {@code permiteVotoBlanco=true}, {@code maxVotosPorElector=1}.
     */
    public record CreateElectionRequest(
            String codigo,
            String nombre,
            String fechaInicio,
            String fechaFin,
            Boolean permiteVotoBlanco,
            Integer maxVotosPorElector
    ) {
        /** Resolved value respecting historical default ({@code true} when omitted). */
        public boolean effectivePermiteVotoBlanco() {
            return permiteVotoBlanco == null ? true : permiteVotoBlanco;
        }

        /** Resolved value respecting historical default ({@code 1} when omitted). */
        public int effectiveMaxVotosPorElector() {
            return maxVotosPorElector == null ? 1 : maxVotosPorElector;
        }
    }

    /**
     * Comprehensive request body for the wizard final-submit endpoint.
     *
     * <p>Carries election metadata, ballot config, and candidates in a single payload.
     * V5: candidates use {@code funcionarioId} instead of {@code numeroOrden}.
     */
    public record CreateElectionFullRequest(
            String codigo,
            String nombre,
            String fechaInicio,
            String fechaFin,
            boolean permiteVotoBlanco,
            int maxVotosPorElector,
            List<CandidateFullRequest> candidatos
    ) {}

    /**
     * Candidate data within the comprehensive election creation request.
     * V5: {@code funcionarioId} replaces {@code numeroOrden}; {@code afiliacionPolitica} removed.
     * All rich profile fields are optional (nullable).
     */
    public record CandidateFullRequest(
            String nombre,
            Integer funcionarioId,
            String fotoUrl,
            String biografia,
            String propuestas
    ) {}

    /**
     * Response projection for a created / queried election.
     * Includes ballot configuration fields.
     */
    public record ElectionResponse(
            String id,
            String codigo,
            String nombre,
            String estado,
            String fechaInicio,
            String fechaFin,
            boolean permiteVotoBlanco,
            int maxVotosPorElector
    ) {
        public static ElectionResponse from(Election election) {
            return new ElectionResponse(
                    election.id().toString(),
                    election.codigo(),
                    election.nombre(),
                    election.status().name(),
                    election.fechaInicio().toString(),
                    election.fechaFin().toString(),
                    election.permiteVotoBlanco(),
                    election.maxVotosPorElector()
            );
        }
    }

    /**
     * Request body for adding a candidate.
     * V5: {@code funcionarioId} (nullable) replaces {@code numeroOrden};
     * {@code afiliacionPolitica} removed.
     */
    public record AddCandidateRequest(
            String nombre,
            String descripcion,
            Integer funcionarioId,
            String fotoUrl,
            String biografia,
            String propuestas
    ) {}

    /**
     * Response projection for a created candidate.
     * V5: {@code funcionarioId} replaces {@code numeroOrden}; {@code afiliacionPolitica} removed.
     */
    public record CandidateResponse(
            String id,
            String nombre,
            Integer funcionarioId,
            String fotoUrl,
            String biografia,
            String propuestas
    ) {}

    /** Request body for updating an election. All fields are optional. */
    public record UpdateElectionRequest(
            String nombre,
            String fechaInicio,
            String fechaFin
    ) {}
}
