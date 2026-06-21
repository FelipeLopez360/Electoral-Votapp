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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ElectionRepositoryAdapter - Query methods and createdAt fix")
@ExtendWith(MockitoExtension.class)
class ElectionRepositoryAdapterTest {

    @Mock
    private EleccionJpaRepository jpaRepository;

    private ElectionRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ElectionRepositoryAdapter(jpaRepository);
    }

    // ─── findByStatusAndFechaInicioLessThanEqual ───────────────────────────────────

    @Test
    @DisplayName("Should return PROGRAMADA elections whose fechaInicio is before now")
    void findByStatusAndFechaInicioLessThanEqual_shouldReturnDueElections_whenFechaInicioHasPassed() {
        // Given
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.of(2026, 6, 10, 12, 0, 0);
        Instant nowInstant = now.toInstant(ZoneOffset.UTC);

        EleccionEntity entity = entityWith(id, "PROGRAMADA",
                now.minusHours(1).toInstant(ZoneOffset.UTC),
                now.plusDays(1).toInstant(ZoneOffset.UTC));
        when(jpaRepository.findByEstadoAndFechaInicioLessThanEqual(eq("PROGRAMADA"), any(Instant.class)))
                .thenReturn(List.of(entity));

        // When
        List<Election> result = adapter.findByStatusAndFechaInicioLessThanEqual(ElectionStatus.PROGRAMADA, now);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(id);
        assertThat(result.get(0).status()).isEqualTo(ElectionStatus.PROGRAMADA);

        ArgumentCaptor<Instant> instantCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(jpaRepository).findByEstadoAndFechaInicioLessThanEqual(eq("PROGRAMADA"), instantCaptor.capture());
        assertThat(instantCaptor.getValue()).isEqualTo(nowInstant);
    }

    @Test
    @DisplayName("Should include PROGRAMADA elections whose fechaInicio is exactly now (LessThanEqual boundary)")
    void findByStatusAndFechaInicioLessThanEqual_shouldIncludeElections_whenFechaInicioEqualsNow() {
        // Given
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.of(2026, 6, 10, 12, 0, 0);
        Instant nowInstant = now.toInstant(ZoneOffset.UTC);

        EleccionEntity entity = entityWith(id, "PROGRAMADA", nowInstant, now.plusDays(1).toInstant(ZoneOffset.UTC));
        when(jpaRepository.findByEstadoAndFechaInicioLessThanEqual(eq("PROGRAMADA"), any(Instant.class)))
                .thenReturn(List.of(entity));

        // When
        List<Election> result = adapter.findByStatusAndFechaInicioLessThanEqual(ElectionStatus.PROGRAMADA, now);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(id);
    }

    @Test
    @DisplayName("Should return empty list when no PROGRAMADA elections have passed fechaInicio")
    void findByStatusAndFechaInicioLessThanEqual_shouldReturnEmpty_whenNoElectionsDue() {
        // Given
        LocalDateTime now = LocalDateTime.of(2026, 6, 10, 12, 0, 0);
        when(jpaRepository.findByEstadoAndFechaInicioLessThanEqual(eq("PROGRAMADA"), any(Instant.class)))
                .thenReturn(List.of());

        // When
        List<Election> result = adapter.findByStatusAndFechaInicioLessThanEqual(ElectionStatus.PROGRAMADA, now);

        // Then
        assertThat(result).isEmpty();
    }

    // ─── findByStatusAndFechaFinLessThanEqual ──────────────────────────────────────

    @Test
    @DisplayName("Should return ACTIVA elections whose fechaFin is before now")
    void findByStatusAndFechaFinLessThanEqual_shouldReturnExpiredElections_whenFechaFinHasPassed() {
        // Given
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.of(2026, 6, 10, 12, 0, 0);
        Instant nowInstant = now.toInstant(ZoneOffset.UTC);

        EleccionEntity entity = entityWith(id, "ACTIVA",
                now.minusDays(5).toInstant(ZoneOffset.UTC),
                now.minusHours(1).toInstant(ZoneOffset.UTC));
        when(jpaRepository.findByEstadoAndFechaFinLessThanEqual(eq("ACTIVA"), any(Instant.class)))
                .thenReturn(List.of(entity));

        // When
        List<Election> result = adapter.findByStatusAndFechaFinLessThanEqual(ElectionStatus.ACTIVA, now);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(id);
        assertThat(result.get(0).status()).isEqualTo(ElectionStatus.ACTIVA);

        ArgumentCaptor<Instant> instantCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(jpaRepository).findByEstadoAndFechaFinLessThanEqual(eq("ACTIVA"), instantCaptor.capture());
        assertThat(instantCaptor.getValue()).isEqualTo(nowInstant);
    }

    @Test
    @DisplayName("Should include ACTIVA elections whose fechaFin is exactly now (LessThanEqual boundary)")
    void findByStatusAndFechaFinLessThanEqual_shouldIncludeElections_whenFechaFinEqualsNow() {
        // Given
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.of(2026, 6, 10, 12, 0, 0);
        Instant nowInstant = now.toInstant(ZoneOffset.UTC);

        EleccionEntity entity = entityWith(id, "ACTIVA", now.minusDays(5).toInstant(ZoneOffset.UTC), nowInstant);
        when(jpaRepository.findByEstadoAndFechaFinLessThanEqual(eq("ACTIVA"), any(Instant.class)))
                .thenReturn(List.of(entity));

        // When
        List<Election> result = adapter.findByStatusAndFechaFinLessThanEqual(ElectionStatus.ACTIVA, now);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(id);
    }

    @Test
    @DisplayName("Should return empty list when no ACTIVA elections have passed fechaFin")
    void findByStatusAndFechaFinLessThanEqual_shouldReturnEmpty_whenNoElectionsExpired() {
        // Given
        LocalDateTime now = LocalDateTime.of(2026, 6, 10, 12, 0, 0);
        when(jpaRepository.findByEstadoAndFechaFinLessThanEqual(eq("ACTIVA"), any(Instant.class)))
                .thenReturn(List.of());

        // When
        List<Election> result = adapter.findByStatusAndFechaFinLessThanEqual(ElectionStatus.ACTIVA, now);

        // Then
        assertThat(result).isEmpty();
    }

    // ─── createdAt preservation on update ───────────────────────────────────

    @Test
    @DisplayName("Should preserve createdAt from existing entity on update (non-null id)")
    void save_shouldPreserveCreatedAt_whenElectionHasExistingId() {
        // Given
        UUID id = UUID.randomUUID();
        Instant originalCreatedAt = Instant.parse("2026-01-01T00:00:00Z");
        LocalDateTime fechaInicio = LocalDateTime.of(2026, 6, 1, 0, 0);
        LocalDateTime fechaFin = LocalDateTime.of(2026, 6, 30, 0, 0);

        Election election = new Election(id, "TEST-001", "Test Election",
                ElectionStatus.ACTIVA, fechaInicio, fechaFin, true, 1);

        EleccionEntity existingEntity = entityWith(id, "PROGRAMADA",
                fechaInicio.toInstant(ZoneOffset.UTC),
                fechaFin.toInstant(ZoneOffset.UTC));
        existingEntity.setCreatedAt(originalCreatedAt);

        when(jpaRepository.findById(id)).thenReturn(Optional.of(existingEntity));
        when(jpaRepository.save(any(EleccionEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        adapter.save(election);

        // Then
        ArgumentCaptor<EleccionEntity> captor = ArgumentCaptor.forClass(EleccionEntity.class);
        verify(jpaRepository).save(captor.capture());
        assertThat(captor.getValue().getCreatedAt()).isEqualTo(originalCreatedAt);
    }

    @Test
    @DisplayName("Should set createdAt to now on INSERT (null id)")
    void save_shouldSetCreatedAtToNow_whenElectionIsNew() {
        // Given
        Instant beforeSave = Instant.now();
        LocalDateTime fechaInicio = LocalDateTime.of(2026, 6, 1, 0, 0);
        LocalDateTime fechaFin = LocalDateTime.of(2026, 6, 30, 0, 0);

        Election election = new Election(null, "TEST-002", "New Election",
                ElectionStatus.PROGRAMADA, fechaInicio, fechaFin, true, 1);

        when(jpaRepository.save(any(EleccionEntity.class))).thenAnswer(inv -> {
            EleccionEntity e = inv.getArgument(0);
            e.setId(UUID.randomUUID()); // simulate DB id generation
            return e;
        });

        // When
        adapter.save(election);

        // Then
        ArgumentCaptor<EleccionEntity> captor = ArgumentCaptor.forClass(EleccionEntity.class);
        verify(jpaRepository).save(captor.capture());
        Instant createdAt = captor.getValue().getCreatedAt();
        assertThat(createdAt).isNotNull();
        assertThat(createdAt).isAfterOrEqualTo(beforeSave);
    }

    @Test
    @DisplayName("Should always update updatedAt on save regardless of insert or update")
    void save_shouldAlwaysUpdateUpdatedAt_onAnyPersist() {
        // Given — update case (non-null id)
        UUID id = UUID.randomUUID();
        Instant beforeSave = Instant.now();
        LocalDateTime fechaInicio = LocalDateTime.of(2026, 6, 1, 0, 0);
        LocalDateTime fechaFin = LocalDateTime.of(2026, 6, 30, 0, 0);

        Election election = new Election(id, "TEST-003", "Update Election",
                ElectionStatus.FINALIZADA, fechaInicio, fechaFin, true, 1);

        EleccionEntity existingEntity = entityWith(id, "ACTIVA",
                fechaInicio.toInstant(ZoneOffset.UTC),
                fechaFin.toInstant(ZoneOffset.UTC));
        existingEntity.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));

        when(jpaRepository.findById(id)).thenReturn(Optional.of(existingEntity));
        when(jpaRepository.save(any(EleccionEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        adapter.save(election);

        // Then
        ArgumentCaptor<EleccionEntity> captor = ArgumentCaptor.forClass(EleccionEntity.class);
        verify(jpaRepository).save(captor.capture());
        assertThat(captor.getValue().getUpdatedAt()).isAfterOrEqualTo(beforeSave);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private EleccionEntity entityWith(UUID id, String estado, Instant fechaInicio, Instant fechaFin) {
        EleccionEntity entity = new EleccionEntity();
        entity.setId(id);
        entity.setCodigo("CODE-" + id.toString().substring(0, 8));
        entity.setNombre("Election " + id);
        entity.setEstado(estado);
        entity.setFechaInicio(fechaInicio);
        entity.setFechaFin(fechaFin);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        return entity;
    }
}
