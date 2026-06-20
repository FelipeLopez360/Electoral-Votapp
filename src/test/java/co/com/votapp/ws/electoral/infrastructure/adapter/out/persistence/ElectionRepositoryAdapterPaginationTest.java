package co.com.votapp.ws.electoral.infrastructure.adapter.out.persistence;

import co.com.votapp.ws.common.domain.model.PageResult;
import co.com.votapp.ws.electoral.domain.Election;
import co.com.votapp.ws.electoral.domain.ElectionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TDD RED cycle for the paginated {@code findAll(int page, int size, String search)}
 * method in {@link ElectionRepositoryAdapter}.
 *
 * <p>Written BEFORE the implementation. Mirrors the CensoRepositoryAdapterTest pattern.
 */
@DisplayName("ElectionRepositoryAdapter - Paginated findAll unit tests")
@ExtendWith(MockitoExtension.class)
class ElectionRepositoryAdapterPaginationTest {

    @Mock
    private EleccionJpaRepository jpaRepository;

    private ElectionRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ElectionRepositoryAdapter(jpaRepository);
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private EleccionEntity entity(UUID id, String codigo, String nombre) {
        EleccionEntity e = new EleccionEntity();
        e.setId(id);
        e.setCodigo(codigo);
        e.setNombre(nombre);
        e.setEstado("PROGRAMADA");
        Instant now = Instant.now();
        e.setFechaInicio(now.plusSeconds(3600));
        e.setFechaFin(now.plusSeconds(7200));
        e.setCreatedAt(now);
        e.setUpdatedAt(now);
        return e;
    }

    // ─── findAll(page, size, search) ──────────────────────────────────────────

    @Test
    @DisplayName("Should return PageResult mapped from Spring Page when elections exist")
    void findAll_shouldReturnPageResult_whenElectionsExist() {
        // Given
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        EleccionEntity e1 = entity(id1, "ELEC-001", "Elección General");
        EleccionEntity e2 = entity(id2, "ELEC-002", "Elección Presidencial");
        Page<EleccionEntity> page = new PageImpl<>(List.of(e1, e2), PageRequest.of(0, 8), 2L);
        when(jpaRepository.search(eq(""), any(Pageable.class))).thenReturn(page);

        // When
        PageResult<Election> result = adapter.findAll(0, 8, null);

        // Then
        assertThat(result.page()).isEqualTo(0);
        assertThat(result.size()).isEqualTo(8);
        assertThat(result.totalElements()).isEqualTo(2L);
        assertThat(result.totalPages()).isEqualTo(1);
        assertThat(result.content()).hasSize(2);
        assertThat(result.content().get(0).codigo()).isEqualTo("ELEC-001");
        assertThat(result.content().get(1).codigo()).isEqualTo("ELEC-002");
    }

    @Test
    @DisplayName("Should pass search term and createdAt DESC sort to JPA repository")
    void findAll_shouldPassSearchAndSort_toJpaRepository() {
        // Given
        Page<EleccionEntity> page = new PageImpl<>(List.of(), PageRequest.of(0, 8), 0L);
        when(jpaRepository.search(eq("Presidencial"), any(Pageable.class))).thenReturn(page);

        // When
        adapter.findAll(0, 8, "Presidencial");

        // Then
        ArgumentCaptor<String> searchCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(jpaRepository).search(searchCaptor.capture(), pageableCaptor.capture());

        assertThat(searchCaptor.getValue()).isEqualTo("Presidencial");
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(8);
        // Must sort by createdAt DESC
        Sort.Order order = pageableCaptor.getValue().getSort().getOrderFor("createdAt");
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    @DisplayName("Should return empty PageResult when no elections match search")
    void findAll_shouldReturnEmptyPageResult_whenNoMatchesFound() {
        // Given
        Page<EleccionEntity> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 8), 0L);
        when(jpaRepository.search(eq("NoExiste"), any(Pageable.class))).thenReturn(emptyPage);

        // When
        PageResult<Election> result = adapter.findAll(0, 8, "NoExiste");

        // Then
        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isEqualTo(0L);
        assertThat(result.totalPages()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should use empty string as search when null is passed")
    void findAll_shouldUseEmptyString_whenSearchIsNull() {
        // Given
        Page<EleccionEntity> page = new PageImpl<>(List.of(), PageRequest.of(0, 8), 0L);
        when(jpaRepository.search(eq(""), any(Pageable.class))).thenReturn(page);

        // When
        adapter.findAll(0, 8, null);

        // Then
        ArgumentCaptor<String> searchCaptor = ArgumentCaptor.forClass(String.class);
        verify(jpaRepository).search(searchCaptor.capture(), any(Pageable.class));
        assertThat(searchCaptor.getValue()).isEqualTo("");
    }

    @Test
    @DisplayName("Should map EleccionEntity fields to Election domain object correctly")
    void findAll_shouldMapEntityToDomain_correctly() {
        // Given
        UUID id = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
        EleccionEntity e = entity(id, "ELEC-MAP", "Elección de Prueba");
        Page<EleccionEntity> page = new PageImpl<>(List.of(e), PageRequest.of(0, 8), 1L);
        when(jpaRepository.search(any(), any(Pageable.class))).thenReturn(page);

        // When
        PageResult<Election> result = adapter.findAll(0, 8, "");

        // Then
        Election election = result.content().get(0);
        assertThat(election.id()).isEqualTo(id);
        assertThat(election.codigo()).isEqualTo("ELEC-MAP");
        assertThat(election.nombre()).isEqualTo("Elección de Prueba");
        assertThat(election.status()).isEqualTo(ElectionStatus.PROGRAMADA);
    }
}
