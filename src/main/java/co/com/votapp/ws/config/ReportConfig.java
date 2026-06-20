package co.com.votapp.ws.config;

import co.com.votapp.ws.electoral.domain.port.out.ReportExporterPort;
import co.com.votapp.ws.electoral.infrastructure.adapter.out.report.OpenPdfReportExporter;
import co.com.votapp.ws.electoral.infrastructure.adapter.out.report.PoiExcelReportExporter;
import co.com.votapp.ws.electoral.infrastructure.adapter.out.report.ReportExporterDispatcher;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Spring configuration for report generation infrastructure.
 *
 * <p>Wires the format-specific {@link ReportExporterPort} implementations and a
 * {@link ReportExporterDispatcher} that routes by format. The dispatcher is the
 * {@code @Primary} bean injected into the domain use case.
 *
 * <p>Per architecture rules: ALL {@code @Configuration} classes live in {@code config/},
 * never inside adapter packages.
 */
@Configuration
@EnableConfigurationProperties(ReportProperties.class)
public class ReportConfig {

    /**
     * PDF report exporter bean — uses OpenPDF for streaming PDF generation.
     *
     * @param reportProperties watermark config sourced from {@code app.report.*}
     * @return a {@link ReportExporterPort} implementation for PDF only
     */
    @Bean
    public ReportExporterPort pdfReportExporter(ReportProperties reportProperties) {
        return new OpenPdfReportExporter(reportProperties.watermarkText());
    }

    /**
     * Excel report exporter bean — uses Apache POI SXSSF for streaming XLSX generation.
     *
     * @param reportProperties watermark config sourced from {@code app.report.*}
     * @return a {@link ReportExporterPort} implementation for XLSX only
     */
    @Bean
    public ReportExporterPort xlsxReportExporter(ReportProperties reportProperties) {
        return new PoiExcelReportExporter(reportProperties.watermarkText());
    }

    /**
     * Dispatcher bean: the primary {@link ReportExporterPort} injected into the domain use case.
     *
     * <p>Routes {@code export()} calls to the correct format-specific adapter based on
     * the {@link co.com.votapp.ws.electoral.domain.model.ReportFormat} parameter.
     *
     * @param pdfReportExporter  the PDF-specific exporter
     * @param xlsxReportExporter the XLSX-specific exporter
     * @return a composite dispatcher implementing the single-port contract
     */
    @Bean
    @Primary
    public ReportExporterPort reportExporterDispatcher(ReportExporterPort pdfReportExporter,
                                                        ReportExporterPort xlsxReportExporter) {
        return new ReportExporterDispatcher(pdfReportExporter, xlsxReportExporter);
    }
}
