package co.com.votapp.ws.electoral.infrastructure.adapter.in.web;

import co.com.votapp.ws.common.domain.model.PageResult;
import co.com.votapp.ws.electoral.application.service.ElectionTransitionAppService;
import co.com.votapp.ws.electoral.domain.Election;
import co.com.votapp.ws.electoral.domain.ElectionStatus;
import co.com.votapp.ws.electoral.domain.port.in.AddCandidateUseCase;
import co.com.votapp.ws.electoral.domain.port.in.CreateElectionUseCase;
import co.com.votapp.ws.electoral.domain.port.in.FinalizeElectionUseCase;
import co.com.votapp.ws.electoral.domain.port.out.CandidateRepositoryPort;
import co.com.votapp.ws.electoral.domain.port.out.ElectionRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TDD RED cycle for the paginated {@code listElections} endpoint in {@link ElectionController}.
 *
 * <p>Written BEFORE production code changes. Tests cover default pagination, explicit params,
 * search pass-through, clamping, and the shape of the returned PageResult.
 */
@DisplayName("ElectionController - Paginated listElections endpoint")
@ExtendWith(MockitoExtension.class)
class ElectionControllerPaginationTest {

    @Mock private CreateElectionUseCase createElectionUseCase;
    @Mock private ElectionTransitionAppService electionTransitionAppService;
    @Mock private FinalizeElectionUseCase finalizeElectionUseCase;
    @Mock private AddCandidateUseCase addCandidateUseCase;
    @Mock private ElectionRepositoryPort electionRepository;
    @Mock private CandidateRepositoryPort candidateRepository;

    private ElectionController controller;

    private static final UUID ELECTION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final LocalDateTime START = LocalDateTime.now().plusDays(1);
    private static final LocalDateTime END = START.plusHours(8);

    @BeforeEach
    void setUp() {
        controller = new ElectionController(
                createElectionUseCase,
                electionTransitionAppService,
                finalizeElectionUseCase,
                addCandidateUseCase,
                electionRepository,
                candidateRepository
        );
    }

    private Election sampleElection(String codigo) {
        return new Election(ELECTION_ID, codigo, "Elección " + codigo, ElectionStatus.PROGRAMADA, START, END);
    }

    private PageResult<Election> pageOf(Election... elections) {
        return new PageResult<>(List.of(elections), 0, 8, elections.length, 1);
    }

    // ─── listElections ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return 200 with PageResult defaulting page=0 size=8 when no params")
    void listElections_shouldReturn200WithPageResult_whenDefaultParams() {
        // Given
        PageResult<Election> pageResult = pageOf(sampleElection("ELEC-001"));
        when(electionRepository.findAll(eq(0), eq(8), eq(null))).thenReturn(pageResult);

        // When
        ResponseEntity<PageResult<ElectionController.ElectionResponse>> response =
                controller.listElections(null, null, null);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        PageResult<ElectionController.ElectionResponse> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.page()).isEqualTo(0);
        assertThat(body.size()).isEqualTo(8);
        assertThat(body.content()).hasSize(1);
        assertThat(body.content().get(0).codigo()).isEqualTo("ELEC-001");
    }

    @Test
    @DisplayName("Should pass explicit page and size to repository")
    void listElections_shouldPassExplicitPageAndSize_toRepository() {
        // Given
        PageResult<Election> pageResult = new PageResult<>(List.of(), 2, 5, 0L, 0);
        when(electionRepository.findAll(eq(2), eq(5), eq(null))).thenReturn(pageResult);

        // When
        controller.listElections(2, 5, null);

        // Then
        ArgumentCaptor<Integer> pageCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> sizeCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(electionRepository).findAll(pageCaptor.capture(), sizeCaptor.capture(), any());
        assertThat(pageCaptor.getValue()).isEqualTo(2);
        assertThat(sizeCaptor.getValue()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should clamp page to 0 when page is negative")
    void listElections_shouldClampPageToZero_whenPageIsNegative() {
        // Given
        when(electionRepository.findAll(eq(0), anyInt(), any()))
                .thenReturn(PageResult.empty(0, 8));

        // When
        controller.listElections(-3, null, null);

        // Then
        ArgumentCaptor<Integer> pageCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(electionRepository).findAll(pageCaptor.capture(), anyInt(), any());
        assertThat(pageCaptor.getValue()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should clamp size to 100 when size exceeds 100")
    void listElections_shouldClampSizeToMax_whenSizeExceeds100() {
        // Given
        when(electionRepository.findAll(anyInt(), eq(100), any()))
                .thenReturn(PageResult.empty(0, 100));

        // When
        controller.listElections(null, 500, null);

        // Then
        ArgumentCaptor<Integer> sizeCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(electionRepository).findAll(anyInt(), sizeCaptor.capture(), any());
        assertThat(sizeCaptor.getValue()).isEqualTo(100);
    }

    @Test
    @DisplayName("Should pass search term to repository and return filtered PageResult")
    void listElections_shouldPassSearchTerm_andReturnFilteredPageResult() {
        // Given
        Election e = sampleElection("PRES-2026");
        PageResult<Election> filtered = new PageResult<>(List.of(e), 0, 8, 1L, 1);
        when(electionRepository.findAll(eq(0), eq(8), eq("Presidencial"))).thenReturn(filtered);

        // When
        ResponseEntity<PageResult<ElectionController.ElectionResponse>> response =
                controller.listElections(null, null, "Presidencial");

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        PageResult<ElectionController.ElectionResponse> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.totalElements()).isEqualTo(1L);
        verify(electionRepository).findAll(eq(0), eq(8), eq("Presidencial"));
    }

    @Test
    @DisplayName("Should return empty PageResult when search has no matching elections")
    void listElections_shouldReturnEmptyPageResult_whenNoMatchesFound() {
        // Given
        when(electionRepository.findAll(anyInt(), anyInt(), eq("XYZ")))
                .thenReturn(PageResult.empty(0, 8));

        // When
        ResponseEntity<PageResult<ElectionController.ElectionResponse>> response =
                controller.listElections(null, null, "XYZ");

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().content()).isEmpty();
        assertThat(response.getBody().totalElements()).isEqualTo(0L);
    }
}
