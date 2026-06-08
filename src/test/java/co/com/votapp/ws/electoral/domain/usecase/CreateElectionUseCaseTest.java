package co.com.votapp.ws.electoral.domain.usecase;

import co.com.votapp.ws.common.exception.DomainException;
import co.com.votapp.ws.electoral.application.command.CreateElectionCommand;
import co.com.votapp.ws.electoral.domain.Election;
import co.com.votapp.ws.electoral.domain.ElectionStatus;
import co.com.votapp.ws.electoral.domain.port.in.CreateElectionUseCase;
import co.com.votapp.ws.electoral.domain.port.out.ElectionRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("CreateElectionUseCase - Election creation business logic")
@ExtendWith(MockitoExtension.class)
class CreateElectionUseCaseTest {

    @Mock
    private ElectionRepositoryPort electionRepository;

    private CreateElectionUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateElectionUseCaseImpl(electionRepository);
    }

    @Test
    @DisplayName("Should create election with PROGRAMADA status when codigo is unique")
    void create_shouldReturnElectionWithProgramadaStatus_whenCodigoIsUnique() {
        // Given
        var command = validCommand("ELEC-2026-001");
        when(electionRepository.findByCodigo("ELEC-2026-001")).thenReturn(Optional.empty());
        when(electionRepository.save(any(Election.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        Election result = useCase.create(command);

        // Then
        assertThat(result.status()).isEqualTo(ElectionStatus.PROGRAMADA);
        assertThat(result.codigo()).isEqualTo("ELEC-2026-001");
        assertThat(result.nombre()).isEqualTo("Eleccion Test");
        assertThat(result.id()).isNotNull();
    }

    @Test
    @DisplayName("Should persist the election via repository when codigo is unique")
    void create_shouldPersistElection_whenCodigoIsUnique() {
        // Given
        var command = validCommand("ELEC-2026-002");
        when(electionRepository.findByCodigo("ELEC-2026-002")).thenReturn(Optional.empty());
        when(electionRepository.save(any(Election.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        useCase.create(command);

        // Then
        ArgumentCaptor<Election> captor = ArgumentCaptor.forClass(Election.class);
        verify(electionRepository).save(captor.capture());
        assertThat(captor.getValue().codigo()).isEqualTo("ELEC-2026-002");
        assertThat(captor.getValue().status()).isEqualTo(ElectionStatus.PROGRAMADA);
    }

    @Test
    @DisplayName("Should throw DomainException when codigo already exists")
    void create_shouldThrowDomainException_whenCodigoAlreadyExists() {
        // Given
        var command = validCommand("ELEC-DUPLICATE");
        var existingElection = electionWith("ELEC-DUPLICATE", ElectionStatus.PROGRAMADA);
        when(electionRepository.findByCodigo("ELEC-DUPLICATE")).thenReturn(Optional.of(existingElection));

        // When & Then
        assertThatThrownBy(() -> useCase.create(command))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("ELEC-DUPLICATE");

        verify(electionRepository, never()).save(any());
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private CreateElectionCommand validCommand(String codigo) {
        return new CreateElectionCommand(
                codigo,
                "Eleccion Test",
                "Descripcion de prueba",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(30)
        );
    }

    private Election electionWith(String codigo, ElectionStatus status) {
        return new Election(
                UUID.randomUUID(),
                codigo,
                "Eleccion Existente",
                status,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(30)
        );
    }
}
