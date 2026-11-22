package mutationtester.progress;

@FunctionalInterface
public interface ProgressListener {

    void onProgress(RunProgress progress);
}
