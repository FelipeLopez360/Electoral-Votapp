package co.com.votapp.ws.auth.infrastructure.adapter.in.web;

import co.com.votapp.ws.auth.application.dto.CreateFuncionarioRequest;
import co.com.votapp.ws.auth.application.dto.FuncionarioResponse;
import co.com.votapp.ws.auth.application.dto.UpdateFuncionarioRequest;
import co.com.votapp.ws.auth.domain.port.in.CreateFuncionarioUseCase;
import co.com.votapp.ws.auth.domain.port.in.UpdateFuncionarioUseCase;
import co.com.votapp.ws.auth.domain.port.out.FuncionarioRepositoryPort;
import co.com.votapp.ws.common.domain.model.PageResult;
import co.com.votapp.ws.common.exception.NotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for funcionario administration (CRUD).
 *
 * <p>Read endpoints (GET list, GET detail) call the output port directly — no business
 * rule lives in reads. Write endpoints (POST, PUT) go through domain use cases.
 *
 * <p>Pattern follows {@code OrganizationController} (reads) and
 * {@code ElectionController} (writes via use cases).
 */
@Tag(name = "Funcionarios", description = "Administración de funcionarios institucionales")
@RestController
@RequestMapping("/api/v1/funcionarios")
public class FuncionarioController {

    private final FuncionarioRepositoryPort funcionarioRepository;
    private final CreateFuncionarioUseCase createFuncionarioUseCase;
    private final UpdateFuncionarioUseCase updateFuncionarioUseCase;

    public FuncionarioController(FuncionarioRepositoryPort funcionarioRepository,
                                  CreateFuncionarioUseCase createFuncionarioUseCase,
                                  UpdateFuncionarioUseCase updateFuncionarioUseCase) {
        this.funcionarioRepository = funcionarioRepository;
        this.createFuncionarioUseCase = createFuncionarioUseCase;
        this.updateFuncionarioUseCase = updateFuncionarioUseCase;
    }

    @Operation(
            summary = "Listar funcionarios",
            description = "Returns a paginated list of funcionarios. Optional search term filters by nombres, "
                    + "apellidos, or documentoIdentidad (case-insensitive). Defaults to page=0, size=8.",
            security = @SecurityRequirement(name = "basicAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated list returned"),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @GetMapping
    public ResponseEntity<PageResult<FuncionarioResponse>> list(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String search) {
        int effectivePage = Math.max(0, page != null ? page : 0);
        int effectiveSize = Math.min(100, Math.max(1, size != null ? size : 8));
        PageResult<FuncionarioResponse> response = funcionarioRepository
                .findAll(effectivePage, effectiveSize, search)
                .map(FuncionarioResponse::fromDomain);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Obtener funcionario",
            description = "Returns the full details of a single funcionario by id.",
            security = @SecurityRequirement(name = "basicAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Funcionario found"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "404", description = "Funcionario not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<FuncionarioResponse> get(@PathVariable Integer id) {
        return funcionarioRepository.findById(id)
                .map(FuncionarioResponse::fromDomain)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new NotFoundException("Funcionario " + id + " no encontrado"));
    }

    @Operation(
            summary = "Crear funcionario",
            description = "Creates a new funcionario. Generates a temporary password returned once "
                    + "in the response (field 'temporary_password'). The funcionario is created "
                    + "with debeCambiarPassword=true.",
            security = @SecurityRequirement(name = "basicAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Funcionario created — temporary_password included in response"),
            @ApiResponse(responseCode = "400", description = "Missing required fields"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "409", description = "documentoIdentidad already exists")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<FuncionarioResponse> create(@RequestBody CreateFuncionarioRequest request) {
        CreateFuncionarioUseCase.Command command = new CreateFuncionarioUseCase.Command(
                request.nombres(),
                request.apellidos(),
                request.tipoDocumento(),
                request.documentoIdentidad(),
                request.email(),
                request.telefono(),
                request.departamentoId(),
                request.cargoId(),
                request.estadoLaboral(),
                request.puedeVotar()
        );
        CreateFuncionarioUseCase.Result result = createFuncionarioUseCase.create(command);
        FuncionarioResponse response = FuncionarioResponse.fromDomainWithPassword(
                result.funcionario(), result.temporaryPassword());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Actualizar funcionario",
            description = "Updates editable fields of an existing funcionario. "
                    + "Null fields are ignored (partial update). Password is never modified here.",
            security = @SecurityRequirement(name = "basicAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Funcionario updated"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "404", description = "Funcionario not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<FuncionarioResponse> update(
            @PathVariable Integer id,
            @RequestBody UpdateFuncionarioRequest request) {
        UpdateFuncionarioUseCase.Command command = new UpdateFuncionarioUseCase.Command(
                id,
                request.nombres(),
                request.apellidos(),
                request.email(),
                request.telefono(),
                request.departamentoId(),
                request.cargoId(),
                request.estadoLaboral(),
                request.puedeVotar(),
                request.debeCambiarPassword()
        );
        return ResponseEntity.ok(FuncionarioResponse.fromDomain(updateFuncionarioUseCase.update(command)));
    }
}
