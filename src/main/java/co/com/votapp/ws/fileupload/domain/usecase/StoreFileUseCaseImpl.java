package co.com.votapp.ws.fileupload.domain.usecase;

import co.com.votapp.ws.common.exception.ValidationException;
import co.com.votapp.ws.fileupload.domain.model.StoreFileCommand;
import co.com.votapp.ws.fileupload.domain.port.in.StoreFileUseCase;
import co.com.votapp.ws.fileupload.domain.port.out.FileStoragePort;

import java.util.Set;
import java.util.UUID;

/**
 * Domain use case: validate file constraints then delegate to the storage port.
 *
 * <p>ZERO Spring annotations — wired manually in {@code DomainConfig}.
 * Domain rules enforced here:
 * <ul>
 *   <li>Content must be non-null and non-empty.</li>
 *   <li>Content type must be one of: image/jpeg, image/png, image/webp.</li>
 *   <li>File size must not exceed {@value MAX_SIZE_BYTES} bytes (5 MB).</li>
 * </ul>
 */
public class StoreFileUseCaseImpl implements StoreFileUseCase {

    static final long MAX_SIZE_BYTES = 5L * 1024 * 1024; // 5 MB

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final FileStoragePort fileStoragePort;

    public StoreFileUseCaseImpl(FileStoragePort fileStoragePort) {
        this.fileStoragePort = fileStoragePort;
    }

    @Override
    public String store(StoreFileCommand command) {
        validateContent(command.content());
        validateContentType(command.contentType());
        validateSize(command.content());

        String filename = buildFilename(command.originalFilename());
        return fileStoragePort.store(filename, command.contentType(), command.content());
    }

    // ─── Validation ──────────────────────────────────────────────────────────

    private void validateContent(byte[] content) {
        if (content == null || content.length == 0) {
            throw new ValidationException("File content must not be empty");
        }
    }

    private void validateContentType(String contentType) {
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new ValidationException(
                    "Unsupported content type: '" + contentType + "'. Allowed types: " + ALLOWED_CONTENT_TYPES);
        }
    }

    private void validateSize(byte[] content) {
        if (content.length > MAX_SIZE_BYTES) {
            throw new ValidationException(
                    "File size " + content.length + " bytes exceeds the maximum allowed size of " + MAX_SIZE_BYTES + " bytes");
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private String buildFilename(String originalFilename) {
        String safe = (originalFilename != null && !originalFilename.isBlank())
                ? originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_")
                : "file";
        return UUID.randomUUID() + "-" + safe;
    }
}
