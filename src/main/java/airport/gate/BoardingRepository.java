package airport.gate;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class BoardingRepository {
    private static final String SEPARATOR = ";";

    private final Path file;

    public BoardingRepository(Path file) {
        this.file = file;
    }

    public void save(BoardingRecord record) {
        checkSafe(record.getPassRef());
        checkSafe(record.getPassportNumber());
        checkSafe(record.getNote());
        Map<String, BoardingRecord> all = loadAll();
        all.put(record.getId(), record);
        writeAll(all);
    }

    public Optional<BoardingRecord> findById(String id) {
        return Optional.ofNullable(loadAll().get(id));
    }

    public List<BoardingRecord> findAll() {
        return new ArrayList<>(loadAll().values());
    }

    private Map<String, BoardingRecord> loadAll() {
        Map<String, BoardingRecord> result = new LinkedHashMap<>();
        if (!Files.exists(file)) {
            return result;
        }
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                if (line.isBlank()) {
                    continue;
                }
                String[] parts = line.split(SEPARATOR, -1);
                LocalDate expiry = LocalDate.parse(parts[3]);
                int cash = Integer.parseInt(parts[4]);
                BoardingStatus status = BoardingStatus.valueOf(parts[5]);
                BoardingRecord record = new BoardingRecord(parts[0], parts[1], parts[2], expiry, cash, status, parts[6]);
                result.put(record.getId(), record);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read " + file, e);
        }
        return result;
    }

    private void writeAll(Map<String, BoardingRecord> all) {
        List<String> lines = new ArrayList<>();
        for (BoardingRecord record : all.values()) {
            lines.add(String.join(SEPARATOR,
                    record.getId(),
                    record.getPassRef(),
                    record.getPassportNumber(),
                    record.getPassportExpiry().toString(),
                    String.valueOf(record.getDeclaredCashUsd()),
                    record.getStatus().name(),
                    record.getNote()));
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