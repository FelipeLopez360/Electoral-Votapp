package co.com.votapp.ws.electoral.infrastructure.adapter.out.persistence;

import co.com.votapp.ws.electoral.domain.model.CensoEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TDD RED cycle for {@link CensoRepositoryAdapter}.
 * Written BEFORE the adapter implementation.
 *
 * <p>Mocks {@link CensoJpaRepository}. Tests verify the adapter correctly delegates
 * to the JPA repo and maps between domain records and JPA entities.
 */
@DisplayName("CensoRepositoryAdapter - Output adapter unit tests")
@ExtendWith(MockitoExtension.class)
class CensoRepositoryAdapterTest {

    @Mock
    private CensoJpaRepository jpaRepository;

    private CensoRepositoryAdapter adapter;

    private static final UUID ELECCION_ID     = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID ENTRY_ID        = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final Integer FUNCIONARIO_ID = 42;
    private static final Instant NOW            = Instant.parse("2026-06-13T12:00:00Z");

    @BeforeEach
    void setUp() {
        adapter = new CensoRepositoryAdapter(jpaRepository);
    }

    // ─── save ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should save entry and return domain record with generated id")
    void save_shouldReturnSavedEntry_whenEntryIsValid() {
        // Given
        var entry = new CensoEntry(null, ELECCION_ID, FUNCIONARIO_ID, 99, NOW);
        var savedEntity = savedEntity(ENTRY_ID, ELECCION_ID, FUNCIONARIO_ID, 99, NOW);
        when(jpaRepository.save(any(CensoEntity.class))).thenReturn(savedEntity);

        // When
        var result = adapter.save(entry);

        // Then
        assertThat(result.id()).isEqualTo(ENTRY_ID);
        assertThat(result.eleccionId()).isEqualTo(ELECCION_ID);
        assertThat(result.funcionarioId()).isEqualTo(FUNCIONARIO_ID);
        assertThat(result.agregadoPor()).isEqualTo(99);
        verify(jpaRepository).save(any(CensoEntity.class));
    }

    // ─── saveAll ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should save all entries and return domain records")
    void saveAll_shouldReturnAllSavedEntries_whenListIsGiven() {
        // Given
        var entry1 = new CensoEntry(null, ELECCION_ID, 1, 99, NOW);
        var entry2 = new CensoEntry(null, ELECCION_ID, 2, 99, NOW);
        var savedEntity1 = savedEntity(UUID.randomUUID(), ELECCION_ID, 1, 99, NOW);
        var savedEntity2 = savedEntity(UUID.randomUUID(), ELECCION_ID, 2, 99, NOW);
        when(jpaRepository.saveAll(any())).thenReturn(List.of(savedEntity1, savedEntity2));

        // When
        var result = adapter.saveAll(List.of(entry1, entry2));

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).eleccionId()).isEqualTo(ELECCION_ID);
        verify(jpaRepository).saveAll(any());
    }

    // ─── delete ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should delegate deleteByEleccionIdAndFuncionarioId to JPA repo")
    void deleteByEleccionIdAndFuncionarioId_shouldDelegate_toJpaRepo() {
        // Given / When
        adapter.deleteByEleccionIdAndFuncionarioId(ELECCION_ID, FUNCIONARIO_ID);

        // Then
        verify(jpaRepository).deleteByEleccionIdAndFuncionarioId(ELECCION_ID, FUNCIONARIO_ID);
    }

    @Test
    @DisplayName("Should delegate deleteAllByEleccionId to JPA repo")
    void deleteAllByEleccionId_shouldDelegate_toJpaRepo() {
        // Given / When
        adapter.deleteAllByEleccionId(ELECCION_ID);

        // Then
        verify(jpaRepository).deleteAllByEleccionId(ELECCION_ID);
    }

    // ─── findByEleccionId ────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return paginated domain entries for an election")
    void findByEleccionId_shouldReturnPage_whenEntriesExist() {
        // Given
        var entity = savedEntity(ENTRY_ID, ELECCION_ID, FUNCIONARIO_ID, 99, NOW);
        Page<CensoEntity> entityPage = new PageImpl<>(List.of(entity), PageRequest.of(0, 10), 1);
        when(jpaRepository.findByEleccionId(eq(ELECCION_ID), any())).thenReturn(entityPage);

        // When
        var result = adapter.findByEleccionId(ELECCION_ID, 0, 10);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).funcionarioId()).isEqualTo(FUNCIONARIO_ID);
    }

    // ─── existsByEleccionIdAndFuncionarioId ──────────────────────────────────

    @Test
    @DisplayName("Should return true when funcionario is in census")
    void existsByEleccionIdAndFuncionarioId_shouldReturnTrue_whenInCensus() {
        // Given
        when(jpaRepository.existsByEleccionIdAndFuncionarioId(ELECCION_ID, FUNCIONARIO_ID)).thenReturn(true);

        // When / Then
        assertThat(adapter.existsByEleccionIdAndFuncionarioId(ELECCION_ID, FUNCIONARIO_ID)).isTrue();
    }

    @Test
    @DisplayName("Should return false when funcionario is not in census")
    void existsByEleccionIdAndFuncionarioId_shouldReturnFalse_whenNotInCensus() {
        // Given
        when(jpaRepository.existsByEleccionIdAndFuncionarioId(ELECCION_ID, FUNCIONARIO_ID)).thenReturn(false);

        // When / Then
        assertThat(adapter.existsByEleccionIdAndFuncionarioId(ELECCION_ID, FUNCIONARIO_ID)).isFalse();
    }

    // ─── countByEleccionId ───────────────────────────────────────────────────

    @Test
    @DisplayName("Should return count of census entries for an election")
    void countByEleccionId_shouldReturnCount_whenEntriesExist() {
        // Given
        when(jpaRepository.countByEleccionId(ELECCION_ID)).thenReturn(5L);

        // When / Then
        assertThat(adapter.countByEleccionId(ELECCION_ID)).isEqualTo(5L);
    }

    // ─── hasCensus ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return true when election has at least one census entry")
    void hasCensus_shouldReturnTrue_whenCensusNotEmpty() {
        // Given
        when(jpaRepository.countByEleccionId(ELECCION_ID)).thenReturn(3L);

        // When / Then
        assertThat(adapter.hasCensus(ELECCION_ID)).isTrue();
    }

    @Test
    @DisplayName("Should return false when election census is empty")
    void hasCensus_shouldReturnFalse_whenCensusIsEmpty() {
        // Given
        when(jpaRepository.countByEleccionId(ELECCION_ID)).thenReturn(0L);

        // When / Then
        assertThat(adapter.hasCensus(ELECCION_ID)).isFalse();
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private CensoEntity savedEntity(UUID id, UUID eleccionId, Integer funcionarioId,
                                    Integer agregadoPor, Instant createdAt) {
        var entity = new CensoEntity();
        entity.setId(id);
        entity.setEleccionId(eleccionId);
        entity.setFuncionarioId(funcionarioId);
        entity.setAgregadoPor(agregadoPor);
        entity.setCreatedAt(createdAt);
        return entity;
    }
}
