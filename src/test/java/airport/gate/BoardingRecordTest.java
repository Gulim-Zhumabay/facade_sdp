package airport.gate;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BoardingRecordTest {
    private static final LocalDate FUTURE = LocalDate.of(2099, 1, 1);

    private BoardingRecord newRecord() {
        return BoardingRecord.createNew("pass-1", "N1234567", FUTURE, 500);
    }

    @Test
    void newRecordStartsAsPending() {
        BoardingRecord record = newRecord();

        assertEquals(BoardingStatus.PENDING, record.getStatus());
        assertEquals(8, record.getId().length());
        assertEquals("pass-1", record.getPassRef());
        assertEquals("N1234567", record.getPassportNumber());
        assertEquals(FUTURE, record.getPassportExpiry());
        assertEquals(500, record.getDeclaredCashUsd());
        assertEquals("waiting for checks", record.getNote());
    }

    @Test
    void pendingRecordCanBoard() {
        BoardingRecord record = newRecord();

        record.board();

        assertEquals(BoardingStatus.BOARDED, record.getStatus());
        assertEquals("all checks passed", record.getNote());
    }

    @Test
    void pendingRecordCanBeDeniedWithReason() {
        BoardingRecord record = newRecord();

        record.deny("passport is expired");

        assertEquals(BoardingStatus.DENIED, record.getStatus());
        assertEquals("passport is expired", record.getNote());
    }

    @Test
    void boardedRecordCanBeOffloaded() {
        BoardingRecord record = newRecord();
        record.board();

        record.offload();

        assertEquals(BoardingStatus.OFFLOADED, record.getStatus());
        assertEquals("passenger offloaded", record.getNote());
    }

    @Test
    void pendingRecordCannotBeOffloaded() {
        BoardingRecord record = newRecord();

        assertThrows(IllegalStateException.class, record::offload);
        assertEquals(BoardingStatus.PENDING, record.getStatus());
    }

    @Test
    void deniedRecordCannotMoveAnywhere() {
        BoardingRecord record = newRecord();
        record.deny("passport is expired");

        assertThrows(IllegalStateException.class, record::board);
        assertThrows(IllegalStateException.class, record::offload);
        assertEquals(BoardingStatus.DENIED, record.getStatus());
    }

    @Test
    void boardedRecordCannotBeDeniedOrBoardedAgain() {
        BoardingRecord record = newRecord();
        record.board();

        assertThrows(IllegalStateException.class, () -> record.deny("late"));
        assertThrows(IllegalStateException.class, record::board);
        assertEquals(BoardingStatus.BOARDED, record.getStatus());
    }

    @Test
    void offloadedRecordCannotMoveAnywhere() {
        BoardingRecord record = newRecord();
        record.board();
        record.offload();

        assertThrows(IllegalStateException.class, record::offload);
    }
}