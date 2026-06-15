package co.com.votapp.ws.electoral.application.service;

import co.com.votapp.ws.electoral.domain.port.in.ActivateElectionUseCase;
import co.com.votapp.ws.electoral.domain.port.in.FinalizeElectionUseCase;
import co.com.votapp.ws.voting.domain.port.in.BulkIssueTokensUseCase;
import co.com.votapp.ws.voting.domain.port.in.BulkIssueTokensUseCase.BulkIssueResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("ElectionTransitionAppService - Transactional delegation wrapper")
@ExtendWith(MockitoExtension.class)
class ElectionTransitionAppServiceTest {

    @Mock private ActivateElectionUseCase activateElectionUseCase;
    @Mock private FinalizeElectionUseCase finalizeElectionUseCase;
    @Mock private BulkIssueTokensUseCase bulkIssueTokensUseCase;

    private ElectionTransitionAppService service;

    @BeforeEach
    void setUp() {
        service = new ElectionTransitionAppService(
                activateElectionUseCase, finalizeElectionUseCase, bulkIssueTokensUseCase);
    }

    // ── activate orchestration ────────────────────────────────────────────────

    @Test
    @DisplayName("Should call ActivateElectionUseCase then BulkIssueTokensUseCase in order")
    void activate_shouldCallActivateThenBulkIssue_inOrder() {
        // Given
        UUID electionId = UUID.randomUUID();
        when(bulkIssueTokensUseCase.issueForElection(electionId))
                .thenReturn(new BulkIssueResult(5, 0, 5));

        // When
        service.activate(electionId);

        // Then
        InOrder inOrder = inOrder(activateElectionUseCase, bulkIssueTokensUseCase);
        inOrder.verify(activateElectionUseCase).activate(electionId);
        inOrder.verify(bulkIssueTokensUseCase).issueForElection(electionId);
    }

    @Test
    @DisplayName("Should propagate exception from BulkIssueTokensUseCase (rollback)")
    void activate_shouldPropagateException_whenBulkIssueFails() {
        // Given
        UUID electionId = UUID.randomUUID();
        doThrow(new RuntimeException("Token issuance failed"))
                .when(bulkIssueTokensUseCase).issueForElection(electionId);

        // When & Then
        assertThatThrownBy(() -> service.activate(electionId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Token issuance failed");

        // Both use cases were called (activate succeeded, bulk failed)
        verify(activateElectionUseCase).activate(electionId);
        verify(bulkIssueTokensUseCase).issueForElection(electionId);
    }

    @Test
    @DisplayName("Should NOT call BulkIssueTokensUseCase when ActivateElectionUseCase fails")
    void activate_shouldNotCallBulkIssue_whenActivateFails() {
        // Given
        UUID electionId = UUID.randomUUID();
        doThrow(new RuntimeException("Activation failed"))
                .when(activateElectionUseCase).activate(electionId);

        // When & Then
        assertThatThrownBy(() -> service.activate(electionId))
                .isInstanceOf(RuntimeException.class);

        verifyNoInteractions(bulkIssueTokensUseCase);
    }

    // ── finalize (unchanged) ─────────────────────────────────────────────────

    @Test
    @DisplayName("Should delegate finalize() to FinalizeElectionUseCase with correct id")
    void finalize_shouldDelegateToFinalizeUseCase_withCorrectId() {
        // Given
        UUID electionId = UUID.randomUUID();

        // When
        service.finalize(electionId);

        // Then
        verify(finalizeElectionUseCase).finalize(electionId);
        verifyNoInteractions(activateElectionUseCase);
        verifyNoInteractions(bulkIssueTokensUseCase);
    }

    // ── @Transactional contract ──────────────────────────────────────────────

    @Test
    @DisplayName("Should have @Transactional on activate() method")
    void activate_shouldHaveTransactionalAnnotation() throws NoSuchMethodException {
        // Given
        Method method = ElectionTransitionAppService.class.getMethod("activate", UUID.class);

        // When / Then
        assertThat(method.isAnnotationPresent(Transactional.class))
                .as("activate() must be annotated with @Transactional")
                .isTrue();
    }

    @Test
    @DisplayName("Should have @Transactional on finalize() method")
    void finalize_shouldHaveTransactionalAnnotation() throws NoSuchMethodException {
        // Given
        Method method = ElectionTransitionAppService.class.getMethod("finalize", UUID.class);

        // When / Then
        assertThat(method.isAnnotationPresent(Transactional.class))
                .as("finalize() must be annotated with @Transactional")
                .isTrue();
    }

    @Test
    @DisplayName("Should not call finalize when activate is invoked")
    void activate_shouldNotInvokeFinalize_whenCalled() {
        // Given
        UUID electionId = UUID.randomUUID();
        when(bulkIssueTokensUseCase.issueForElection(electionId))
                .thenReturn(new BulkIssueResult(0, 0, 0));

        // When
        service.activate(electionId);

        // Then
        verifyNoInteractions(finalizeElectionUseCase);
    }

    @Test
    @DisplayName("Should not call activate when finalize is invoked")
    void finalize_shouldNotInvokeActivate_whenCalled() {
        // Given
        UUID electionId = UUID.randomUUID();

        // When
        service.finalize(electionId);

        // Then
        verifyNoInteractions(activateElectionUseCase);
    }
}
