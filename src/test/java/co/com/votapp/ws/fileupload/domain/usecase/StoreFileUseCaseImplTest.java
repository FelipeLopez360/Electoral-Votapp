package co.com.votapp.ws.fileupload.domain.usecase;

import co.com.votapp.ws.common.exception.ValidationException;
import co.com.votapp.ws.fileupload.domain.model.StoreFileCommand;
import co.com.votapp.ws.fileupload.domain.port.in.StoreFileUseCase;
import co.com.votapp.ws.fileupload.domain.port.out.FileStoragePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link StoreFileUseCaseImpl}.
 *
 * <p>TDD RED cycle: written BEFORE the production code exists.
 * Validates domain invariants: empty bytes, unsupported content type, oversized file.
 */
@DisplayName("StoreFileUseCaseImpl - File storage domain business logic")
@ExtendWith(MockitoExtension.class)
class StoreFileUseCaseImplTest {

    @Mock
    private FileStoragePort fileStoragePort;

    private StoreFileUseCase useCase;

    // Max allowed size: 5 MB
    private static final long MAX_SIZE_BYTES = 5 * 1024 * 1024;

    @BeforeEach
    void setUp() {
        useCase = new StoreFileUseCaseImpl(fileStoragePort);
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should delegate to FileStoragePort and return URL when file is valid JPEG")
    void store_shouldReturnUrl_whenValidJpegProvided() {
        // Given
        byte[] content = new byte[1024]; // 1 KB — well within limit
        var command = new StoreFileCommand(content, "image/jpeg", "photo.jpg");
        when(fileStoragePort.store(anyString(), anyString(), any(byte[].class)))
                .thenReturn("/files/uuid-photo.jpg");

        // When
        String url = useCase.store(command);

        // Then
        assertThat(url).isEqualTo("/files/uuid-photo.jpg");
        verify(fileStoragePort).store(anyString(), anyString(), any(byte[].class));
    }

    @Test
    @DisplayName("Should delegate to FileStoragePort and return URL when file is valid PNG")
    void store_shouldReturnUrl_whenValidPngProvided() {
        // Given
        byte[] content = new byte[2048]; // 2 KB
        var command = new StoreFileCommand(content, "image/png", "logo.png");
        when(fileStoragePort.store(anyString(), anyString(), any(byte[].class)))
                .thenReturn("/files/uuid-logo.png");

        // When
        String url = useCase.store(command);

        // Then
        assertThat(url).isEqualTo("/files/uuid-logo.png");
        verify(fileStoragePort).store(anyString(), anyString(), any(byte[].class));
    }

    @Test
    @DisplayName("Should delegate to FileStoragePort and return URL when file is valid WebP")
    void store_shouldReturnUrl_whenValidWebpProvided() {
        // Given
        byte[] content = new byte[512];
        var command = new StoreFileCommand(content, "image/webp", "banner.webp");
        when(fileStoragePort.store(anyString(), anyString(), any(byte[].class)))
                .thenReturn("/files/uuid-banner.webp");

        // When
        String url = useCase.store(command);

        // Then
        assertThat(url).isEqualTo("/files/uuid-banner.webp");
    }

    // ── Empty content ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should throw ValidationException when byte array is empty")
    void store_shouldThrowValidationException_whenContentIsEmpty() {
        // Given
        byte[] emptyContent = new byte[0];
        var command = new StoreFileCommand(emptyContent, "image/jpeg", "photo.jpg");

        // When & Then
        assertThatThrownBy(() -> useCase.store(command))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("empty");

        verify(fileStoragePort, never()).store(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Should throw ValidationException when byte array is null")
    void store_shouldThrowValidationException_whenContentIsNull() {
        // Given
        var command = new StoreFileCommand(null, "image/jpeg", "photo.jpg");

        // When & Then
        assertThatThrownBy(() -> useCase.store(command))
                .isInstanceOf(ValidationException.class);

        verify(fileStoragePort, never()).store(anyString(), anyString(), any());
    }

    // ── Unsupported content type ──────────────────────────────────────────────

    @Test
    @DisplayName("Should throw ValidationException when content type is not supported (PDF)")
    void store_shouldThrowValidationException_whenContentTypeIsPdf() {
        // Given
        byte[] content = new byte[1024];
        var command = new StoreFileCommand(content, "application/pdf", "doc.pdf");

        // When & Then
        assertThatThrownBy(() -> useCase.store(command))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("content type");

        verify(fileStoragePort, never()).store(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Should throw ValidationException when content type is not supported (text/plain)")
    void store_shouldThrowValidationException_whenContentTypeIsText() {
        // Given
        byte[] content = new byte[512];
        var command = new StoreFileCommand(content, "text/plain", "notes.txt");

        // When & Then
        assertThatThrownBy(() -> useCase.store(command))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("content type");

        verify(fileStoragePort, never()).store(anyString(), anyString(), any());
    }

    // ── File too large ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should throw ValidationException when file exceeds 5 MB limit")
    void store_shouldThrowValidationException_whenFileTooLarge() {
        // Given — exactly 1 byte over the limit
        byte[] oversized = new byte[(int) MAX_SIZE_BYTES + 1];
        var command = new StoreFileCommand(oversized, "image/jpeg", "big.jpg");

        // When & Then
        assertThatThrownBy(() -> useCase.store(command))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("size");

        verify(fileStoragePort, never()).store(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Should accept file at exactly 5 MB limit")
    void store_shouldAcceptFile_whenFileIsExactlyAtLimit() {
        // Given — exactly at the limit
        byte[] exactLimit = new byte[(int) MAX_SIZE_BYTES];
        var command = new StoreFileCommand(exactLimit, "image/jpeg", "exactly5mb.jpg");
        when(fileStoragePort.store(anyString(), anyString(), any(byte[].class)))
                .thenReturn("/files/uuid-exactly5mb.jpg");

        // When
        String url = useCase.store(command);

        // Then — no exception; storage is called
        assertThat(url).isNotBlank();
        verify(fileStoragePort).store(anyString(), anyString(), any(byte[].class));
    }
}
