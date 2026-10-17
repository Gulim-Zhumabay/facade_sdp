package airport.checkin;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class BoardingPassRepository {
    private static final String SEPARATOR = ";";

    private final Path file;

    public BoardingPassRepository(Path file) {
        this.file = file;
    }

    public void save(BoardingPass pass) {
        checkSafe(pass.getPassengerName());
        checkSafe(pass.getFlightNumber());
        Map<String, BoardingPass> all = loadAll();
        all.put(pass.getId(), pass);
        writeAll(all);
    }

    public Optional<BoardingPass> findById(String id) {
        return Optional.ofNullable(loadAll().get(id));
    }

    public List<BoardingPass> findAll() {
        return new ArrayList<>(loadAll().values());
    }

    private Map<String, BoardingPass> loadAll() {
        Map<String, BoardingPass> result = new LinkedHashMap<>();
        if (!Files.exists(file)) {
            return result;
        }
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                if (line.isBlank()) {
                    continue;
                }
                String[] parts = line.split(SEPARATOR, -1);
                PassStatus status = PassStatus.valueOf(parts[3]);
                BoardingPass pass = new BoardingPass(parts[0], parts[1], parts[2], status);
                result.put(pass.getId(), pass);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read " + file, e);
        }
        return result;
    }

    private void writeAll(Map<String, BoardingPass> all) {
        List<String> lines = new ArrayList<>();
        for (BoardingPass pass : all.values()) {
            lines.add(String.join(SEPARATOR,
                    pass.getId(), pass.getPassengerName(), pass.getFlightNumber(), pass.getStatus().name()));
        }
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(file, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot write " + file, e);
        }
    }

    private void checkSafe(String value) {
        if (value == null || value.contains(SEPARATOR) || value.contains("\n") || value.contains("\r")) {
            throw new IllegalArgumentException("value must not be null or contain ';' or line breaks");
        }
    }
}