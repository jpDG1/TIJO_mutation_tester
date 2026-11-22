package mutationtester.web;

import io.swagger.v3.oas.annotations.Operation;
import mutationtester.verification.SurefireReportReader;
import mutationtester.verification.SurefireReportReader.ReportSummary;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Path;

@RestController
public final class ProjectVerificationController {

    private static final Path REPORT_DIRECTORY = Path.of("target", "surefire-reports");

    @GetMapping(value = "/api/project-verification", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Read the latest available Maven test results for this project")
    public ReportSummary results() throws IOException {
        return SurefireReportReader.read(REPORT_DIRECTORY);
    }
}
