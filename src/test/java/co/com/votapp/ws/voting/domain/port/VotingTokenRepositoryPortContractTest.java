package co.com.votapp.ws.voting.domain.port;

import co.com.votapp.ws.voting.domain.VotingToken;
import co.com.votapp.ws.voting.domain.port.out.VotingTokenRepository;
import co.com.votapp.ws.voting.domain.TokenStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract tests verifying new methods were added to {@link VotingTokenRepository} (Task 1.2).
 *
 * <p>RED phase: compile-fails until findIssuedByFuncionarioAndEleccion and findById are added.
 */
@DisplayName("VotingTokenRepository - Contract tests for new portal methods")
class VotingTokenRepositoryPortContractTest {

    @Test
    @DisplayName("Should expose findIssuedByFuncionarioAndEleccion(funcionarioId, eleccionId) method")
    void votingTokenRepository_shouldHaveFindIssuedByFuncionarioAndEleccion() throws NoSuchMethodException {
        // Then — verify the method signature exists on the interface
        var method = VotingTokenRepository.class.getMethod(
                "findIssuedByFuncionarioAndEleccion", Integer.class, UUID.class);
        assertThat(method).isNotNull();
        assertThat(method.getReturnType()).isEqualTo(Optional.class);
    }

    @Test
    @DisplayName("Should expose findById(UUID tokenId) method")
    void votingTokenRepository_shouldHaveFindById() throws NoSuchMethodException {
        // Then — verify the method signature exists on the interface
        var method = VotingTokenRepository.class.getMethod("findById", UUID.class);
        assertThat(method).isNotNull();
        assertThat(method.getReturnType()).isEqualTo(Optional.class);
    }

    @Test
    @DisplayName("Should return an ISSUED token when searching by funcionario and election")
    void findIssuedByFuncionarioAndEleccion_shouldReturnToken_whenExists() {
        // Given
        UUID eleccionId = UUID.randomUUID();
        UUID tokenId = UUID.randomUUID();
        Integer funcionarioId = 42;

        VotingTokenRepository repo = Mockito.mock(VotingTokenRepository.class);
        VotingToken token = new VotingToken(tokenId, eleccionId, 42L, "hash", TokenStatus.ISSUED, Instant.now());
        Mockito.when(repo.findIssuedByFuncionarioAndEleccion(funcionarioId, eleccionId))
                .thenReturn(Optional.of(token));

        // When
        Optional<VotingToken> result = repo.findIssuedByFuncionarioAndEleccion(funcionarioId, eleccionId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().status()).isEqualTo(TokenStatus.ISSUED);
    }

    @Test
    @DisplayName("Should return a token when searching by tokenId")
    void findById_shouldReturnToken_whenExists() {
        // Given
        UUID tokenId = UUID.randomUUID();
        UUID eleccionId = UUID.randomUUID();

        VotingTokenRepository repo = Mockito.mock(VotingTokenRepository.class);
        VotingToken token = new VotingToken(tokenId, eleccionId, 42L, "hash", TokenStatus.ISSUED, Instant.now());
        Mockito.when(repo.findById(tokenId)).thenReturn(Optional.of(token));

        // When
        Optional<VotingToken> result = repo.findById(tokenId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(tokenId);
    }
}
