package airport.facade;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TripRepositoryTest {
    @TempDir
    Path dir;

    private TripRepository repositoryOnDisk() {
        return new TripRepository(dir.resolve("data").resolve("trips.csv"));
    }

    @Test
    void emptyRepositoryFindsNothing() {
        TripRepository repository = repositoryOnDisk();

        assertTrue(repository.findAll().isEmpty());
        assertTrue(repository.findById("missing").isEmpty());
    }

    @Test
    void tripWithoutBaggageAndGateSurvivesRestart() {
        TripRecord trip = TripRecord.createNew("pass-1");
        repositoryOnDisk().save(trip);

        TripRecord loaded = repositoryOnDisk().findById(trip.getId()).orElseThrow();

        assertEquals("pass-1", loaded.getPassId());
        assertNull(loaded.getBaggageId());
        assertNull(loaded.getBoardingId());
        assertEquals(TripStatus.PENDING_B, loaded.getStatus());
    }

    @Test
    void fullTripSurvivesRestart() {
        TripRecord trip = TripRecord.createNew("pass-1");
        trip.attachBaggage("bag-1");
        trip.baggageCleared();
        trip.attachBoarding("gate-1");
        trip.completed();
        repositoryOnDisk().save(trip);

        TripRecord loaded = repositoryOnDisk().findById(trip.getId()).orElseThrow();

        assertEquals("bag-1", loaded.getBaggageId());
        assertEquals("gate-1", loaded.getBoardingId());
        assertEquals(TripStatus.COMPLETED, loaded.getStatus());
        assertEquals("passenger boarded", loaded.getNote());
    }

    @Test
    void savingAgainUpdatesInsteadOfDuplicating() {
        TripRecord trip = TripRecord.createNew("pass-1");
        repositoryOnDisk().save(trip);
        trip.compensated("reason");
        repositoryOnDisk().save(trip);

        assertEquals(1, repositoryOnDisk().findAll().size());
        assertEquals(TripStatus.COMPENSATED, repositoryOnDisk().findById(trip.getId()).orElseThrow().getStatus());
    }

    @Test
    void tripsKeepTheirOrder() {
        TripRecord first = TripRecord.createNew("pass-1");
        TripRecord second = TripRecord.createNew("pass-2");
        repositoryOnDisk().save(first);
        repositoryOnDisk().save(second);

        assertEquals(first.getId(), repositoryOnDisk().findAll().get(0).getId());
        assertEquals(second.getId(), repositoryOnDisk().findAll().get(1).getId());
    }

    @Test
    void unreadableStorageGivesClearError() throws IOException {
        Path file = dir.resolve("trips.csv");
        Files.createDirectory(file);
        TripRepository repository = new TripRepository(file);

        assertThrows(UncheckedIOException.class, repository::findAll);
    }

    @Test
    void unwritableStorageGivesClearError() throws IOException {
        Path file = dir.resolve("trips.csv");
        Files.createDirectory(file);
        TripRepository repository = new TripRepository(file);
        TripRecord trip = TripRecord.createNew("pass-1");

        assertThrows(UncheckedIOException.class, () -> repository.save(trip));
    }
}