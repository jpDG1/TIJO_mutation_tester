package mutationtester.progress;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProgressTrackerTest {

    @Test
    void notifiesEveryListenerAfterEachMutant() {
        // Arrange
        List<RunProgress> firstUpdates = new ArrayList<>();
        List<RunProgress> secondUpdates = new ArrayList<>();
        ProgressTracker tracker = new ProgressTracker(2, List.of(firstUpdates::add, secondUpdates::add));

        // Act
        tracker.publishProgress();
        tracker.mutantProcessed();
        tracker.mutantProcessed();

        // Assert
        List<RunProgress> expected = List.of(new RunProgress(0, 2), new RunProgress(1, 2), new RunProgress(2, 2));
        assertEquals(expected, firstUpdates);
        assertEquals(expected, secondUpdates);
    }
}
