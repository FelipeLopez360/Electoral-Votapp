package co.com.votapp.ws.voting.infrastructure.adapter.out.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ParticipacionRepositoryAdapter}.
 *
 * <p>TDD: RED phase written before production code (task 1.1).
 *
 * <p>Verifies that {@code countByEleccionId(UUID)} correctly delegates
 * to the JPA repository's derived query without any transformation.
 */
@DisplayName("ParticipacionRepositoryAdapter - countByEleccionId delegation")
@ExtendWith(MockitoExtension.class)
class ParticipacionRepositoryAdapterTest {

    @Mock
    private ParticipacionJpaRepository jpaRepository;

    private ParticipacionRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ParticipacionRepositoryAdapter(jpaRepository);
    }

    @Test
    @DisplayName("Should delegate countByEleccionId to JPA repository and return its result")
    void countByEleccionId_shouldDelegateToJpaAndReturnCount() {
        // Given
        UUID eleccionId = UUID.randomUUID();
        when(jpaRepository.countByEleccionId(eleccionId)).thenReturn(42L);

        // When
        long result = adapter.countByEleccionId(eleccionId);

        // Then
        assertThat(result).isEqualTo(42L);
        verify(jpaRepository).countByEleccionId(eleccionId);
    }

    @Test
    @DisplayName("Should return 0 when there are no participation records for the election")
    void countByEleccionId_shouldReturnZero_whenNoParticipationExists() {
        // Given
        UUID eleccionId = UUID.randomUUID();
        when(jpaRepository.countByEleccionId(eleccionId)).thenReturn(0L);

        // When
        long result = adapter.countByEleccionId(eleccionId);

        // Then
        assertThat(result).isEqualTo(0L);
        verify(jpaRepository).countByEleccionId(eleccionId);
    }

    @Test
    @DisplayName("Should forward the exact eleccionId to JPA without transformation")
    void countByEleccionId_shouldForwardExactUUID_toJpaRepository() {
        // Given
        UUID eleccionId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        when(jpaRepository.countByEleccionId(eleccionId)).thenReturn(1L);

        // When
        adapter.countByEleccionId(eleccionId);

        // Then
        verify(jpaRepository).countByEleccionId(eleccionId);
    }
}
