package airport.baggage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BaggageRepositoryTest {
    @TempDir
    Path dir;

    private BaggageRepository repositoryOnDisk() {
        return new BaggageRepository(dir.resolve("data").resolve("baggage.csv"));
    }

    @Test
    void emptyRepositoryFindsNothing() {
        BaggageRepository repository = repositoryOnDisk();

        assertTrue(repository.findAll().isEmpty());
        assertTrue(repository.findById("missing").isEmpty());
    }

    @Test
    void savedItemSurvivesRestart() {
        BaggageItem item = BaggageItem.createNew("owner-1", 12.5, "одежда, книги");
        item.clear();
        repositoryOnDisk().save(item);

        BaggageItem loaded = repositoryOnDisk().findById(item.getId()).orElseThrow();

        assertEquals("owner-1", loaded.getOwnerRef());
        assertEquals(12.5, loaded.getWeightKg());
        assertEquals("одежда, книги", loaded.getContents());
        assertEquals(BaggageStatus.CLEARED, loaded.getStatus());
    }

    @Test
    void savingAgainUpdatesInsteadOfDuplicating() {
        BaggageItem item = BaggageItem.createNew("owner-1", 5, "clothes");
        repositoryOnDisk().save(item);
        item.returnToOwner();
        repositoryOnDisk().save(item);

        assertEquals(1, repositoryOnDisk().findAll().size());
        assertEquals(BaggageStatus.RETURNED, repositoryOnDisk().findById(item.getId()).orElseThrow().getStatus());
    }

    @Test
    void itemsKeepTheirOrder() {
        BaggageItem first = BaggageItem.createNew("owner-1", 5, "first");
        BaggageItem second = BaggageItem.createNew("owner-2", 6, "second");
        repositoryOnDisk().save(first);
        repositoryOnDisk().save(second);

        assertEquals(first.getId(), repositoryOnDisk().findAll().get(0).getId());
        assertEquals(second.getId(), repositoryOnDisk().findAll().get(1).getId());
    }

    @Test
    void valuesThatWouldBreakTheFileAreRejected() {
        BaggageRepository repository = repositoryOnDisk();
        BaggageItem semicolon = new BaggageItem("id000001", "owner-1", 5, "bad;contents", BaggageStatus.ACCEPTED);
        BaggageItem lineBreak = new BaggageItem("id000002", "owner-1", 5, "bad\ncontents", BaggageStatus.ACCEPTED);
        BaggageItem nothing = new BaggageItem("id000003", null, 5, "clothes", BaggageStatus.ACCEPTED);

        assertThrows(IllegalArgumentException.class, () -> repository.save(semicolon));
        assertThrows(IllegalArgumentException.class, () -> repository.save(lineBreak));
        assertThrows(IllegalArgumentException.class, () -> repository.save(nothing));
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    void unreadableStorageGivesClearError() throws IOException {
        Path file = dir.resolve("baggage.csv");
        Files.createDirectory(file);
        BaggageRepository repository = new BaggageRepository(file);

        assertThrows(UncheckedIOException.class, repository::findAll);
    }

    @Test
    void unwritableStorageGivesClearError() throws IOException {
        Path file = dir.resolve("baggage.csv");
        Files.createDirectory(file);
        BaggageRepository repository = new BaggageRepository(file);
        BaggageItem item = BaggageItem.createNew("owner-1", 5, "clothes");

        assertThrows(UncheckedIOException.class, () -> repository.save(item));
    }
}