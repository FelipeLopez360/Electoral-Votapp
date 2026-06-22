package co.com.votapp.ws.electoral.infrastructure.adapter.out.persistence;

import co.com.votapp.ws.electoral.domain.Election;
import co.com.votapp.ws.electoral.domain.ElectionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ElectionRepositoryAdapter#findByStatus(ElectionStatus)}.
 *
 * <p>TDD: RED phase written before production code (task 1.3).
 *
 * <p>Verifies:
 * <ul>
 *   <li>Delegates to JPA {@code findByEstado} with the enum's name as String.</li>
 *   <li>Maps each {@link EleccionEntity} to a {@link Election} domain record correctly.</li>
 *   <li>Returns an empty list when no elections with the given status exist.</li>
 *   <li>Maps multiple entities correctly.</li>
 * </ul>
 */
@DisplayName("ElectionRepositoryAdapter - findByStatus delegation and entity mapping")
@ExtendWith(MockitoExtension.class)
class ElectionRepositoryAdapterFindByStatusTest {

    @Mock
    private EleccionJpaRepository jpaRepository;

    private ElectionRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ElectionRepositoryAdapter(jpaRepository);
    }

    @Test
    @DisplayName("Should pass status name as String to JPA repository")
    void findByStatus_shouldPassStatusNameToJpa_asString() {
        // Given
        when(jpaRepository.findByEstado("ACTIVA")).thenReturn(List.of());

        // When
        adapter.findByStatus(ElectionStatus.ACTIVA);

        // Then
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(jpaRepository).findByEstado(captor.capture());
        assertThat(captor.getValue()).isEqualTo("ACTIVA");
    }

    @Test
    @DisplayName("Should map entity fields to domain Election record correctly")
    void findByStatus_shouldMapEntityToDomain_withAllFields() {
        // Given
        UUID id = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        Instant fechaInicio = Instant.parse("2026-06-01T00:00:00Z");
        Instant fechaFin = Instant.parse("2026-06-30T00:00:00Z");

        EleccionEntity entity = buildEntity(id, "ELEC-001", "Test Election", "ACTIVA",
                fechaInicio, fechaFin, true, 1);
        when(jpaRepository.findByEstado("ACTIVA")).thenReturn(List.of(entity));

        // When
        List<Election> result = adapter.findByStatus(ElectionStatus.ACTIVA);

        // Then
        assertThat(result).hasSize(1);
        Election election = result.get(0);
        assertThat(election.id()).isEqualTo(id);
        assertThat(election.codigo()).isEqualTo("ELEC-001");
        assertThat(election.nombre()).isEqualTo("Test Election");
        assertThat(election.status()).isEqualTo(ElectionStatus.ACTIVA);
        assertThat(election.fechaInicio()).isEqualTo(LocalDateTime.ofInstant(fechaInicio, ZoneOffset.UTC));
        assertThat(election.fechaFin()).isEqualTo(LocalDateTime.ofInstant(fechaFin, ZoneOffset.UTC));
        assertThat(election.permiteVotoBlanco()).isTrue();
        assertThat(election.maxVotosPorElector()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should return empty list when no elections have the given status")
    void findByStatus_shouldReturnEmptyList_whenNoElectionsMatch() {
        // Given
        when(jpaRepository.findByEstado("ACTIVA")).thenReturn(List.of());

        // When
        List<Election> result = adapter.findByStatus(ElectionStatus.ACTIVA);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should map multiple entities to domain elections correctly")
    void findByStatus_shouldMapMultipleEntities_toDomainList() {
        // Given
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        Instant inicio = Instant.parse("2026-06-01T00:00:00Z");
        Instant fin = Instant.parse("2026-06-30T00:00:00Z");

        EleccionEntity entity1 = buildEntity(id1, "ELEC-A", "Election A", "ACTIVA", inicio, fin, true, 1);
        EleccionEntity entity2 = buildEntity(id2, "ELEC-B", "Election B", "ACTIVA", inicio, fin, false, 2);
        when(jpaRepository.findByEstado("ACTIVA")).thenReturn(List.of(entity1, entity2));

        // When
        List<Election> result = adapter.findByStatus(ElectionStatus.ACTIVA);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Election::id).containsExactlyInAnyOrder(id1, id2);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private EleccionEntity buildEntity(UUID id, String codigo, String nombre, String estado,
                                       Instant fechaInicio, Instant fechaFin,
                                       boolean permiteVotoBlanco, int maxVotosPorElector) {
        EleccionEntity entity = new EleccionEntity();
        entity.setId(id);
        entity.setCodigo(codigo);
        entity.setNombre(nombre);
        entity.setEstado(estado);
        entity.setFechaInicio(fechaInicio);
        entity.setFechaFin(fechaFin);
        entity.setPermiteVotoBlanco(permiteVotoBlanco);
        entity.setMaxVotosPorElector(maxVotosPorElector);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        return entity;
    }
}
