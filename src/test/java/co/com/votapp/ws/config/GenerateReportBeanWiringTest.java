package co.com.votapp.ws.config;

import co.com.votapp.ws.electoral.domain.port.in.GenerateElectionReportUseCase;
import co.com.votapp.ws.electoral.domain.port.in.GetElectionResultsUseCase;
import co.com.votapp.ws.electoral.domain.port.out.ReportExporterPort;
import co.com.votapp.ws.electoral.domain.usecase.GenerateElectionReportUseCaseImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit test verifying {@link DomainConfig} produces the correct
 * {@link GenerateElectionReportUseCase} bean without loading a Spring context.
 */
@DisplayName("DomainConfig - GenerateElectionReportUseCase bean wiring")
class GenerateReportBeanWiringTest {

    private final DomainConfig config = new DomainConfig();

    @Test
    @DisplayName("Should create GenerateElectionReportUseCaseImpl as the GenerateElectionReportUseCase bean")
    void generateElectionReportUseCase_shouldBeGenerateElectionReportUseCaseImpl() {
        // Given
        GetElectionResultsUseCase resultsUseCase = mock(GetElectionResultsUseCase.class);
        ReportExporterPort pdfExporter = mock(ReportExporterPort.class);

        // When
        GenerateElectionReportUseCase bean = config.generateElectionReportUseCase(resultsUseCase, pdfExporter);

        // Then
        assertThat(bean).isNotNull().isInstanceOf(GenerateElectionReportUseCaseImpl.class);
    }
}
