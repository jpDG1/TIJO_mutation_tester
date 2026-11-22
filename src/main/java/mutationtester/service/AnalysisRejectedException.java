package mutationtester.service;

import java.util.List;

public final class AnalysisRejectedException extends RuntimeException {

    private final List<String> diagnostics;

    public AnalysisRejectedException(String message) {
        this(message, List.of());
    }

    public AnalysisRejectedException(String message, List<String> diagnostics) {
        super(message);
        this.diagnostics = List.copyOf(diagnostics);
    }

    public List<String> diagnostics() {
        return diagnostics;
    }
}
