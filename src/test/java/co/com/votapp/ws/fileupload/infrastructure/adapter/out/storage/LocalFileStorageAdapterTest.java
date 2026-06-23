package co.com.votapp.ws.fileupload.infrastructure.adapter.out.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link LocalFileStorageAdapter}.
 *
 * <p>TDD RED cycle: written BEFORE production code.
 * Uses JUnit 5 {@code @TempDir} for an isolated, auto-cleaned filesystem.
 * No Spring context — pure unit test.
 */
@DisplayName("LocalFileStorageAdapter - Local filesystem storage adapter")
class LocalFileStorageAdapterTest {

    @TempDir
    Path uploadDir;

    private LocalFileStorageAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new LocalFileStorageAdapter(uploadDir);
    }

    // ── File written to disk ──────────────────────────────────────────────────

    @Test
    @DisplayName("Should write file to upload directory with the given filename")
    void store_shouldWriteFileToDisk_withGivenFilename() throws IOException {
        // Given
        byte[] content = "JPEG-bytes".getBytes();
        String filename = "abc123-photo.jpg";

        // When
        adapter.store(filename, "image/jpeg", content);

        // Then — the file exists on disk with the exact content
        Path written = uploadDir.resolve(filename);
        assertThat(written).exists();
        assertThat(Files.readAllBytes(written)).isEqualTo(content);
    }

    @Test
    @DisplayName("Should write different files independently without overwriting")
    void store_shouldWriteIndependentFiles_whenCalledTwice() throws IOException {
        // Given
        byte[] first = "first-content".getBytes();
        byte[] second = "second-content".getBytes();

        // When
        adapter.store("file1-photo.jpg", "image/jpeg", first);
        adapter.store("file2-banner.png", "image/png", second);

        // Then — both files exist and are independent
        assertThat(Files.readAllBytes(uploadDir.resolve("file1-photo.jpg"))).isEqualTo(first);
        assertThat(Files.readAllBytes(uploadDir.resolve("file2-banner.png"))).isEqualTo(second);
    }

    // ── URL returned ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return URL path /files/{filename}")
    void store_shouldReturnCorrectUrlPath_whenFileStored() {
        // Given
        byte[] content = "PNG-bytes".getBytes();
        String filename = "uuid-banner.png";

        // When
        String url = adapter.store(filename, "image/png", content);

        // Then
        assertThat(url).isEqualTo("/files/" + filename);
    }

    @Test
    @DisplayName("Should return URL that includes the full filename with UUID prefix")
    void store_shouldReturnUrlWithUuidPrefix_whenFilenameHasUuid() {
        // Given
        byte[] content = "WEBP-bytes".getBytes();
        String filename = "550e8400-e29b-41d4-a716-446655440000-logo.webp";

        // When
        String url = adapter.store(filename, "image/webp", content);

        // Then
        assertThat(url).isEqualTo("/files/550e8400-e29b-41d4-a716-446655440000-logo.webp");
        assertThat(url).startsWith("/files/");
    }

    // ── IOException propagation ───────────────────────────────────────────────

    @Test
    @DisplayName("Should throw RuntimeException wrapping IOException when upload dir is not writable")
    void store_shouldThrowRuntimeException_whenDirectoryNotWritable() {
        // Given — a non-existent directory that cannot be written to
        Path nonExistentDir = Path.of("/tmp/does-not-exist-xyz-votapp/uploads");
        LocalFileStorageAdapter brokenAdapter = new LocalFileStorageAdapter(nonExistentDir);

        // When & Then
        assertThatThrownBy(() -> brokenAdapter.store("file.jpg", "image/jpeg", "data".getBytes()))
                .isInstanceOf(RuntimeException.class);
    }
}
