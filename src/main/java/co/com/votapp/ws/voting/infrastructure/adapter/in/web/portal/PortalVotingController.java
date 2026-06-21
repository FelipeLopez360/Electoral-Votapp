package co.com.votapp.ws.voting.infrastructure.adapter.in.web.portal;

import co.com.votapp.ws.auth.infrastructure.adapter.in.web.PortalAuthFilter;
import co.com.votapp.ws.common.exception.NotFoundException;
import co.com.votapp.ws.electoral.domain.Candidate;
import co.com.votapp.ws.electoral.domain.Election;
import co.com.votapp.ws.electoral.domain.port.out.CandidateRepositoryPort;
import co.com.votapp.ws.electoral.domain.port.out.ElectionRepositoryPort;
import co.com.votapp.ws.voting.application.service.CastVoteAppService;
import co.com.votapp.ws.voting.domain.VotingToken;
import co.com.votapp.ws.voting.domain.port.out.ParticipacionRepositoryPort;
import co.com.votapp.ws.voting.domain.port.out.VotingTokenRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST adapter for portal voting operations.
 *
 * <p>All endpoints in this controller are protected by
 * {@link PortalAuthFilter}, which resolves the authenticated funcionario ID
 * from the Redis session and stores it in the {@code portalFuncionarioId} request attribute.
 *
 * <p>Controllers are THIN — business logic lives in the use cases and app service.
 */
@RestController
@RequestMapping("/api/v1/portal")
@Tag(name = "Portal Voting", description = "Funcionario self-service voting portal")
public class PortalVotingController {

    private final VotingTokenRepository votingTokenRepository;
    private final ElectionRepositoryPort electionRepository;
    private final CandidateRepositoryPort candidateRepository;
    private final ParticipacionRepositoryPort participacionRepository;
    private final CastVoteAppService castVoteAppService;

    public PortalVotingController(VotingTokenRepository votingTokenRepository,
                                  ElectionRepositoryPort electionRepository,
                                  CandidateRepositoryPort candidateRepository,
                                  ParticipacionRepositoryPort participacionRepository,
                                  CastVoteAppService castVoteAppService) {
        this.votingTokenRepository = votingTokenRepository;
        this.electionRepository = electionRepository;
        this.candidateRepository = candidateRepository;
        this.participacionRepository = participacionRepository;
        this.castVoteAppService = castVoteAppService;
    }

    // ── GET /api/v1/portal/mis-elecciones ─────────────────────────────────────

    @GetMapping("/mis-elecciones")
    @Operation(
            summary = "My assigned elections",
            description = "Returns all elections where the authenticated funcionario has an assigned token, "
                    + "plus the voting status for each. NEVER exposes rawToken or hash."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of assigned elections"),
            @ApiResponse(responseCode = "401", description = "Session token missing or expired")
    })
    public ResponseEntity<MisEleccionesResponse> misElecciones(HttpServletRequest request) {
        Integer funcionarioId = resolvedFuncionarioId(request);

        List<VotingToken> tokens = votingTokenRepository.findAllByFuncionarioId(funcionarioId);

        List<EleccionItem> items = tokens.stream()
                .map(token -> {
                    Election election = electionRepository.findById(token.eleccionId())
                            .orElse(null);
                    if (election == null) return null;

                    boolean yaVoto = participacionRepository.hasParticipated(
                            token.eleccionId(), token.funcionarioId());

                    return new EleccionItem(
                            token.eleccionId().toString(),
                            election.nombre(),
                            election.status().name(),
                            token.status().name(),
                            yaVoto
                    );
                })
                .filter(item -> item != null)
                .toList();

        return ResponseEntity.ok(new MisEleccionesResponse(items));
    }

    // ── GET /api/v1/portal/ballot/{eleccionId} ───────────────────────────────

    @GetMapping("/ballot/{eleccionId}")
    @Operation(
            summary = "Get portal ballot",
            description = "Returns the ordered list of candidates for an election that the authenticated "
                    + "funcionario has an ISSUED token for. Requires a valid portal session. "
                    + "NEVER returns rawToken or token hash."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ballot returned"),
            @ApiResponse(responseCode = "401", description = "Session token missing or expired"),
            @ApiResponse(responseCode = "404", description = "No ISSUED token for this election")
    })
    public ResponseEntity<PortalBallotResponse> portalBallot(
            @PathVariable UUID eleccionId,
            HttpServletRequest request) {

        Integer funcionarioId = resolvedFuncionarioId(request);

        // Verify this funcionario has an ISSUED token for this election
        votingTokenRepository.findIssuedByFuncionarioAndEleccion(funcionarioId, eleccionId)
                .orElseThrow(() -> new NotFoundException("No tenés un token asignado para esta elección"));

        Election election = electionRepository.findById(eleccionId)
                .orElseThrow(() -> new NotFoundException("Elección no encontrada"));

        List<Candidate> candidates = candidateRepository.findByEleccionIdOrderByNumeroOrden(eleccionId);

        List<PortalCandidateItem> items = candidates.stream()
                .map(c -> new PortalCandidateItem(
                        c.id().toString(), c.nombre(), c.esVotoEnBlanco(), c.numeroOrden(),
                        c.fotoUrl(), c.biografia(), c.propuestas(), c.afiliacionPolitica()))
                .toList();

        return ResponseEntity.ok(new PortalBallotResponse(
                election.id().toString(),
                election.nombre(),
                election.maxVotosPorElector(),
                election.permiteVotoBlanco(),
                items
        ));
    }

    // ── POST /api/v1/portal/votar/{eleccionId} ───────────────────────────────

    @PostMapping("/votar/{eleccionId}")
    @Operation(
            summary = "Cast vote via portal (multi-selection)",
            description = "Casts one or more anonymous votes for the authenticated funcionario. "
                    + "Accepts a JSON body with a list of candidate UUIDs (candidatoIds). "
                    + "The rawToken is resolved internally by looking up the ISSUED token "
                    + "for this funcionario+election pair. All votes are cast atomically."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Vote(s) cast successfully"),
            @ApiResponse(responseCode = "401", description = "Session token missing or expired"),
            @ApiResponse(responseCode = "404", description = "No assigned token for this election"),
            @ApiResponse(responseCode = "409", description = "Domain rule violated — already voted, election not active, blank/null exclusivity, max votes exceeded, etc.")
    })
    public ResponseEntity<Void> votar(
            @PathVariable UUID eleccionId,
            @RequestBody VotarRequest votarRequest,
            HttpServletRequest request) {

        Integer funcionarioId = resolvedFuncionarioId(request);

        // Resolve the ISSUED token for this funcionario + election
        VotingToken token = votingTokenRepository
                .findIssuedByFuncionarioAndEleccion(funcionarioId, eleccionId)
                .orElseThrow(() -> new NotFoundException("No tenés un token asignado para esta elección"));

        // Delegate atomic multi-vote casting to the transactional app service
        castVoteAppService.castVoteByTokenId(token.id(), votarRequest.candidatoIds());

        return ResponseEntity.noContent().build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Reads the funcionario ID placed by PortalAuthFilter. */
    private Integer resolvedFuncionarioId(HttpServletRequest request) {
        return (Integer) request.getAttribute(PortalAuthFilter.FUNCIONARIO_ID_ATTRIBUTE);
    }

    // ── Response DTOs ─────────────────────────────────────────────────────────

    /** Response body for GET /mis-elecciones — never includes rawToken or hash. */
    public record MisEleccionesResponse(List<EleccionItem> elecciones) {}

    /** Single election item for the dashboard. */
    public record EleccionItem(
            String eleccionId,
            String nombre,
            String estadoEleccion,
            String estadoToken,
            boolean yaVoto
    ) {}

    /**
     * Request body for POST /portal/votar/{eleccionId}.
     *
     * <p>Contains the list of selected candidate UUIDs. At least one ID is required.
     * Blank vote mutual exclusivity is enforced by the domain use case.
     */
    public record VotarRequest(List<UUID> candidatoIds) {}

    /**
     * Response body for GET /portal/ballot/{eleccionId}.
     *
     * <p>Includes ballot configuration ({@code maxVotosPorElector}, {@code permiteVotoBlanco})
     * used by the VotePage to drive multi-select and blank-vote exclusivity UI logic.
     */
    public record PortalBallotResponse(
            String eleccionId,
            String nombre,
            int maxVotosPorElector,
            boolean permiteVotoBlanco,
            List<PortalCandidateItem> candidates
    ) {}

    /**
     * Single candidate item in the portal ballot.
     *
     * <p>Includes rich profile fields for candidate cards.
     * Synthetic candidates (blank vote, null vote) have {@code null} for all rich fields.
     */
    public record PortalCandidateItem(
            String id,
            String nombre,
            boolean esVotoEnBlanco,
            int numeroOrden,
            String fotoUrl,
            String biografia,
            String propuestas,
            String afiliacionPolitica
    ) {}
}
