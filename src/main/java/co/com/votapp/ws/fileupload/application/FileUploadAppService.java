package co.com.votapp.ws.fileupload.application;

import co.com.votapp.ws.fileupload.domain.model.StoreFileCommand;
import co.com.votapp.ws.fileupload.domain.port.in.StoreFileUseCase;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Application service — orchestrates the file upload flow.
 *
 * <p>Extracts raw bytes from {@link MultipartFile} (a servlet-layer type)
 * before constructing a {@link StoreFileCommand} so the domain use case
 * never depends on servlet APIs.
 */
@Service
public class FileUploadAppService {

    private final StoreFileUseCase storeFileUseCase;

    public FileUploadAppService(StoreFileUseCase storeFileUseCase) {
        this.storeFileUseCase = storeFileUseCase;
    }

    /**
     * Extracts bytes from the multipart file and delegates to the domain use case.
     *
     * @param multipartFile the uploaded file from the HTTP request
     * @return hosted URL path where the stored file can be retrieved
     */
    public String upload(MultipartFile multipartFile) {
        byte[] content = extractBytes(multipartFile);
        var command = new StoreFileCommand(
                content,
                multipartFile.getContentType(),
                multipartFile.getOriginalFilename()
        );
        return storeFileUseCase.store(command);
    }

    private byte[] extractBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new RuntimeException("Failed to read uploaded file bytes: " + ex.getMessage(), ex);
        }
    }
}
