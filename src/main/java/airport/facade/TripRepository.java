package airport.facade;

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

public class TripRepository {
    private static final String SEPARATOR = ";";

    private final Path file;

    public TripRepository(Path file) {
        this.file = file;
    }

    public void save(TripRecord trip) {
        Map<String, TripRecord> all = loadAll();
        all.put(trip.getId(), trip);
        writeAll(all);
    }

    public Optional<TripRecord> findById(String id) {
        return Optional.ofNullable(loadAll().get(id));
    }

    public List<TripRecord> findAll() {
        return new ArrayList<>(loadAll().values());
    }

    private Map<String, TripRecord> loadAll() {
        Map<String, TripRecord> result = new LinkedHashMap<>();
        if (!Files.exists(file)) {
            return result;
        }
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                if (line.isBlank()) {
                    continue;
                }
                String[] parts = line.split(SEPARATOR, -1);
                TripRecord trip = new TripRecord(parts[0], parts[1], emptyToNull(parts[2]), emptyToNull(parts[3]),
                        TripStatus.valueOf(parts[4]), parts[5]);
                result.put(trip.getId(), trip);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read " + file, e);
        }
        return result;
    }

    private void writeAll(Map<String, TripRecord> all) {
        List<String> lines = new ArrayList<>();
        for (TripRecord trip : all.values()) {
            lines.add(String.join(SEPARATOR,
                    trip.getId(),
                    trip.getPassId(),
                    nullToEmpty(trip.getBaggageId()),
                    nullToEmpty(trip.getBoardingId()),
                    trip.getStatus().name(),
                    trip.getNote()));
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

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String emptyToNull(String value) {
        return value.isEmpty() ? null : value;
    }
}