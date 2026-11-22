package mutationtester.verification;

import mutationtester.verification.SurefireReportReader.ReportSummary;
import mutationtester.verification.SurefireReportReader.TestCounts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SurefireReportReaderTest {

    @TempDir
    Path reports;

    @Test
    void totalsActualResultsByTestSuite() throws IOException {
        // Arrange
        Files.writeString(reports.resolve("mutationtester.report.ScoreCalculatorTest.txt"),
                "Tests run: 4, Failures: 1, Errors: 0, Skipped: 1, Time elapsed: 0.2 s");
        Files.writeString(reports.resolve("mutationtester.report.MutationReportTest.txt"),
                "Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.1 s");
        Files.writeString(reports.resolve("mutationtester.integration.MutationRunControllerIntegrationTest.txt"),
                "Tests run: 5, Failures: 0, Errors: 1, Skipped: 0, Time elapsed: 1.3 s");
        Files.writeString(reports.resolve("samples.PriceCalculatorStrongTest.txt"),
                "Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.1 s");

        // Act
        ReportSummary summary = SurefireReportReader.read(reports);

        // Assert
        assertEquals(new TestCounts(4, 1, 1), summary.unit());
        assertEquals(new TestCounts(4, 1, 0), summary.integration());
    }

    @Test
    void returnsEmptyCountsBeforeMavenWritesReports() throws IOException {
        // Arrange
        Path missingDirectory = reports.resolve("missing");

        // Act
        ReportSummary summary = SurefireReportReader.read(missingDirectory);

        // Assert
        TestCounts empty = new TestCounts(0, 0, 0);
        assertEquals(new ReportSummary(empty, empty), summary);
    }
}
