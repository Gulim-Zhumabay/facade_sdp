package airport.baggage;

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

public class BaggageRepository {
    private static final String SEPARATOR = ";";

    private final Path file;

    public BaggageRepository(Path file) {
        this.file = file;
    }

    public void save(BaggageItem item) {
        checkSafe(item.getOwnerRef());
        checkSafe(item.getContents());
        Map<String, BaggageItem> all = loadAll();
        all.put(item.getId(), item);
        writeAll(all);
    }

    public Optional<BaggageItem> findById(String id) {
        return Optional.ofNullable(loadAll().get(id));
    }

    public List<BaggageItem> findAll() {
        return new ArrayList<>(loadAll().values());
    }

    private Map<String, BaggageItem> loadAll() {
        Map<String, BaggageItem> result = new LinkedHashMap<>();
        if (!Files.exists(file)) {
            return result;
        }
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                if (line.isBlank()) {
                    continue;
                }
                String[] parts = line.split(SEPARATOR, -1);
                double weightKg = Double.parseDouble(parts[2]);
                BaggageStatus status = BaggageStatus.valueOf(parts[4]);
                BaggageItem item = new BaggageItem(parts[0], parts[1], weightKg, parts[3], status);
                result.put(item.getId(), item);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read " + file, e);
        }
        return result;
    }

    private void writeAll(Map<String, BaggageItem> all) {
        List<String> lines = new ArrayList<>();
        for (BaggageItem item : all.values()) {
            lines.add(String.join(SEPARATOR,
                    item.getId(),
                    item.getOwnerRef(),
                    String.valueOf(item.getWeightKg()),
                    item.getContents(),
                    item.getStatus().name()));
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