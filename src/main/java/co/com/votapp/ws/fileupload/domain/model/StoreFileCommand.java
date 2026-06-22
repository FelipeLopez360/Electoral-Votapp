package co.com.votapp.ws.fileupload.domain.model;

/**
 * Command carrying the file data needed to store a file.
 *
 * <p>Pure Java record — ZERO Spring/JPA/Jackson imports.
 * The controller extracts bytes from {@code MultipartFile} before constructing this command
 * so the domain never depends on the servlet layer.
 *
 * @param content         raw file bytes (may be null or empty — validated by the use case)
 * @param contentType     MIME type declared by the caller (e.g. "image/jpeg")
 * @param originalFilename original filename from the multipart upload
 */
public record StoreFileCommand(
        byte[] content,
        String contentType,
        String originalFilename
) {}
