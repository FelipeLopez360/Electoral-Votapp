package co.com.votapp.ws.fileupload.infrastructure.adapter.in.web;

import co.com.votapp.ws.fileupload.application.FileUploadAppService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST controller — handles file uploads via multipart/form-data.
 *
 * <p>Endpoint: {@code POST /api/v1/files/upload}
 * <p>Response: {@code 201 Created} with {@code {"url": "/files/uuid-filename.ext"}}
 * <p>Errors:
 * <ul>
 *   <li>400 — empty file, unsupported content type, or file too large (via ValidationException → GlobalExceptionHandler)</li>
 *   <li>401 — unauthenticated request (admin chain)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/files")
public class FileUploadController {

    private final FileUploadAppService fileUploadAppService;

    public FileUploadController(FileUploadAppService fileUploadAppService) {
        this.fileUploadAppService = fileUploadAppService;
    }

    /**
     * Accepts a multipart file upload and returns the hosted URL.
     *
     * @param file the uploaded file from the {@code "file"} multipart field
     * @return 201 with {@code {"url": "..."}} on success
     */
    @PostMapping("/upload")
    public ResponseEntity<UploadResponse> upload(@RequestParam("file") MultipartFile file) {
        String url = fileUploadAppService.upload(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(new UploadResponse(url));
    }

    /** Response DTO — record per architecture convention. */
    public record UploadResponse(String url) {}
}
