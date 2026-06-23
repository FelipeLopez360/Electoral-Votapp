package co.com.votapp.ws.electoral.infrastructure;

import co.com.votapp.ws.candidates.infrastructure.adapter.out.persistence.CandidatoEntity;
import co.com.votapp.ws.candidates.infrastructure.adapter.out.persistence.CandidatoJpaRepository;
import co.com.votapp.ws.electoral.domain.Candidate;
import co.com.votapp.ws.electoral.infrastructure.adapter.out.persistence.CandidateRepositoryAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CandidateRepositoryAdapter} after V5 changes.
 *
 * <p>Sorting contract: real candidates alphabetically by nombre ASC, synthetic candidates last.
 *
 * <p>RED cycle: written first. Will fail until CandidateRepositoryAdapter is updated.
 */
@DisplayName("CandidateRepositoryAdapter - Alphabetical sorting, synthetic-last")
@ExtendWith(MockitoExtension.class)
class CandidateRepositoryAdapterTest {

    @Mock
    private CandidatoJpaRepository jpaRepository;

    private CandidateRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new CandidateRepositoryAdapter(jpaRepository);
    }

    @Test
    @DisplayName("Should sort real candidates alphabetically by nombre ASC, synthetics last")
    void findByEleccionIdOrderByNombre_shouldReturnAlphabeticallyWithSyntheticsLast() {
        // Given
        UUID eleccionId = UUID.randomUUID();
        var entities = List.of(
                candidatoEntity(eleccionId, "Voto en Blanco", true, false, null),
                candidatoEntity(eleccionId, "Carlos Mendez", false, false, 3),
                candidatoEntity(eleccionId, "Ana González", false, false, 1),
                candidatoEntity(eleccionId, "Voto Nulo", false, true, null),
                candidatoEntity(eleccionId, "Beatriz López", false, false, 2)
        );
        when(jpaRepository.findByEleccionId(eleccionId)).thenReturn(entities);

        // When
        List<Candidate> result = adapter.findByEleccionIdOrderByNombre(eleccionId);

        // Then — real candidates first (alphabetically), synthetics last
        assertThat(result).hasSize(5);
        assertThat(result.get(0).nombre()).isEqualTo("Ana González");
        assertThat(result.get(1).nombre()).isEqualTo("Beatriz López");
        assertThat(result.get(2).nombre()).isEqualTo("Carlos Mendez");
        // Last two are synthetics (blank/null vote — order between them is stable)
        assertThat(result.get(3).esVotoEnBlanco() || result.get(3).esVotoNulo()).isTrue();
        assertThat(result.get(4).esVotoEnBlanco() || result.get(4).esVotoNulo()).isTrue();
    }

    @Test
    @DisplayName("Should return empty list when no candidates exist for election")
    void findByEleccionIdOrderByNombre_shouldReturnEmpty_whenNoCandidates() {
        // Given
        UUID eleccionId = UUID.randomUUID();
        when(jpaRepository.findByEleccionId(eleccionId)).thenReturn(List.of());

        // When
        List<Candidate> result = adapter.findByEleccionIdOrderByNombre(eleccionId);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("existsByEleccionIdAndFuncionarioId should return true when match found")
    void existsByEleccionIdAndFuncionarioId_shouldReturnTrue_whenFuncionarioAlreadyCandidate() {
        // Given
        UUID eleccionId = UUID.randomUUID();
        var entities = List.of(
                candidatoEntity(eleccionId, "Juan Pérez", false, false, 5)
        );
        when(jpaRepository.findByEleccionId(eleccionId)).thenReturn(entities);

        // When
        boolean result = adapter.existsByEleccionIdAndFuncionarioId(eleccionId, 5);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("existsByEleccionIdAndFuncionarioId should return false when no match")
    void existsByEleccionIdAndFuncionarioId_shouldReturnFalse_whenFuncionarioNotCandidate() {
        // Given
        UUID eleccionId = UUID.randomUUID();
        var entities = List.of(
                candidatoEntity(eleccionId, "Juan Pérez", false, false, 5)
        );
        when(jpaRepository.findByEleccionId(eleccionId)).thenReturn(entities);

        // When
        boolean result = adapter.existsByEleccionIdAndFuncionarioId(eleccionId, 99);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should map funcionarioId correctly from entity to domain")
    void save_shouldMapFuncionarioId() {
        // Given — test the toDomain mapping indirectly via findByEleccionId
        UUID eleccionId = UUID.randomUUID();
        var entity = candidatoEntity(eleccionId, "María García", false, false, 7);
        when(jpaRepository.findByEleccionId(eleccionId)).thenReturn(List.of(entity));

        // When
        List<Candidate> result = adapter.findByEleccionIdOrderByNombre(eleccionId);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).funcionarioId()).isEqualTo(7);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private CandidatoEntity candidatoEntity(UUID eleccionId, String nombre,
                                             boolean esVotoEnBlanco, boolean esVotoNulo,
                                             Integer funcionarioId) {
        CandidatoEntity entity = new CandidatoEntity();
        entity.setId(UUID.randomUUID());
        entity.setEleccionId(eleccionId);
        entity.setNombre(nombre);
        entity.setEsVotoEnBlanco(esVotoEnBlanco);
        entity.setEsVotoNulo(esVotoNulo);
        entity.setFuncionarioId(funcionarioId);
        return entity;
    }
}
