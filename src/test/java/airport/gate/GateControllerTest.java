package airport.gate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GateControllerTest {
    private static final LocalDate FUTURE = LocalDate.of(2099, 1, 1);

    @TempDir
    Path dir;

    private BoardingRepository repositoryOnDisk() {
        return new BoardingRepository(dir.resolve("boarding.csv"));
    }

    private GateController gate() {
        return new GateController(repositoryOnDisk(), new BorderControl());
    }

    private void assertSavedStatus(String recordId, BoardingStatus expected) {
        BoardingRecord saved = repositoryOnDisk().findById(recordId).orElseThrow();
        assertEquals(expected, saved.getStatus());
    }

    @Test
    void goodPassengerBoardsAndIsSaved() {
        BoardingRecord record = gate().board("pass-1", "N1234567", FUTURE, 500);

        assertEquals(BoardingStatus.BOARDED, record.getStatus());
        assertSavedStatus(record.getId(), BoardingStatus.BOARDED);
    }

    @Test
    void expiredPassportIsDeniedAndSaved() {
        BoardingRecord record = gate().board("pass-1", "N1234567", LocalDate.of(2020, 1, 1), 0);

        assertEquals(BoardingStatus.DENIED, record.getStatus());
        assertEquals("passport is expired", record.getNote());
        assertSavedStatus(record.getId(), BoardingStatus.DENIED);
    }

    @Test
    void tooMuchCashIsDeniedAndSaved() {
        BoardingRecord record = gate().board("pass-1", "N1234567", FUTURE, 50000);

        assertEquals(BoardingStatus.DENIED, record.getStatus());
        assertSavedStatus(record.getId(), BoardingStatus.DENIED);
    }

    @Test
    void badPassReferenceIsRejectedAndNothingIsSaved() {
        assertThrows(IllegalArgumentException.class, () -> gate().board(null, "N1234567", FUTURE, 0));
        assertThrows(IllegalArgumentException.class, () -> gate().board("", "N1234567", FUTURE, 0));
        assertThrows(IllegalArgumentException.class, () -> gate().board("bad;ref", "N1234567", FUTURE, 0));
        assertThrows(IllegalArgumentException.class, () -> gate().board("a".repeat(37), "N1234567", FUTURE, 0));
        assertTrue(repositoryOnDisk().findAll().isEmpty());
    }

    @Test
    void badPassportNumberIsRejectedAndNothingIsSaved() {
        assertThrows(IllegalArgumentException.class, () -> gate().board("pass-1", null, FUTURE, 0));
        assertThrows(IllegalArgumentException.class, () -> gate().board("pass-1", "12345", FUTURE, 0));
        assertThrows(IllegalArgumentException.class, () -> gate().board("pass-1", "abcdefg", FUTURE, 0));
        assertThrows(IllegalArgumentException.class, () -> gate().board("pass-1", "N123456789", FUTURE, 0));
        assertTrue(repositoryOnDisk().findAll().isEmpty());
    }

    @Test
    void missingExpiryAndNegativeCashAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> gate().board("pass-1", "N1234567", null, 0));
        assertThrows(IllegalArgumentException.class, () -> gate().board("pass-1", "N1234567", FUTURE, -1));
        assertTrue(repositoryOnDisk().findAll().isEmpty());
    }

    @Test
    void boardedPassengerCanBeOffloaded() {
        BoardingRecord record = gate().board("pass-1", "N1234567", FUTURE, 0);

        BoardingRecord offloaded = gate().offload(record.getId());

        assertEquals(BoardingStatus.OFFLOADED, offloaded.getStatus());
        assertSavedStatus(record.getId(), BoardingStatus.OFFLOADED);
    }

    @Test
    void deniedPassengerCannotBeOffloaded() {
        BoardingRecord record = gate().board("pass-1", "N1234567", LocalDate.of(2020, 1, 1), 0);

        assertThrows(IllegalStateException.class, () -> gate().offload(record.getId()));
        assertSavedStatus(record.getId(), BoardingStatus.DENIED);
    }

    @Test
    void offloadingUnknownRecordFails() {
        assertThrows(IllegalArgumentException.class, () -> gate().offload("missing"));
    }
}