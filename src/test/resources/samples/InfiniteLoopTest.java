package samples;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InfiniteLoopTest {

    @Test
    void countsDownToZero() {
        // Arrange
        InfiniteLoopSource counter = new InfiniteLoopSource();

        // Act
        int result = counter.countDown(1);

        // Assert
        assertEquals(0, result);
    }
}
