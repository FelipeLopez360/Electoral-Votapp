package co.com.votapp.ws.electoral.infrastructure.adapter.out.persistence;

import co.com.votapp.ws.candidates.infrastructure.adapter.out.persistence.CandidatoEntity;
import co.com.votapp.ws.candidates.infrastructure.adapter.out.persistence.CandidatoJpaRepository;
import co.com.votapp.ws.electoral.domain.Candidate;
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
 * Unit tests for {@link CandidateRepositoryAdapter}.
 *
 * <p>V5: Sorting is now alphabetical by nombre (synthetics last), not by numeroOrden.
 */
@DisplayName("CandidateRepositoryAdapter - Alphabetical ballot order and synthetic candidate placement")
@ExtendWith(MockitoExtension.class)
class CandidateRepositoryAdapterTest {

    @Mock
    private CandidatoJpaRepository jpaRepository;

    private CandidateRepositoryAdapter adapter;

    private final UUID eleccionId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        adapter = new CandidateRepositoryAdapter(jpaRepository);
    }

    @Test
    @DisplayName("Should place blank vote candidate (esVotoEnBlanco=true) after all real candidates alphabetically")
    void findByEleccionIdOrderByNombre_shouldPlaceBlankVoteLast_whenMixedCandidates() {
        // Given — real candidates + blank vote
        CandidatoEntity realA = buildEntity(UUID.randomUUID(), "Candidate A", false, false, 1);
        CandidatoEntity realB = buildEntity(UUID.randomUUID(), "Candidate B", false, false, 2);
        CandidatoEntity blank = buildEntity(UUID.randomUUID(), "Voto en Blanco", true, false, null);

        when(jpaRepository.findByEleccionId(eleccionId)).thenReturn(List.of(blank, realA, realB));

        // When
        List<Candidate> result = adapter.findByEleccionIdOrderByNombre(eleccionId);

        // Then — real candidates first (alphabetically), synthetics last
        assertThat(result).hasSize(3);
        assertThat(result.get(0).nombre()).isEqualTo("Candidate A");
        assertThat(result.get(1).nombre()).isEqualTo("Candidate B");
        assertThat(result.get(2).nombre()).isEqualTo("Voto en Blanco");
    }

    @Test
    @DisplayName("Should place null vote candidate (esVotoNulo=true) after all real candidates alphabetically")
    void findByEleccionIdOrderByNombre_shouldPlaceNullVoteLast_whenMixedCandidates() {
        // Given — real candidates + null vote
        CandidatoEntity realA = buildEntity(UUID.randomUUID(), "Candidate A", false, false, 1);
        CandidatoEntity realB = buildEntity(UUID.randomUUID(), "Candidate B", false, false, 2);
        CandidatoEntity nullVote = buildEntity(UUID.randomUUID(), "Voto Nulo", false, true, null);

        when(jpaRepository.findByEleccionId(eleccionId)).thenReturn(List.of(nullVote, realA, realB));

        // When
        List<Candidate> result = adapter.findByEleccionIdOrderByNombre(eleccionId);

        // Then — real candidates first, null vote last
        assertThat(result).hasSize(3);
        assertThat(result.get(0).nombre()).isEqualTo("Candidate A");
        assertThat(result.get(1).nombre()).isEqualTo("Candidate B");
        assertThat(result.get(2).nombre()).isEqualTo("Voto Nulo");
    }

    @Test
    @DisplayName("Should sort real candidates alphabetically and place both synthetics last")
    void findByEleccionIdOrderByNombre_shouldSortRealAlphabeticallyAndSyntheticLast_whenAllPresent() {
        // Given — full ballot: real (A, B, C), blank, null
        CandidatoEntity realA = buildEntity(UUID.randomUUID(), "Candidate A", false, false, 1);
        CandidatoEntity realB = buildEntity(UUID.randomUUID(), "Candidate B", false, false, 2);
        CandidatoEntity realC = buildEntity(UUID.randomUUID(), "Candidate C", false, false, 3);
        CandidatoEntity blank = buildEntity(UUID.randomUUID(), "Voto en Blanco", true, false, null);
        CandidatoEntity nullVote = buildEntity(UUID.randomUUID(), "Voto Nulo", false, true, null);

        when(jpaRepository.findByEleccionId(eleccionId))
                .thenReturn(List.of(nullVote, blank, realC, realA, realB));

        // When
        List<Candidate> result = adapter.findByEleccionIdOrderByNombre(eleccionId);

        // Then
        assertThat(result).hasSize(5);
        assertThat(result.get(0).nombre()).isEqualTo("Candidate A");
        assertThat(result.get(1).nombre()).isEqualTo("Candidate B");
        assertThat(result.get(2).nombre()).isEqualTo("Candidate C");
        // Synthetics at end
        List<String> syntheticNames = result.subList(3, 5).stream()
                .map(Candidate::nombre)
                .toList();
        assertThat(syntheticNames).containsExactlyInAnyOrder("Voto en Blanco", "Voto Nulo");
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private CandidatoEntity buildEntity(UUID id, String nombre,
                                         boolean esVotoEnBlanco, boolean esVotoNulo,
                                         Integer funcionarioId) {
        CandidatoEntity entity = new CandidatoEntity();
        entity.setId(id);
        entity.setEleccionId(eleccionId);
        entity.setNombre(nombre);
        entity.setEsVotoEnBlanco(esVotoEnBlanco);
        entity.setEsVotoNulo(esVotoNulo);
        entity.setFuncionarioId(funcionarioId);
        return entity;
    }
}
