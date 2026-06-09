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
                .map(c -> new PortalCandidateItem(c.id().toString(), c.nombre(), c.esVotoEnBlanco(), c.numeroOrden()))
                .toList();

        return ResponseEntity.ok(new PortalBallotResponse(
                election.id().toString(),
                election.nombre(),
                items
        ));
    }

    // ── POST /api/v1/portal/votar/{eleccionId}/{candidatoId} ─────────────────

    @PostMapping("/votar/{eleccionId}/{candidatoId}")
    @Operation(
            summary = "Cast vote via portal",
            description = "Casts an anonymous vote for the authenticated funcionario. "
                    + "The rawToken is resolved internally by looking up the ISSUED token "
                    + "for this funcionario+election pair. The vote is cast atomically."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Vote cast successfully"),
            @ApiResponse(responseCode = "401", description = "Session token missing or expired"),
            @ApiResponse(responseCode = "404", description = "No assigned token for this election"),
            @ApiResponse(responseCode = "409", description = "Domain rule violated — already voted, election not active, etc.")
    })
    public ResponseEntity<Void> votar(
            @PathVariable UUID eleccionId,
            @PathVariable UUID candidatoId,
            HttpServletRequest request) {

        Integer funcionarioId = resolvedFuncionarioId(request);

        // Resolve the ISSUED token for this funcionario + election
        VotingToken token = votingTokenRepository
                .findIssuedByFuncionarioAndEleccion(funcionarioId, eleccionId)
                .orElseThrow(() -> new NotFoundException("No tenés un token asignado para esta elección"));

        // Delegate atomic vote-casting to the transactional app service
        castVoteAppService.castVoteByTokenId(token.id(), candidatoId);

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

    /** Response body for GET /portal/ballot/{eleccionId}. */
    public record PortalBallotResponse(
            String eleccionId,
            String nombre,
            List<PortalCandidateItem> candidates
    ) {}

    /** Single candidate item in the portal ballot. */
    public record PortalCandidateItem(
            String id,
            String nombre,
            boolean esVotoEnBlanco,
            int numeroOrden
    ) {}
}
