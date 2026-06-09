package co.com.votapp.ws.electoral.infrastructure.adapter.in.web;

import co.com.votapp.ws.common.exception.DomainException;
import co.com.votapp.ws.electoral.domain.Ballot;
import co.com.votapp.ws.electoral.domain.CandidateOption;
import co.com.votapp.ws.electoral.domain.port.in.ActivateElectionUseCase;
import co.com.votapp.ws.electoral.domain.port.in.AddCandidateUseCase;
import co.com.votapp.ws.electoral.domain.port.in.CreateElectionUseCase;
import co.com.votapp.ws.electoral.domain.port.in.FinalizeElectionUseCase;
import co.com.votapp.ws.electoral.domain.port.in.GetBallotUseCase;
import co.com.votapp.ws.electoral.domain.port.out.CandidateRepositoryPort;
import co.com.votapp.ws.electoral.domain.port.out.ElectionRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Task 3.3 — Tests proving getBallot validates the {id} path param against the token's election.
 *
 * <p>RED phase: these tests will fail until the controller is fixed to actually use the path param.
 */
@DisplayName("ElectionController.getBallot - path param validation (Task 3.3)")
@ExtendWith(MockitoExtension.class)
class ElectionControllerBallotValidationTest {

    @Mock private CreateElectionUseCase createElectionUseCase;
    @Mock private ActivateElectionUseCase activateElectionUseCase;
    @Mock private FinalizeElectionUseCase finalizeElectionUseCase;
    @Mock private GetBallotUseCase getBallotUseCase;
    @Mock private AddCandidateUseCase addCandidateUseCase;
    @Mock private ElectionRepositoryPort electionRepository;
    @Mock private CandidateRepositoryPort candidateRepository;

    private ElectionController controller;

    private static final UUID ELECTION_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID OTHER_ID    = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @BeforeEach
    void setUp() {
        controller = new ElectionController(
                createElectionUseCase, activateElectionUseCase, finalizeElectionUseCase,
                getBallotUseCase, addCandidateUseCase, electionRepository, candidateRepository
        );
    }

    @Test
    @DisplayName("Should return 200 when path id matches the ballot's eleccionId")
    void getBallot_shouldReturn200_whenPathIdMatchesBallotEleccion() {
        // Given
        String rawToken = "valid-token";
        Ballot ballot = new Ballot(ELECTION_ID, "Elección Test",
                List.of(new CandidateOption(UUID.randomUUID(), "Candidato A", false)));
        when(getBallotUseCase.getBallot(rawToken)).thenReturn(ballot);

        // When
        ResponseEntity<ElectionController.BallotResponse> response =
                controller.getBallot(ELECTION_ID.toString(), rawToken);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().eleccionId()).isEqualTo(ELECTION_ID.toString());
    }

    @Test
    @DisplayName("Should throw DomainException when path id does not match the ballot's eleccionId")
    void getBallot_shouldThrowDomainException_whenPathIdMismatchesBallotEleccion() {
        // Given — token belongs to ELECTION_ID but path says OTHER_ID
        String rawToken = "valid-token";
        Ballot ballot = new Ballot(ELECTION_ID, "Elección Test",
                List.of(new CandidateOption(UUID.randomUUID(), "Candidato A", false)));
        when(getBallotUseCase.getBallot(rawToken)).thenReturn(ballot);

        // When & Then — controller must detect mismatch and throw
        assertThatThrownBy(() -> controller.getBallot(OTHER_ID.toString(), rawToken))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("token no pertenece");
    }
}
