package samples;

public class InfiniteLoopSource {

    public int countDown(int value) {
        while (value > 0) {
            if (value != 0) {
                value--;
            }
        }
        return value;
    }
}
