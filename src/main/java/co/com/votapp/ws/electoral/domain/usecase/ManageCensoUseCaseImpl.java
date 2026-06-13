package co.com.votapp.ws.electoral.domain.usecase;

import co.com.votapp.ws.auth.domain.Funcionario;
import co.com.votapp.ws.auth.domain.port.out.FuncionarioRepositoryPort;
import co.com.votapp.ws.common.exception.DomainException;
import co.com.votapp.ws.electoral.domain.ElectionStatus;
import co.com.votapp.ws.electoral.domain.exception.ElectionNotModifiableException;
import co.com.votapp.ws.electoral.domain.model.CensoEntry;
import co.com.votapp.ws.electoral.domain.port.in.ManageCensoUseCase;
import co.com.votapp.ws.electoral.domain.port.out.CensoRepositoryPort;
import co.com.votapp.ws.electoral.domain.port.out.ElectionRepositoryPort;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Domain use case implementation for census management.
 *
 * <p>Enforces the business rule that census mutations are only permitted when
 * the election is in {@link ElectionStatus#PROGRAMADA} state.
 *
 * <p>ZERO Spring annotations — wired manually via {@code DomainConfig}.
 * Dependencies are injected via constructor.
 */
public class ManageCensoUseCaseImpl implements ManageCensoUseCase {

    private final ElectionRepositoryPort electionRepository;
    private final FuncionarioRepositoryPort funcionarioRepository;
    private final CensoRepositoryPort censoRepository;

    public ManageCensoUseCaseImpl(ElectionRepositoryPort electionRepository,
                                   FuncionarioRepositoryPort funcionarioRepository,
                                   CensoRepositoryPort censoRepository) {
        this.electionRepository = electionRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.censoRepository = censoRepository;
    }

    @Override
    public BulkAddResult addByDepartamento(UUID eleccionId, Integer departamentoId, Integer adminId) {
        guardProgramada(eleccionId);

        List<Funcionario> eligible = funcionarioRepository.findEligibleByDepartamento(departamentoId);
        return bulkAdd(eleccionId, eligible, adminId);
    }

    @Override
    public BulkAddResult addByFilters(UUID eleccionId, Integer departamentoId,
                                       String estadoLaboral, Boolean puedeVotar, Integer adminId) {
        guardProgramada(eleccionId);

        List<Funcionario> eligible = funcionarioRepository.findEligibleByFilters(
                departamentoId, estadoLaboral, puedeVotar);
        return bulkAdd(eleccionId, eligible, adminId);
    }

    @Override
    public void addIndividual(UUID eleccionId, Integer funcionarioId, Integer adminId) {
        guardProgramada(eleccionId);

        Funcionario funcionario = funcionarioRepository.findById(funcionarioId)
                .orElseThrow(() -> new DomainException(
                        "Funcionario not found: " + funcionarioId));

        if (!funcionario.isActivo() || !funcionario.isPuedeVotar()) {
            throw new DomainException(
                    "Funcionario " + funcionarioId + " is not eligible: must be ACTIVO and puede_votar=true");
        }

        CensoEntry entry = new CensoEntry(null, eleccionId, funcionarioId, adminId, Instant.now());
        censoRepository.save(entry);
    }

    @Override
    public void removeIndividual(UUID eleccionId, Integer funcionarioId) {
        guardProgramada(eleccionId);
        censoRepository.deleteByEleccionIdAndFuncionarioId(eleccionId, funcionarioId);
    }

    @Override
    public void clearCenso(UUID eleccionId) {
        guardProgramada(eleccionId);
        censoRepository.deleteAllByEleccionId(eleccionId);
    }

    @Override
    public Page<CensoEntry> listCenso(UUID eleccionId, int page, int size) {
        return censoRepository.findByEleccionId(eleccionId, page, size);
    }

    @Override
    public long countCenso(UUID eleccionId) {
        return censoRepository.countByEleccionId(eleccionId);
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    /**
     * Load the election and assert it is in PROGRAMADA state.
     * Throws {@link ElectionNotModifiableException} for non-PROGRAMADA elections.
     * Throws {@link DomainException} when the election does not exist.
     */
    private void guardProgramada(UUID eleccionId) {
        var election = electionRepository.findById(eleccionId)
                .orElseThrow(() -> new DomainException("Election not found: " + eleccionId));

        if (election.status() != ElectionStatus.PROGRAMADA) {
            throw new ElectionNotModifiableException(eleccionId, election.status());
        }
    }

    /**
     * Perform the actual bulk-add: filter out duplicates, save new entries, return counts.
     */
    private BulkAddResult bulkAdd(UUID eleccionId, List<Funcionario> candidates, Integer adminId) {
        int total = candidates.size();
        int skipped = 0;

        List<CensoEntry> toAdd = candidates.stream()
                .filter(f -> {
                    boolean alreadyIn = censoRepository.existsByEleccionIdAndFuncionarioId(
                            eleccionId, f.getId());
                    return !alreadyIn;
                })
                .map(f -> new CensoEntry(null, eleccionId, f.getId(), adminId, Instant.now()))
                .toList();

        skipped = total - toAdd.size();

        if (!toAdd.isEmpty()) {
            censoRepository.saveAll(toAdd);
        }

        return new BulkAddResult(toAdd.size(), skipped, total);
    }
}
