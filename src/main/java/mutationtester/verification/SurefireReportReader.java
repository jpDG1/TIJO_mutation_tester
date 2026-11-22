package mutationtester.verification;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class SurefireReportReader {

    private static final Pattern SUMMARY = Pattern.compile(
            "^Tests run: (\\d+), Failures: (\\d+), Errors: (\\d+), Skipped: (\\d+)");
    private static final TestCounts EMPTY = new TestCounts(0, 0, 0);

    private SurefireReportReader() {
    }

    public static ReportSummary read(Path reportDirectory) throws IOException {
        if (!Files.isDirectory(reportDirectory)) {
            return new ReportSummary(EMPTY, EMPTY);
        }

        TestCounts unit = EMPTY;
        TestCounts integration = EMPTY;
        try (Stream<Path> files = Files.list(reportDirectory)) {
            for (Path file : files.filter(SurefireReportReader::isProjectReport).toList()) {
                TestCounts counts = readCounts(file).orElse(EMPTY);
                if (file.getFileName().toString().startsWith("mutationtester.integration.")) {
                    integration = integration.plus(counts);
                } else {
                    unit = unit.plus(counts);
                }
            }
        }
        return new ReportSummary(unit, integration);
    }

    private static boolean isProjectReport(Path file) {
        String name = file.getFileName().toString();
        return name.startsWith("mutationtester.") && name.endsWith("Test.txt") && Files.isRegularFile(file);
    }

    private static Optional<TestCounts> readCounts(Path file) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            return reader.lines().limit(6)
                    .map(SUMMARY::matcher)
                    .filter(Matcher::find)
                    .map(SurefireReportReader::parseCounts)
                    .findFirst();
        }
    }

    private static TestCounts parseCounts(Matcher summary) {
        int total = Integer.parseInt(summary.group(1));
        int failed = Integer.parseInt(summary.group(2)) + Integer.parseInt(summary.group(3));
        int skipped = Integer.parseInt(summary.group(4));
        return new TestCounts(total - failed - skipped, failed, skipped);
    }

    public record ReportSummary(TestCounts unit, TestCounts integration) {
    }

    public record TestCounts(int passed, int failed, int skipped) {

        private TestCounts plus(TestCounts other) {
            return new TestCounts(passed + other.passed, failed + other.failed, skipped + other.skipped);
        }
    }
}
