package co.com.votapp.ws.fileupload.domain.port.out;

/**
 * Output port — contract for binary file persistence.
 *
 * <p>Pure Java interface — no Spring annotations. Implemented by
 * {@code LocalFileStorageAdapter} (and future S3/GCS adapters without touching the domain).
 */
public interface FileStoragePort {

    /**
     * Persists the file and returns a publicly accessible URL path.
     *
     * @param filename    unique filename to use when persisting (e.g. {@code "uuid-photo.jpg"})
     * @param contentType MIME type of the file (e.g. {@code "image/jpeg"})
     * @param content     raw file bytes
     * @return hosted URL path (e.g. {@code "/files/uuid-photo.jpg"})
     */
    String store(String filename, String contentType, byte[] content);
}
