package co.com.votapp.ws.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Type-safe configuration properties for election report generation.
 *
 * <p>Sourced from {@code app.report.*} in {@code application.yaml}.
 * Constructor binding is automatic for records (no {@code @ConstructorBinding} needed
 * per Spring Boot 4.0 docs).
 *
 * @param watermarkText the institutional watermark text embedded in generated PDF/Excel reports
 */
@ConfigurationProperties(prefix = "app.report")
public record ReportProperties(
        String watermarkText
) {
    public ReportProperties {
        if (watermarkText == null || watermarkText.isBlank()) {
            watermarkText = "Votapp - Sistema Electoral";
        }
    }
}
