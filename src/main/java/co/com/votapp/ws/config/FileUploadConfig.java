package co.com.votapp.ws.config;

import co.com.votapp.ws.fileupload.infrastructure.adapter.out.storage.LocalFileStorageAdapter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configuration for the file upload feature.
 *
 * <p>Responsibilities:
 * <ol>
 *   <li>Enable {@link FileUploadProperties} binding.</li>
 *   <li>Create the upload directory if it does not exist.</li>
 *   <li>Provide a {@link LocalFileStorageAdapter} bean wired with the upload directory.</li>
 *   <li>Register a Spring MVC static resource handler so {@code /files/**} requests
 *       are served directly from the upload directory.</li>
 * </ol>
 */
@Configuration
@EnableConfigurationProperties(FileUploadProperties.class)
public class FileUploadConfig implements WebMvcConfigurer {

    private final FileUploadProperties properties;

    public FileUploadConfig(FileUploadProperties properties) {
        this.properties = properties;
    }

    /**
     * Provides the upload directory {@link Path} bean used by {@link LocalFileStorageAdapter}.
     * Creates the directory eagerly if it does not exist.
     */
    @Bean
    public Path uploadDirPath() {
        Path dir = Paths.get(properties.uploadDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(dir);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to create upload directory: " + dir, ex);
        }
        return dir;
    }

    /**
     * Provides the {@link LocalFileStorageAdapter} wired with the resolved upload directory.
     */
    @Bean
    public LocalFileStorageAdapter localFileStorageAdapter(Path uploadDirPath) {
        return new LocalFileStorageAdapter(uploadDirPath);
    }

    /**
     * Registers static resource handler: {@code GET /files/**} → upload directory.
     *
     * <p>This allows the URL returned by the upload endpoint to be resolved directly
     * without a controller — Spring MVC serves the bytes from disk.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path dir = Paths.get(properties.uploadDir()).toAbsolutePath().normalize();
        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:" + dir.toString() + "/");
    }
}
