package co.com.votapp.ws.voting.domain.usecase;

import co.com.votapp.ws.auth.domain.Funcionario;
import co.com.votapp.ws.auth.domain.port.out.FuncionarioRepositoryPort;
import co.com.votapp.ws.electoral.domain.port.out.CensoRepositoryPort;
import co.com.votapp.ws.voting.domain.TokenStatus;
import co.com.votapp.ws.voting.domain.VotingToken;
import co.com.votapp.ws.voting.domain.port.in.BulkIssueTokensUseCase;
import co.com.votapp.ws.voting.domain.port.in.BulkIssueTokensUseCase.BulkIssueResult;
import co.com.votapp.ws.voting.domain.port.out.VotingTokenRepository;
import co.com.votapp.ws.votereligibility.domain.port.out.VoterEligibilityRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link BulkIssueTokensUseCaseImpl}.
 *
 * <p>TDD: tests written BEFORE implementation (RED phase).
 * All ports are mocked — pure domain logic tested here.
 */
@DisplayName("BulkIssueTokensUseCaseImpl - Bulk token issuance domain logic")
@ExtendWith(MockitoExtension.class)
class BulkIssueTokensUseCaseImplTest {

    @Mock private CensoRepositoryPort censoRepository;
    @Mock private VotingTokenRepository votingTokenRepository;
    @Mock private VoterEligibilityRepositoryPort eligibilityRepository;
    @Mock private FuncionarioRepositoryPort funcionarioRepository;

    private BulkIssueTokensUseCaseImpl useCase;

    private static final UUID ELECCION_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @BeforeEach
    void setUp() {
        useCase = new BulkIssueTokensUseCaseImpl(
                censoRepository, votingTokenRepository, eligibilityRepository, funcionarioRepository);
    }

    // ── Census populated path ─────────────────────────────────────────────────

    @Test
    @DisplayName("Should issue tokens for all eligible census members when census is populated")
    void issueForElection_shouldIssueTokens_whenCensusIsPopulated() {
        // Given
        when(censoRepository.findAllFuncionarioIdsByEleccionId(ELECCION_ID))
                .thenReturn(List.of(1, 2, 3));
        when(eligibilityRepository.isEligibleForElection(1L, ELECCION_ID)).thenReturn(true);
        when(eligibilityRepository.isEligibleForElection(2L, ELECCION_ID)).thenReturn(true);
        when(eligibilityRepository.isEligibleForElection(3L, ELECCION_ID)).thenReturn(true);
        when(votingTokenRepository.existsIssuedTokenFor(ELECCION_ID, 1L)).thenReturn(false);
        when(votingTokenRepository.existsIssuedTokenFor(ELECCION_ID, 2L)).thenReturn(false);
        when(votingTokenRepository.existsIssuedTokenFor(ELECCION_ID, 3L)).thenReturn(false);
        when(votingTokenRepository.saveAllIssued(anyList())).thenAnswer(inv -> inv.getArgument(0));

        // When
        BulkIssueResult result = useCase.issueForElection(ELECCION_ID);

        // Then
        assertThat(result.issued()).isEqualTo(3);
        assertThat(result.skipped()).isEqualTo(0);
        assertThat(result.total()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should skip ineligible census members and count them as skipped")
    void issueForElection_shouldSkipIneligible_whenFuncionarioIneligible() {
        // Given — funcionario 2 is INACTIVO
        when(censoRepository.findAllFuncionarioIdsByEleccionId(ELECCION_ID))
                .thenReturn(List.of(1, 2, 3));
        when(eligibilityRepository.isEligibleForElection(1L, ELECCION_ID)).thenReturn(true);
        when(eligibilityRepository.isEligibleForElection(2L, ELECCION_ID)).thenReturn(false);
        when(eligibilityRepository.isEligibleForElection(3L, ELECCION_ID)).thenReturn(true);
        when(votingTokenRepository.existsIssuedTokenFor(ELECCION_ID, 1L)).thenReturn(false);
        when(votingTokenRepository.existsIssuedTokenFor(ELECCION_ID, 3L)).thenReturn(false);
        when(votingTokenRepository.saveAllIssued(anyList())).thenAnswer(inv -> inv.getArgument(0));

        // When
        BulkIssueResult result = useCase.issueForElection(ELECCION_ID);

        // Then
        assertThat(result.issued()).isEqualTo(2);
        assertThat(result.skipped()).isEqualTo(1);
        assertThat(result.total()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should skip funcionarios that already have an ISSUED token")
    void issueForElection_shouldSkipExistingToken_whenAlreadyIssued() {
        // Given — funcionario 2 already has a token
        when(censoRepository.findAllFuncionarioIdsByEleccionId(ELECCION_ID))
                .thenReturn(List.of(1, 2));
        when(eligibilityRepository.isEligibleForElection(1L, ELECCION_ID)).thenReturn(true);
        when(eligibilityRepository.isEligibleForElection(2L, ELECCION_ID)).thenReturn(true);
        when(votingTokenRepository.existsIssuedTokenFor(ELECCION_ID, 1L)).thenReturn(false);
        when(votingTokenRepository.existsIssuedTokenFor(ELECCION_ID, 2L)).thenReturn(true);
        when(votingTokenRepository.saveAllIssued(anyList())).thenAnswer(inv -> inv.getArgument(0));

        // When
        BulkIssueResult result = useCase.issueForElection(ELECCION_ID);

        // Then
        assertThat(result.issued()).isEqualTo(1);
        assertThat(result.skipped()).isEqualTo(1);
        assertThat(result.total()).isEqualTo(2);
    }

    // ── Empty census fallback path ─────────────────────────────────────────────

    @Test
    @DisplayName("Should fall back to global eligible funcionarios when census is empty")
    void issueForElection_shouldFallbackToGlobal_whenCensusEmpty() {
        // Given — empty census → use global eligible list
        when(censoRepository.findAllFuncionarioIdsByEleccionId(ELECCION_ID))
                .thenReturn(List.of());
        Funcionario f1 = buildFuncionario(10);
        Funcionario f2 = buildFuncionario(20);
        when(funcionarioRepository.findEligibleByFilters(null, "ACTIVO", true))
                .thenReturn(List.of(f1, f2));
        when(eligibilityRepository.isEligibleForElection(10L, ELECCION_ID)).thenReturn(true);
        when(eligibilityRepository.isEligibleForElection(20L, ELECCION_ID)).thenReturn(true);
        when(votingTokenRepository.existsIssuedTokenFor(ELECCION_ID, 10L)).thenReturn(false);
        when(votingTokenRepository.existsIssuedTokenFor(ELECCION_ID, 20L)).thenReturn(false);
        when(votingTokenRepository.saveAllIssued(anyList())).thenAnswer(inv -> inv.getArgument(0));

        // When
        BulkIssueResult result = useCase.issueForElection(ELECCION_ID);

        // Then
        assertThat(result.issued()).isEqualTo(2);
        assertThat(result.total()).isEqualTo(2);
        verify(funcionarioRepository).findEligibleByFilters(null, "ACTIVO", true);
    }

    // ── Token generation quality ──────────────────────────────────────────────

    @Test
    @DisplayName("Should generate tokens with ISSUED status and non-null hash")
    void issueForElection_shouldGenerateIssuedTokens_withHashedContent() {
        // Given
        when(censoRepository.findAllFuncionarioIdsByEleccionId(ELECCION_ID))
                .thenReturn(List.of(99));
        when(eligibilityRepository.isEligibleForElection(99L, ELECCION_ID)).thenReturn(true);
        when(votingTokenRepository.existsIssuedTokenFor(ELECCION_ID, 99L)).thenReturn(false);
        when(votingTokenRepository.saveAllIssued(anyList())).thenAnswer(inv -> inv.getArgument(0));

        // When
        useCase.issueForElection(ELECCION_ID);

        // Then
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<VotingToken>> captor = ArgumentCaptor.forClass(List.class);
        verify(votingTokenRepository).saveAllIssued(captor.capture());

        List<VotingToken> savedTokens = captor.getValue();
        assertThat(savedTokens).hasSize(1);
        VotingToken token = savedTokens.get(0);
        assertThat(token.status()).isEqualTo(TokenStatus.ISSUED);
        assertThat(token.eleccionId()).isEqualTo(ELECCION_ID);
        assertThat(token.funcionarioId()).isEqualTo(99L);
        assertThat(token.tokenHash()).isNotBlank();
        assertThat(token.id()).isNotNull();
        assertThat(token.issuedAt()).isNotNull();
        // SHA-256 hex is always 64 chars
        assertThat(token.tokenHash()).hasSize(64);
    }

    @Test
    @DisplayName("Should never call saveAllIssued when all census members are skipped")
    void issueForElection_shouldNotCallSave_whenAllSkipped() {
        // Given — all ineligible
        when(censoRepository.findAllFuncionarioIdsByEleccionId(ELECCION_ID))
                .thenReturn(List.of(1, 2));
        when(eligibilityRepository.isEligibleForElection(1L, ELECCION_ID)).thenReturn(false);
        when(eligibilityRepository.isEligibleForElection(2L, ELECCION_ID)).thenReturn(false);

        // When
        BulkIssueResult result = useCase.issueForElection(ELECCION_ID);

        // Then
        assertThat(result.issued()).isEqualTo(0);
        assertThat(result.skipped()).isEqualTo(2);
        verify(votingTokenRepository, never()).saveAllIssued(anyList());
    }

    @Test
    @DisplayName("Should return zero counts when census and global fallback are both empty")
    void issueForElection_shouldReturnZero_whenNoFuncionariosExist() {
        // Given
        when(censoRepository.findAllFuncionarioIdsByEleccionId(ELECCION_ID))
                .thenReturn(List.of());
        when(funcionarioRepository.findEligibleByFilters(null, "ACTIVO", true))
                .thenReturn(List.of());

        // When
        BulkIssueResult result = useCase.issueForElection(ELECCION_ID);

        // Then
        assertThat(result.issued()).isEqualTo(0);
        assertThat(result.skipped()).isEqualTo(0);
        assertThat(result.total()).isEqualTo(0);
        verify(votingTokenRepository, never()).saveAllIssued(anyList());
    }

    // ── Issued-count accuracy (DB-level conflict semantics) ───────────────────

    @Test
    @DisplayName("Should report issued=0 on complete retry when saveAllIssued returns empty (all DB-conflicts)")
    void issueForElection_shouldReportZeroIssued_whenRetryFindsAllTokensExistAtDB() {
        // Given — two eligible candidates that pass both pre-checks but whose tokens
        // already exist in DB (DO NOTHING → saveAllIssued returns empty list)
        when(censoRepository.findAllFuncionarioIdsByEleccionId(ELECCION_ID))
                .thenReturn(List.of(1, 2));
        when(eligibilityRepository.isEligibleForElection(1L, ELECCION_ID)).thenReturn(true);
        when(eligibilityRepository.isEligibleForElection(2L, ELECCION_ID)).thenReturn(true);
        when(votingTokenRepository.existsIssuedTokenFor(ELECCION_ID, 1L)).thenReturn(false);
        when(votingTokenRepository.existsIssuedTokenFor(ELECCION_ID, 2L)).thenReturn(false);
        // Simulates a retry where all tokens are already in DB — adapter returns empty
        when(votingTokenRepository.saveAllIssued(anyList())).thenReturn(List.of());

        // When
        BulkIssueResult result = useCase.issueForElection(ELECCION_ID);

        // Then — issued must be 0, not 2 (count must reflect actual DB inserts)
        assertThat(result.issued()).isEqualTo(0);
        assertThat(result.total()).isEqualTo(2);
        // skipped = pre-check skips (0) + DB-level conflicts (2) = 2
        assertThat(result.skipped()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should report correct issued count when saveAllIssued partially conflicts at DB level")
    void issueForElection_shouldReportPartialIssued_whenSaveAllIssuedSkipsConflicts() {
        // Given — 3 candidates pass pre-checks; DB inserts 2, conflicts 1
        when(censoRepository.findAllFuncionarioIdsByEleccionId(ELECCION_ID))
                .thenReturn(List.of(1, 2, 3));
        when(eligibilityRepository.isEligibleForElection(1L, ELECCION_ID)).thenReturn(true);
        when(eligibilityRepository.isEligibleForElection(2L, ELECCION_ID)).thenReturn(true);
        when(eligibilityRepository.isEligibleForElection(3L, ELECCION_ID)).thenReturn(true);
        when(votingTokenRepository.existsIssuedTokenFor(ELECCION_ID, 1L)).thenReturn(false);
        when(votingTokenRepository.existsIssuedTokenFor(ELECCION_ID, 2L)).thenReturn(false);
        when(votingTokenRepository.existsIssuedTokenFor(ELECCION_ID, 3L)).thenReturn(false);
        // Adapter returns only 2 tokens (one was skipped by ON CONFLICT DO NOTHING)
        when(votingTokenRepository.saveAllIssued(anyList()))
                .thenAnswer(inv -> {
                    List<?> all = inv.getArgument(0);
                    return all.subList(0, 2); // first 2 inserted, last 1 conflict
                });

        // When
        BulkIssueResult result = useCase.issueForElection(ELECCION_ID);

        // Then
        assertThat(result.issued()).isEqualTo(2);
        assertThat(result.skipped()).isEqualTo(1); // DB-level conflict
        assertThat(result.total()).isEqualTo(3);
        // invariant: issued + skipped == total
        assertThat(result.issued() + result.skipped()).isEqualTo(result.total());
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private Funcionario buildFuncionario(Integer id) {
        return new Funcionario(id, "EMP-" + id, "DOC-" + id,
                "Nombre", "Apellido", "CC", "email@test.com",
                "123", 1, 1, LocalDate.now(), true, "ACTIVO", false);
    }
}
