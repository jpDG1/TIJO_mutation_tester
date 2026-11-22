package mutationtester.integration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

final class SampleSources {

    private SampleSources() {
    }

    static String read(String filename) throws IOException {
        try (InputStream stream = SampleSources.class.getResourceAsStream("/samples/" + filename)) {
            if (stream == null) {
                throw new IOException("Missing sample file: " + filename);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
