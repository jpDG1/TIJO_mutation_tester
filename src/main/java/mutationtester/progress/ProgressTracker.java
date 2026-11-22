package mutationtester.progress;

import java.util.List;

public final class ProgressTracker {

    private final int totalMutants;
    private final List<ProgressListener> listeners;
    private int processedMutants;

    public ProgressTracker(int totalMutants, List<ProgressListener> listeners) {
        this.totalMutants = totalMutants;
        this.listeners = List.copyOf(listeners);
    }

    public RunProgress currentProgress() {
        return new RunProgress(processedMutants, totalMutants);
    }

    public void publishProgress() {
        RunProgress progress = currentProgress();
        listeners.forEach(listener -> listener.onProgress(progress));
    }

    public void mutantProcessed() {
        processedMutants++;
        publishProgress();
    }
}
