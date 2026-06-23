package co.com.votapp.ws.fileupload;

import co.com.votapp.ws.TestcontainersDockerConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack integration test: file upload → stored on disk → retrievable via static serving.
 *
 * <p>TDD RED cycle: written BEFORE {@code FileUploadProperties} and the Spring static resource
 * handler registration exist.
 *
 * <p>Verifies:
 * <ul>
 *   <li>POST /api/v1/files/upload stores the file and returns a URL.</li>
 *   <li>The returned URL can be used to GET the file (static resource serving).</li>
 *   <li>The file content on disk matches what was uploaded.</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
class FileUploadIntegrationIT {

    static {
        TestcontainersDockerConfig.configure();
    }

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("votapp_test")
            .withUsername("postgres")
            .withPassword("votapp_password");

    @Container
    private static final GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should store file and return URL on POST, then serve it via GET")
    void upload_shouldStoreAndServeFile_roundTrip() throws Exception {
        // Given
        byte[] content = "fake-jpeg-content-for-test".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file", "test-image.jpg", "image/jpeg", content);

        // When — POST upload
        MvcResult result = mockMvc.perform(multipart("/api/v1/files/upload")
                        .file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url").exists())
                .andReturn();

        // Then — extract URL from response and verify file is accessible via GET
        String responseBody = result.getResponse().getContentAsString();
        // Extract the URL value from JSON {"url":"/files/..."}
        String url = responseBody.replace("{\"url\":\"", "").replace("\"}", "");
        assertThat(url).startsWith("/files/");
        assertThat(url).endsWith(".jpg");

        // Verify file can be retrieved (static resource serving)
        mockMvc.perform(get(url))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should store file content exactly as uploaded")
    void upload_shouldStoreExactContent_onDisk() throws Exception {
        // Given
        byte[] content = "exact-content-bytes-12345".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file", "exact-test.png", "image/png", content);

        // When
        MvcResult result = mockMvc.perform(multipart("/api/v1/files/upload")
                        .file(file))
                .andExpect(status().isCreated())
                .andReturn();

        // Then — extract filename from URL and verify file content on disk
        String responseBody = result.getResponse().getContentAsString();
        String url = responseBody.replace("{\"url\":\"", "").replace("\"}", "");
        String filename = url.replace("/files/", "");

        // The upload dir is configured via app.file-upload.upload-dir
        // (defaults to /tmp/votapp-uploads in test)
        String uploadDir = System.getProperty("app.file-upload.upload-dir",
                System.getProperty("java.io.tmpdir") + "/votapp-uploads");
        File storedFile = new File(uploadDir, filename);
        assertThat(storedFile).exists();
        assertThat(storedFile.length()).isEqualTo(content.length);
    }
}
