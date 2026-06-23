package co.com.votapp.ws.fileupload.infrastructure.adapter.out.storage;

import co.com.votapp.ws.fileupload.domain.port.out.FileStoragePort;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Output adapter — stores files on the local filesystem.
 *
 * <p>Implements {@link FileStoragePort} for MVP. Swap with S3/GCS adapter
 * when production storage is needed — the domain is completely unaffected.
 *
 * <p>Files are stored under the configured upload directory and served via the
 * static resource handler registered in {@code FileUploadConfig}.
 *
 * <p>NOT annotated with {@code @Component} — wired explicitly via
 * {@code FileUploadConfig.localFileStorageAdapter()} per hexagonal architecture convention
 * (adapters that require constructor args are never auto-detected).
 */
public class LocalFileStorageAdapter implements FileStoragePort {

    private final Path uploadDir;

    public LocalFileStorageAdapter(Path uploadDir) {
        this.uploadDir = uploadDir;
    }

    @Override
    public String store(String filename, String contentType, byte[] content) {
        try {
            Path destination = uploadDir.resolve(filename);
            Files.write(destination, content,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
            return "/files/" + filename;
        } catch (IOException ex) {
            throw new RuntimeException("Failed to store file '" + filename + "': " + ex.getMessage(), ex);
        }
    }
}
