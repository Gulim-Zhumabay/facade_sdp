package airport.gate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoardingRepositoryTest {
    private static final LocalDate FUTURE = LocalDate.of(2099, 1, 1);

    @TempDir
    Path dir;

    private BoardingRepository repositoryOnDisk() {
        return new BoardingRepository(dir.resolve("data").resolve("boarding.csv"));
    }

    @Test
    void emptyRepositoryFindsNothing() {
        BoardingRepository repository = repositoryOnDisk();

        assertTrue(repository.findAll().isEmpty());
        assertTrue(repository.findById("missing").isEmpty());
    }

    @Test
    void savedRecordSurvivesRestart() {
        BoardingRecord record = BoardingRecord.createNew("pass-1", "N1234567", FUTURE, 500);
        record.deny("passport is expired");
        repositoryOnDisk().save(record);

        BoardingRecord loaded = repositoryOnDisk().findById(record.getId()).orElseThrow();

        assertEquals("pass-1", loaded.getPassRef());
        assertEquals("N1234567", loaded.getPassportNumber());
        assertEquals(FUTURE, loaded.getPassportExpiry());
        assertEquals(500, loaded.getDeclaredCashUsd());
        assertEquals(BoardingStatus.DENIED, loaded.getStatus());
        assertEquals("passport is expired", loaded.getNote());
    }

    @Test
    void savingAgainUpdatesInsteadOfDuplicating() {
        BoardingRecord record = BoardingRecord.createNew("pass-1", "N1234567", FUTURE, 0);
        repositoryOnDisk().save(record);
        record.board();
        repositoryOnDisk().save(record);

        assertEquals(1, repositoryOnDisk().findAll().size());
        assertEquals(BoardingStatus.BOARDED, repositoryOnDisk().findById(record.getId()).orElseThrow().getStatus());
    }

    @Test
    void recordsKeepTheirOrder() {
        BoardingRecord first = BoardingRecord.createNew("pass-1", "N1234567", FUTURE, 0);
        BoardingRecord second = BoardingRecord.createNew("pass-2", "N7654321", FUTURE, 0);
        repositoryOnDisk().save(first);
        repositoryOnDisk().save(second);

        assertEquals(first.getId(), repositoryOnDisk().findAll().get(0).getId());
        assertEquals(second.getId(), repositoryOnDisk().findAll().get(1).getId());
    }

    @Test
    void valuesThatWouldBreakTheFileAreRejected() {
        BoardingRepository repository = repositoryOnDisk();
        BoardingRecord badRef = new BoardingRecord("id000001", "bad;ref", "N1234567", FUTURE, 0,
                BoardingStatus.PENDING, "note");
        BoardingRecord badPassport = new BoardingRecord("id000002", "pass-1", "bad\nnumber", FUTURE, 0,
                BoardingStatus.PENDING, "note");
        BoardingRecord noNote = new BoardingRecord("id000003", "pass-1", "N1234567", FUTURE, 0,
                BoardingStatus.PENDING, null);

        assertThrows(IllegalArgumentException.class, () -> repository.save(badRef));
        assertThrows(IllegalArgumentException.class, () -> repository.save(badPassport));
        assertThrows(IllegalArgumentException.class, () -> repository.save(noNote));
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    void unreadableStorageGivesClearError() throws IOException {
        Path file = dir.resolve("boarding.csv");
        Files.createDirectory(file);
        BoardingRepository repository = new BoardingRepository(file);

        assertThrows(UncheckedIOException.class, repository::findAll);
    }

    @Test
    void unwritableStorageGivesClearError() throws IOException {
        Path file = dir.resolve("boarding.csv");
        Files.createDirectory(file);
        BoardingRepository repository = new BoardingRepository(file);
        BoardingRecord record = BoardingRecord.createNew("pass-1", "N1234567", FUTURE, 0);

        assertThrows(UncheckedIOException.class, () -> repository.save(record));
    }
}