package mutationtester.web.dto;

import mutationtester.service.MutationRun;
import mutationtester.service.RunStatus;

public record RunProgressResponse(RunStatus status, int processedMutants, int totalMutants, String error) {

    public static RunProgressResponse from(MutationRun run) {
        return new RunProgressResponse(run.status(), run.progress().processedMutants(),
                run.progress().totalMutants(), run.error());
    }
}
