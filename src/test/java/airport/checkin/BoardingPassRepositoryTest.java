package airport.checkin;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoardingPassRepositoryTest {
    @TempDir
    Path dir;

    private BoardingPassRepository repositoryOnDisk() {
        return new BoardingPassRepository(dir.resolve("data").resolve("passes.csv"));
    }

    @Test
    void emptyRepositoryFindsNothing() {
        BoardingPassRepository repository = repositoryOnDisk();

        assertTrue(repository.findAll().isEmpty());
        assertTrue(repository.findById("missing").isEmpty());
    }

    @Test
    void savedPassSurvivesRestart() {
        BoardingPass pass = BoardingPass.createNew("Айгерим", "KC101");
        pass.issue();
        repositoryOnDisk().save(pass);

        BoardingPass loaded = repositoryOnDisk().findById(pass.getId()).orElseThrow();

        assertEquals("Айгерим", loaded.getPassengerName());
        assertEquals("KC101", loaded.getFlightNumber());
        assertEquals(PassStatus.ISSUED, loaded.getStatus());
    }

    @Test
    void savingAgainUpdatesInsteadOfDuplicating() {
        BoardingPass pass = BoardingPass.createNew("Aigerim", "KC101");
        repositoryOnDisk().save(pass);
        pass.cancel();
        repositoryOnDisk().save(pass);

        assertEquals(1, repositoryOnDisk().findAll().size());
        assertEquals(PassStatus.CANCELLED, repositoryOnDisk().findById(pass.getId()).orElseThrow().getStatus());
    }

    @Test
    void passesKeepTheirOrder() {
        BoardingPass first = BoardingPass.createNew("First", "KC101");
        BoardingPass second = BoardingPass.createNew("Second", "KC102");
        repositoryOnDisk().save(first);
        repositoryOnDisk().save(second);

        assertEquals(first.getId(), repositoryOnDisk().findAll().get(0).getId());
        assertEquals(second.getId(), repositoryOnDisk().findAll().get(1).getId());
    }

    @Test
    void valuesThatWouldBreakTheFileAreRejected() {
        BoardingPassRepository repository = repositoryOnDisk();
        BoardingPass semicolon = new BoardingPass("id000001", "bad;name", "KC101", PassStatus.PENDING);
        BoardingPass lineBreak = new BoardingPass("id000002", "bad\nname", "KC101", PassStatus.PENDING);
        BoardingPass nothing = new BoardingPass("id000003", null, "KC101", PassStatus.PENDING);

        assertThrows(IllegalArgumentException.class, () -> repository.save(semicolon));
        assertThrows(IllegalArgumentException.class, () -> repository.save(lineBreak));
        assertThrows(IllegalArgumentException.class, () -> repository.save(nothing));
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    void unreadableStorageGivesClearError() throws IOException {
        Path file = dir.resolve("passes.csv");
        Files.createDirectory(file);
        BoardingPassRepository repository = new BoardingPassRepository(file);

        assertThrows(UncheckedIOException.class, repository::findAll);
    }

    @Test
    void unwritableStorageGivesClearError() throws IOException {
        Path file = dir.resolve("passes.csv");
        Files.createDirectory(file);
        BoardingPassRepository repository = new BoardingPassRepository(file);
        BoardingPass pass = BoardingPass.createNew("Aigerim", "KC101");

        assertThrows(UncheckedIOException.class, () -> repository.save(pass));
    }
}