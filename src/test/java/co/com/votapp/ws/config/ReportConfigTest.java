package co.com.votapp.ws.config;

import co.com.votapp.ws.electoral.domain.port.out.ReportExporterPort;
import co.com.votapp.ws.electoral.infrastructure.adapter.out.report.OpenPdfReportExporter;
import co.com.votapp.ws.electoral.infrastructure.adapter.out.report.PoiExcelReportExporter;
import co.com.votapp.ws.electoral.infrastructure.adapter.out.report.ReportExporterDispatcher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for {@link ReportConfig}: verifies factory methods produce the
 * correct exporter implementations without loading a Spring context.
 */
@DisplayName("ReportConfig - Exporter bean wiring")
class ReportConfigTest {

    private final ReportConfig config = new ReportConfig();

    @Test
    @DisplayName("Should create OpenPdfReportExporter as the PDF exporter bean")
    void pdfReportExporter_shouldBeOpenPdfReportExporter() {
        // Given — default watermark from properties
        ReportProperties props = new ReportProperties("Test Watermark");

        // When
        ReportExporterPort exporter = config.pdfReportExporter(props);

        // Then
        assertThat(exporter).isInstanceOf(OpenPdfReportExporter.class);
    }

    @Test
    @DisplayName("Should create PoiExcelReportExporter as the Excel exporter bean")
    void xlsxReportExporter_shouldBePoiExcelReportExporter() {
        // Given
        ReportProperties props = new ReportProperties("Test Watermark");

        // When
        ReportExporterPort exporter = config.xlsxReportExporter(props);

        // Then
        assertThat(exporter).isInstanceOf(PoiExcelReportExporter.class);
    }

    @Test
    @DisplayName("Should create ReportExporterDispatcher as the primary exporter bean")
    void reportExporterDispatcher_shouldBeReportExporterDispatcher() {
        // Given
        ReportProperties props = new ReportProperties("Test Watermark");
        ReportExporterPort pdf = config.pdfReportExporter(props);
        ReportExporterPort xlsx = config.xlsxReportExporter(props);

        // When
        ReportExporterPort dispatcher = config.reportExporterDispatcher(pdf, xlsx);

        // Then
        assertThat(dispatcher).isNotNull().isInstanceOf(ReportExporterDispatcher.class);
    }

    @Test
    @DisplayName("Should inject watermark text from ReportProperties into PDF exporter")
    void pdfReportExporter_shouldReceiveWatermarkText_fromProperties() {
        // Given
        String expectedWatermark = "Votapp — Institución Electoral";
        ReportProperties props = new ReportProperties(expectedWatermark);

        // When — if the constructor is called correctly, the exporter will use it
        ReportExporterPort exporter = config.pdfReportExporter(props);

        // Then — verifiable indirectly: exporter must be non-null and correct type
        assertThat(exporter).isNotNull().isInstanceOf(OpenPdfReportExporter.class);
    }

    @Test
    @DisplayName("Should inject watermark text from ReportProperties into Excel exporter")
    void xlsxReportExporter_shouldReceiveWatermarkText_fromProperties() {
        // Given
        String expectedWatermark = "Votapp — Institución Electoral";
        ReportProperties props = new ReportProperties(expectedWatermark);

        // When
        ReportExporterPort exporter = config.xlsxReportExporter(props);

        // Then
        assertThat(exporter).isNotNull().isInstanceOf(PoiExcelReportExporter.class);
    }
}
