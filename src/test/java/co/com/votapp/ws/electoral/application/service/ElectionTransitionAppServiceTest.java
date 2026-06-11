package co.com.votapp.ws.electoral.application.service;

import co.com.votapp.ws.electoral.domain.port.in.ActivateElectionUseCase;
import co.com.votapp.ws.electoral.domain.port.in.FinalizeElectionUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@DisplayName("ElectionTransitionAppService - Transactional delegation wrapper")
@ExtendWith(MockitoExtension.class)
class ElectionTransitionAppServiceTest {

    @Mock
    private ActivateElectionUseCase activateElectionUseCase;

    @Mock
    private FinalizeElectionUseCase finalizeElectionUseCase;

    private ElectionTransitionAppService service;

    @BeforeEach
    void setUp() {
        service = new ElectionTransitionAppService(activateElectionUseCase, finalizeElectionUseCase);
    }

    @Test
    @DisplayName("Should delegate activate() to ActivateElectionUseCase with correct id")
    void activate_shouldDelegateToActivateUseCase_withCorrectId() {
        // Given
        UUID electionId = UUID.randomUUID();

        // When
        service.activate(electionId);

        // Then
        verify(activateElectionUseCase).activate(electionId);
        verifyNoInteractions(finalizeElectionUseCase);
    }

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
    }

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
