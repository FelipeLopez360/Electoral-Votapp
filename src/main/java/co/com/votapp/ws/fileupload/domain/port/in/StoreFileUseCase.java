package co.com.votapp.ws.fileupload.domain.port.in;

import co.com.votapp.ws.fileupload.domain.model.StoreFileCommand;

/**
 * Input port — declares the file-storage use case for driving adapters.
 *
 * <p>Pure Java interface — no Spring annotations. Implemented by
 * {@code StoreFileUseCaseImpl} and wired via {@code DomainConfig}.
 */
public interface StoreFileUseCase {

    /**
     * Validates and stores the file described by the command.
     *
     * @param command file bytes, content type, and original filename
     * @return hosted URL where the stored file can be retrieved (e.g. {@code /files/uuid-name.jpg})
     * @throws co.com.votapp.ws.common.exception.ValidationException if the file is empty,
     *         exceeds the size limit, or has an unsupported content type
     */
    String store(StoreFileCommand command);
}
