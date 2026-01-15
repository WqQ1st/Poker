package data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.nio.file.Files;
import java.nio.file.Path;

public class SaveLoad {
    private final ObjectMapper mapper;

    public SaveLoad() {
        mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public void save(Path path, GameState state) throws Exception {
        Files.createDirectories(path.getParent());
        mapper.writeValue(path.toFile(), state);
    }

    public GameState load(Path path) throws Exception {
        return mapper.readValue(path.toFile(), GameState.class);
    }
}
