package airport.gate;

import java.time.LocalDate;
import java.util.UUID;

public class BoardingRecord {
    private final String id;
    private final String passRef;
    private final String passportNumber;
    private final LocalDate passportExpiry;
    private final int declaredCashUsd;
    private BoardingStatus status;
    private String note;

    public BoardingRecord(String id, String passRef, String passportNumber, LocalDate passportExpiry,
                          int declaredCashUsd, BoardingStatus status, String note) {
        this.id = id;
        this.passRef = passRef;
        this.passportNumber = passportNumber;
        this.passportExpiry = passportExpiry;
        this.declaredCashUsd = declaredCashUsd;
        this.status = status;
        this.note = note;
    }

    public static BoardingRecord createNew(String passRef, String passportNumber, LocalDate passportExpiry,
                                           int declaredCashUsd) {
        String id = UUID.randomUUID().toString().substring(0, 8);
        return new BoardingRecord(id, passRef, passportNumber, passportExpiry, declaredCashUsd,
                BoardingStatus.PENDING, "waiting for checks");
    }

    public String getId() {
        return id;
    }

    public String getPassRef() {
        return passRef;
    }

    public String getPassportNumber() {
        return passportNumber;
    }

    public LocalDate getPassportExpiry() {
        return passportExpiry;
    }

    public int getDeclaredCashUsd() {
        return declaredCashUsd;
    }

    public BoardingStatus getStatus() {
        return status;
    }

    public String getNote() {
        return note;
    }

    public void board() {
        moveTo(BoardingStatus.BOARDED);
        note = "all checks passed";
    }

    public void deny(String reason) {
        moveTo(BoardingStatus.DENIED);
        note = reason;
    }

    public void offload() {
        moveTo(BoardingStatus.OFFLOADED);
        note = "passenger offloaded";
    }

    private void moveTo(BoardingStatus next) {
        if (!status.canMoveTo(next)) {
            throw new IllegalStateException("cannot move boarding record from " + status + " to " + next);
        }
        status = next;
    }
}