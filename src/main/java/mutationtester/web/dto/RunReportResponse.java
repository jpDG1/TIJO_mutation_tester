package mutationtester.web.dto;

import mutationtester.report.MutantResult;
import mutationtester.report.MutationReport;

import java.util.List;
import java.util.UUID;

public record RunReportResponse(
        UUID runId,
        String sourceCode,
        double mutationScore,
        int killed,
        int timedOut,
        int survived,
        int invalid,
        List<MutantResult> mutants,
        List<Integer> survivingLines
) {

    public static RunReportResponse from(UUID runId, MutationReport report) {
        return new RunReportResponse(runId, report.sourceCode(), report.mutationScore(), report.killed(),
                report.timedOut(), report.survived(), report.invalid(), report.mutants(), report.survivingLines());
    }
}
