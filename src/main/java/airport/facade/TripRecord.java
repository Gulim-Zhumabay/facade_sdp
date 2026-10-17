package airport.facade;

import java.util.UUID;

public class TripRecord {
    private final String id;
    private final String passId;
    private String baggageId;
    private String boardingId;
    private TripStatus status;
    private String note;

    public TripRecord(String id, String passId, String baggageId, String boardingId, TripStatus status, String note) {
        this.id = id;
        this.passId = passId;
        this.baggageId = baggageId;
        this.boardingId = boardingId;
        this.status = status;
        this.note = clean(note);
    }

    public static TripRecord createNew(String passId) {
        String id = UUID.randomUUID().toString().substring(0, 8);
        return new TripRecord(id, passId, null, null, TripStatus.PENDING_B, "pass issued, waiting for baggage");
    }

    public String getId() {
        return id;
    }

    public String getPassId() {
        return passId;
    }

    public String getBaggageId() {
        return baggageId;
    }

    public String getBoardingId() {
        return boardingId;
    }

    public TripStatus getStatus() {
        return status;
    }

    public String getNote() {
        return note;
    }

    public void attachBaggage(String baggageId) {
        this.baggageId = baggageId;
    }

    public void attachBoarding(String boardingId) {
        this.boardingId = boardingId;
    }

    public void baggageCleared() {
        moveTo(TripStatus.PENDING_C);
        note = "baggage cleared, waiting for gate";
    }

    public void completed() {
        moveTo(TripStatus.COMPLETED);
        note = "passenger boarded";
    }

    public void compensated(String reason) {
        moveTo(TripStatus.COMPENSATED);
        note = clean("undone: " + reason);
    }

    private void moveTo(TripStatus next) {
        if (!status.canMoveTo(next)) {
            throw new IllegalStateException("cannot move trip from " + status + " to " + next);
        }
        status = next;
    }

    private static String clean(String text) {
        if (text == null) {
            return "";
        }
        return text.replace(';', ',').replace('\r', ' ').replace('\n', ' ');
    }
}