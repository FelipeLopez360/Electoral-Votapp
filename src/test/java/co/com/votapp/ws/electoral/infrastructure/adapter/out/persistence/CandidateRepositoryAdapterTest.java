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
 * <p>Focuses on the sort order contract of {@code findByEleccionIdOrderByNumeroOrden}:
 * synthetic candidates (blank vote and null vote) MUST appear LAST in ballot order,
 * after all real candidates sorted ascending by {@code numeroOrden}.
 *
 * <p>Rationale: The null vote is stored with {@code numeroOrden=-1} and the blank vote
 * with {@code numeroOrden=0}. A naive ascending sort would place them BEFORE real candidates
 * (1, 2, 3...), which is wrong UX for ballot display in the portal.
 */
@DisplayName("CandidateRepositoryAdapter - Ballot order and synthetic candidate placement")
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
    @DisplayName("Should place blank vote candidate (esVotoEnBlanco=true) after all real candidates")
    void findByEleccionIdOrderByNumeroOrden_shouldPlaceBlankVoteLast_whenMixedCandidates() {
        // Given — real candidates (numeroOrden 1, 2) + blank vote (numeroOrden 0)
        CandidatoEntity realA = buildEntity(UUID.randomUUID(), "Candidate A", 1, false, false);
        CandidatoEntity realB = buildEntity(UUID.randomUUID(), "Candidate B", 2, false, false);
        CandidatoEntity blank = buildEntity(UUID.randomUUID(), "Voto en Blanco", 0, true, false);

        when(jpaRepository.findByEleccionId(eleccionId)).thenReturn(List.of(blank, realA, realB));

        // When
        List<Candidate> result = adapter.findByEleccionIdOrderByNumeroOrden(eleccionId);

        // Then — real candidates first (ascending), synthetics last
        assertThat(result).hasSize(3);
        assertThat(result.get(0).nombre()).isEqualTo("Candidate A");
        assertThat(result.get(1).nombre()).isEqualTo("Candidate B");
        assertThat(result.get(2).nombre()).isEqualTo("Voto en Blanco");
    }

    @Test
    @DisplayName("Should place null vote candidate (esVotoNulo=true) after all real candidates")
    void findByEleccionIdOrderByNumeroOrden_shouldPlaceNullVoteLast_whenMixedCandidates() {
        // Given — real candidates (1, 2) + null vote (numeroOrden -1)
        CandidatoEntity realA = buildEntity(UUID.randomUUID(), "Candidate A", 1, false, false);
        CandidatoEntity realB = buildEntity(UUID.randomUUID(), "Candidate B", 2, false, false);
        CandidatoEntity nullVote = buildEntity(UUID.randomUUID(), "Voto Nulo", -1, false, true);

        when(jpaRepository.findByEleccionId(eleccionId)).thenReturn(List.of(nullVote, realA, realB));

        // When
        List<Candidate> result = adapter.findByEleccionIdOrderByNumeroOrden(eleccionId);

        // Then — real candidates first (ascending), null vote last
        assertThat(result).hasSize(3);
        assertThat(result.get(0).nombre()).isEqualTo("Candidate A");
        assertThat(result.get(1).nombre()).isEqualTo("Candidate B");
        assertThat(result.get(2).nombre()).isEqualTo("Voto Nulo");
    }

    @Test
    @DisplayName("Should sort real candidates ascending and place both synthetics last")
    void findByEleccionIdOrderByNumeroOrden_shouldSortRealAscendingAndSyntheticLast_whenAllPresent() {
        // Given — full ballot: real (1, 2, 3), blank (0), null (-1)
        CandidatoEntity realA = buildEntity(UUID.randomUUID(), "Candidate A", 1, false, false);
        CandidatoEntity realB = buildEntity(UUID.randomUUID(), "Candidate B", 2, false, false);
        CandidatoEntity realC = buildEntity(UUID.randomUUID(), "Candidate C", 3, false, false);
        CandidatoEntity blank = buildEntity(UUID.randomUUID(), "Voto en Blanco", 0, true, false);
        CandidatoEntity nullVote = buildEntity(UUID.randomUUID(), "Voto Nulo", -1, false, true);

        when(jpaRepository.findByEleccionId(eleccionId))
                .thenReturn(List.of(nullVote, blank, realC, realA, realB));

        // When
        List<Candidate> result = adapter.findByEleccionIdOrderByNumeroOrden(eleccionId);

        // Then
        assertThat(result).hasSize(5);
        // Real candidates first, ascending
        assertThat(result.get(0).nombre()).isEqualTo("Candidate A");
        assertThat(result.get(1).nombre()).isEqualTo("Candidate B");
        assertThat(result.get(2).nombre()).isEqualTo("Candidate C");
        // Synthetics at end (relative order among synthetics is stable but not contractually mandated)
        List<String> syntheticNames = result.subList(3, 5).stream()
                .map(Candidate::nombre)
                .toList();
        assertThat(syntheticNames).containsExactlyInAnyOrder("Voto en Blanco", "Voto Nulo");
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private CandidatoEntity buildEntity(UUID id, String nombre, int numeroOrden,
                                        boolean esVotoEnBlanco, boolean esVotoNulo) {
        CandidatoEntity entity = new CandidatoEntity();
        entity.setId(id);
        entity.setEleccionId(eleccionId);
        entity.setNombre(nombre);
        entity.setNumeroOrden(numeroOrden);
        entity.setEsVotoEnBlanco(esVotoEnBlanco);
        entity.setEsVotoNulo(esVotoNulo);
        return entity;
    }
}
