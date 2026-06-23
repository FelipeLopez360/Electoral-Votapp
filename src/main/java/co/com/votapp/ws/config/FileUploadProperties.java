package co.com.votapp.ws.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Type-safe configuration for the file upload feature.
 *
 * <p>Sourced from {@code app.file-upload.*} in {@code application.yaml}.
 * Constructor binding is automatic for records (no {@code @ConstructorBinding} needed
 * per Spring Boot 4.0 docs).
 *
 * @param uploadDir   filesystem directory where uploaded files are stored
 * @param maxSizeBytes maximum allowed upload size in bytes (default 5 MB)
 * @param allowedTypes comma-separated list of accepted MIME types
 */
@ConfigurationProperties(prefix = "app.file-upload")
public record FileUploadProperties(
        String uploadDir,
        long maxSizeBytes,
        String allowedTypes
) {
    private static final long DEFAULT_MAX_SIZE = 5L * 1024 * 1024; // 5 MB
    private static final String DEFAULT_TYPES = "image/jpeg,image/png,image/webp";
    private static final String DEFAULT_DIR = System.getProperty("java.io.tmpdir") + "/votapp-uploads";

    public FileUploadProperties {
        if (uploadDir == null || uploadDir.isBlank()) {
            uploadDir = DEFAULT_DIR;
        }
        if (maxSizeBytes <= 0) {
            maxSizeBytes = DEFAULT_MAX_SIZE;
        }
        if (allowedTypes == null || allowedTypes.isBlank()) {
            allowedTypes = DEFAULT_TYPES;
        }
    }
}
