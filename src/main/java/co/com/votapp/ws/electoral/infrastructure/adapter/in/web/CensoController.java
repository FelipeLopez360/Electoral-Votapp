package co.com.votapp.ws.electoral.infrastructure.adapter.in.web;

import co.com.votapp.ws.auth.domain.Funcionario;
import co.com.votapp.ws.auth.domain.port.out.FuncionarioRepositoryPort;
import co.com.votapp.ws.electoral.application.dto.BulkAddCensoRequest;
import co.com.votapp.ws.electoral.application.dto.BulkAddCensoResponse;
import co.com.votapp.ws.electoral.application.dto.CensoEntryResponse;
import co.com.votapp.ws.electoral.domain.model.CensoEntry;
import co.com.votapp.ws.common.domain.model.PageResult;
import co.com.votapp.ws.electoral.domain.port.in.ManageCensoUseCase;
import co.com.votapp.ws.organization.domain.Departamento;
import co.com.votapp.ws.organization.domain.port.out.DepartamentoRepositoryPort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST adapter for election census (censo electoral) management.
 *
 * <p>All endpoints require admin credentials (HTTP Basic).
 * Census mutations are restricted to elections in PROGRAMADA state —
 * the use case enforces this; violations surface as HTTP 409 via GlobalExceptionHandler.
 */
@RestController
@RequestMapping("/api/v1/elections/{electionId}/censo")
@Tag(name = "Censo Electoral", description = "Election census management (MVP)")
public class CensoController {

    private final ManageCensoUseCase manageCensoUseCase;
    private final FuncionarioRepositoryPort funcionarioRepository;
    private final DepartamentoRepositoryPort departamentoRepository;

    public CensoController(ManageCensoUseCase manageCensoUseCase,
                           FuncionarioRepositoryPort funcionarioRepository,
                           DepartamentoRepositoryPort departamentoRepository) {
        this.manageCensoUseCase = manageCensoUseCase;
        this.funcionarioRepository = funcionarioRepository;
        this.departamentoRepository = departamentoRepository;
    }

    // ── POST /bulk ────────────────────────────────────────────────────────────

    @PostMapping("/bulk")
    @Operation(
            summary = "Bulk-add funcionarios to census by filter",
            description = "Adds all eligible funcionarios matching the given filters to the election's census. "
                    + "Idempotent — duplicates are skipped. Election must be in PROGRAMADA state.",
            security = @SecurityRequirement(name = "basicAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bulk add completed, counts returned"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "404", description = "Election not found"),
            @ApiResponse(responseCode = "409", description = "Election is not in PROGRAMADA state")
    })
    public ResponseEntity<BulkAddCensoResponse> bulkAdd(
            @PathVariable UUID electionId,
            @Valid @RequestBody BulkAddCensoRequest request) {

        String estadoLaboral = request.estadoLaboral() != null ? request.estadoLaboral() : "ACTIVO";
        Boolean puedeVotar = request.puedeVotar() != null ? request.puedeVotar() : Boolean.TRUE;

        ManageCensoUseCase.BulkAddResult result = manageCensoUseCase.addByFilters(
                electionId,
                request.departamentoId(),
                estadoLaboral,
                puedeVotar,
                null   // adminId — nullable in MVP (no principal extraction yet)
        );

        return ResponseEntity.ok(new BulkAddCensoResponse(result.added(), result.skipped(), result.total()));
    }

    // ── POST /{funcionarioId} ─────────────────────────────────────────────────

    @PostMapping("/{funcionarioId}")
    @Operation(
            summary = "Add a single funcionario to the census",
            description = "Individually adds an eligible funcionario to the election's census. "
                    + "The funcionario must be ACTIVO and puede_votar=true.",
            security = @SecurityRequirement(name = "basicAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Funcionario added to census"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "404", description = "Election or funcionario not found"),
            @ApiResponse(responseCode = "409", description = "Election is not PROGRAMADA or funcionario is ineligible")
    })
    public ResponseEntity<Void> addIndividual(
            @PathVariable UUID electionId,
            @PathVariable Integer funcionarioId) {

        manageCensoUseCase.addIndividual(electionId, funcionarioId, null);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // ── DELETE /{funcionarioId} ───────────────────────────────────────────────

    @DeleteMapping("/{funcionarioId}")
    @Operation(
            summary = "Remove a single funcionario from the census",
            description = "Removes a funcionario from the election's census. "
                    + "Idempotent — no error if the funcionario is not in census. "
                    + "Election must be in PROGRAMADA state.",
            security = @SecurityRequirement(name = "basicAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Funcionario removed"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "404", description = "Election not found"),
            @ApiResponse(responseCode = "409", description = "Election is not PROGRAMADA")
    })
    public ResponseEntity<Void> removeIndividual(
            @PathVariable UUID electionId,
            @PathVariable Integer funcionarioId) {

        manageCensoUseCase.removeIndividual(electionId, funcionarioId);
        return ResponseEntity.noContent().build();
    }

    // ── DELETE / ──────────────────────────────────────────────────────────────

    @DeleteMapping
    @Operation(
            summary = "Clear all census entries for an election",
            description = "Removes all funcionarios from the election's census. "
                    + "Election must be in PROGRAMADA state.",
            security = @SecurityRequirement(name = "basicAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Census cleared"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "404", description = "Election not found"),
            @ApiResponse(responseCode = "409", description = "Election is not PROGRAMADA")
    })
    public ResponseEntity<Void> clearCenso(@PathVariable UUID electionId) {
        manageCensoUseCase.clearCenso(electionId);
        return ResponseEntity.noContent().build();
    }

    // ── GET / ─────────────────────────────────────────────────────────────────

    @GetMapping
    @Operation(
            summary = "List census entries for an election",
            description = "Returns a paginated list of all funcionarios in the election's census.",
            security = @SecurityRequirement(name = "basicAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated census list"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "404", description = "Election not found")
    })
    public ResponseEntity<PageResult<CensoEntryResponse>> listCenso(
            @PathVariable UUID electionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageResult<CensoEntry> entries = manageCensoUseCase.listCenso(electionId, page, size);

        // Build lookup caches for funcionario + departamento enrichment
        Map<Integer, Funcionario> funcionarioCache = entries.content().stream()
                .map(CensoEntry::funcionarioId)
                .distinct()
                .map(funcionarioRepository::findById)
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .collect(Collectors.toMap(Funcionario::getId, f -> f));

        Map<Integer, String> departamentoNombres = departamentoRepository.findAllByActivoTrue().stream()
                .collect(Collectors.toMap(Departamento::getId, Departamento::getNombre));

        PageResult<CensoEntryResponse> response = entries.map(
                entry -> toResponse(entry, funcionarioCache, departamentoNombres));
        return ResponseEntity.ok(response);
    }

    // ── GET /count ────────────────────────────────────────────────────────────

    @GetMapping("/count")
    @Operation(
            summary = "Count census entries for an election",
            description = "Returns the total number of funcionarios in the election's census.",
            security = @SecurityRequirement(name = "basicAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Census entry count"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "404", description = "Election not found")
    })
    public ResponseEntity<CountResponse> countCenso(@PathVariable UUID electionId) {
        long count = manageCensoUseCase.countCenso(electionId);
        return ResponseEntity.ok(new CountResponse(count));
    }

    // ── Mapping helpers ───────────────────────────────────────────────────────

    private CensoEntryResponse toResponse(CensoEntry entry, Map<Integer, Funcionario> funcionarioCache,
                                           Map<Integer, String> departamentoNombres) {
        Funcionario f = funcionarioCache.get(entry.funcionarioId());
        String deptNombre = "";
        if (f != null && f.getDepartamentoId() != null) {
            deptNombre = departamentoNombres.getOrDefault(f.getDepartamentoId(), "");
        }
        return new CensoEntryResponse(
                entry.id(),
                entry.funcionarioId(),
                f != null ? f.getNumeroEmpleado() : "",
                f != null ? f.getNombres() : "",
                f != null ? f.getApellidos() : "",
                deptNombre,
                entry.fechaAgregado()
        );
    }

    // ── Response records (controller-local) ──────────────────────────────────

    /**
     * Response for the count endpoint.
     *
     * @param count total entries in the census
     */
    public record CountResponse(long count) {}
}
