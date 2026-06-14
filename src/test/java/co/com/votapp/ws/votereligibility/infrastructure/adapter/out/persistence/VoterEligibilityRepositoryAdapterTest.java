package co.com.votapp.ws.votereligibility.infrastructure.adapter.out.persistence;

import co.com.votapp.ws.auth.infrastructure.adapter.out.persistence.FuncionarioEntity;
import co.com.votapp.ws.auth.infrastructure.adapter.out.persistence.FuncionarioJpaRepository;
import co.com.votapp.ws.electoral.infrastructure.adapter.out.persistence.CensoJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@DisplayName("VoterEligibilityRepositoryAdapter - Global and election-scoped eligibility")
@ExtendWith(MockitoExtension.class)
class VoterEligibilityRepositoryAdapterTest {

    @Mock
    private FuncionarioJpaRepository funcionarioJpaRepository;

    @Mock
    private CensoJpaRepository censoJpaRepository;

    private VoterEligibilityRepositoryAdapter adapter;

    private static final Long FUNCIONARIO_ID = 10L;
    private static final UUID ELECCION_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        adapter = new VoterEligibilityRepositoryAdapter(funcionarioJpaRepository, censoJpaRepository);
    }

    // ─── isEligible (global) ──────────────────────────────────────────────────

    @Test
    @DisplayName("isEligible should return true when funcionario is ACTIVO and puede_votar=true")
    void isEligible_shouldReturnTrue_whenActivoAndPuedeVotar() {
        // Given
        when(funcionarioJpaRepository.findById(FUNCIONARIO_ID.intValue()))
                .thenReturn(Optional.of(activoFuncionario()));

        // When & Then
        assertThat(adapter.isEligible(FUNCIONARIO_ID)).isTrue();
    }

    @Test
    @DisplayName("isEligible should return false when funcionario is INACTIVO")
    void isEligible_shouldReturnFalse_whenInactivo() {
        // Given
        when(funcionarioJpaRepository.findById(FUNCIONARIO_ID.intValue()))
                .thenReturn(Optional.of(inactivoFuncionario()));

        // When & Then
        assertThat(adapter.isEligible(FUNCIONARIO_ID)).isFalse();
    }

    @Test
    @DisplayName("isEligible should return false when funcionario does not exist")
    void isEligible_shouldReturnFalse_whenFuncionarioNotFound() {
        // Given
        when(funcionarioJpaRepository.findById(FUNCIONARIO_ID.intValue()))
                .thenReturn(Optional.empty());

        // When & Then
        assertThat(adapter.isEligible(FUNCIONARIO_ID)).isFalse();
    }

    // ─── isEligibleForElection — census exists path ───────────────────────────

    @Test
    @DisplayName("isEligibleForElection should return true when census exists and funcionario is in census")
    void isEligibleForElection_shouldReturnTrue_whenCensusExistsAndFuncionarioInCensus() {
        // Given — global eligible, census has entries, funcionario IS in census
        when(funcionarioJpaRepository.findById(FUNCIONARIO_ID.intValue()))
                .thenReturn(Optional.of(activoFuncionario()));
        when(censoJpaRepository.countByEleccionId(ELECCION_ID)).thenReturn(5L);
        when(censoJpaRepository.existsByEleccionIdAndFuncionarioId(ELECCION_ID, FUNCIONARIO_ID.intValue()))
                .thenReturn(true);

        // When & Then
        assertThat(adapter.isEligibleForElection(FUNCIONARIO_ID, ELECCION_ID)).isTrue();
    }

    @Test
    @DisplayName("isEligibleForElection should return false when census exists but funcionario NOT in census")
    void isEligibleForElection_shouldReturnFalse_whenCensusExistsButFuncionarioNotInCensus() {
        // Given — global eligible, census has entries, but funcionario is NOT in census
        when(funcionarioJpaRepository.findById(FUNCIONARIO_ID.intValue()))
                .thenReturn(Optional.of(activoFuncionario()));
        when(censoJpaRepository.countByEleccionId(ELECCION_ID)).thenReturn(3L);
        when(censoJpaRepository.existsByEleccionIdAndFuncionarioId(ELECCION_ID, FUNCIONARIO_ID.intValue()))
                .thenReturn(false);

        // When & Then
        assertThat(adapter.isEligibleForElection(FUNCIONARIO_ID, ELECCION_ID)).isFalse();
    }

    // ─── isEligibleForElection — empty census fallback ────────────────────────

    @Test
    @DisplayName("isEligibleForElection should return true when census is empty (backward-compat fallback)")
    void isEligibleForElection_shouldReturnTrue_whenCensusIsEmpty() {
        // Given — global eligible, census is EMPTY → fallback to global
        when(funcionarioJpaRepository.findById(FUNCIONARIO_ID.intValue()))
                .thenReturn(Optional.of(activoFuncionario()));
        when(censoJpaRepository.countByEleccionId(ELECCION_ID)).thenReturn(0L);

        // When & Then
        assertThat(adapter.isEligibleForElection(FUNCIONARIO_ID, ELECCION_ID)).isTrue();
    }

    // ─── isEligibleForElection — globally ineligible ─────────────────────────

    @Test
    @DisplayName("isEligibleForElection should return false when funcionario is globally ineligible (even if in census)")
    void isEligibleForElection_shouldReturnFalse_whenGloballyIneligible() {
        // Given — INACTIVO: global check fails, no need to query census
        when(funcionarioJpaRepository.findById(FUNCIONARIO_ID.intValue()))
                .thenReturn(Optional.of(inactivoFuncionario()));

        // When & Then
        assertThat(adapter.isEligibleForElection(FUNCIONARIO_ID, ELECCION_ID)).isFalse();
    }

    @Test
    @DisplayName("isEligibleForElection should return false when funcionario not found")
    void isEligibleForElection_shouldReturnFalse_whenFuncionarioNotFound() {
        // Given
        when(funcionarioJpaRepository.findById(FUNCIONARIO_ID.intValue()))
                .thenReturn(Optional.empty());

        // When & Then
        assertThat(adapter.isEligibleForElection(FUNCIONARIO_ID, ELECCION_ID)).isFalse();
    }

    // ─── Test data factories ──────────────────────────────────────────────────

    private FuncionarioEntity activoFuncionario() {
        FuncionarioEntity f = new FuncionarioEntity();
        f.setId(FUNCIONARIO_ID.intValue());
        f.setEstadoLaboral("ACTIVO");
        f.setPuedeVotar(true);
        return f;
    }

    private FuncionarioEntity inactivoFuncionario() {
        FuncionarioEntity f = new FuncionarioEntity();
        f.setId(FUNCIONARIO_ID.intValue());
        f.setEstadoLaboral("INACTIVO");
        f.setPuedeVotar(true);
        return f;
    }
}
