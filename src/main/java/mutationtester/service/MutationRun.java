package mutationtester.service;

import mutationtester.progress.RunProgress;
import mutationtester.report.MutationReport;

import java.util.UUID;

public record MutationRun(
        UUID runId,
        RunStatus status,
        RunProgress progress,
        MutationReport report,
        String error
) {
}
