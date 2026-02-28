package co.com.votapp.ws.common.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger documentation configuration.
 *
 * <p>Declares global API metadata (title, description, version, contact) and the
 * {@code basicAuth} HTTP Basic security scheme used by all {@code /api/**} endpoints.
 *
 * <p>SpringDoc reads these annotations at startup and merges them into the generated
 * OpenAPI 3.x JSON document served at {@code /v3/api-docs}.
 * Swagger UI is available at {@code /swagger-ui.html} (no credentials required).
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Electoral Votapp API",
                description = "REST API for the Electoral Votapp institutional voting platform. "
                        + "All /api/** endpoints require HTTP Basic authentication.",
                version = "1.0.0",
                contact = @Contact(
                        name = "Votapp Team",
                        email = "dev@votapp.com.co"
                )
        )
)
@SecurityScheme(
        name = "basicAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "basic",
        in = SecuritySchemeIn.HEADER,
        description = "HTTP Basic authentication. Required for all /api/** endpoints."
)
public class OpenApiConfig {
    // SpringDoc reads annotations at startup — no bean definitions required.
}
