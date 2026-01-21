package poker.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public class LogWriter implements AutoCloseable {
    private final ObjectMapper mapper;
    private final BufferedWriter out;

    public LogWriter(Path file) throws IOException {
        Files.createDirectories(file.getParent());

        this.mapper = new ObjectMapper();
        // single line per event
        mapper.disable(SerializationFeature.INDENT_OUTPUT);

        this.out = Files.newBufferedWriter(
                file,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.APPEND
        );

    }

    public synchronized void append(Object event) {
        try {
            out.write(mapper.writeValueAsString(event));
            out.newLine();
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException("Failed to append log event", e);
        }
    }

    @Override
    public void close() throws IOException {
        out.close();
    }
}
