package airport.checkin;

import java.util.UUID;

public class BoardingPass {
    private final String id;
    private final String passengerName;
    private final String flightNumber;
    private PassStatus status;

    public BoardingPass(String id, String passengerName, String flightNumber, PassStatus status) {
        this.id = id;
        this.passengerName = passengerName;
        this.flightNumber = flightNumber;
        this.status = status;
    }

    public static BoardingPass createNew(String passengerName, String flightNumber) {
        String id = UUID.randomUUID().toString().substring(0, 8);
        return new BoardingPass(id, passengerName, flightNumber, PassStatus.PENDING);
    }

    public String getId() {
        return id;
    }

    public String getPassengerName() {
        return passengerName;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public PassStatus getStatus() {
        return status;
    }

    public void issue() {
        moveTo(PassStatus.ISSUED);
    }

    public void cancel() {
        moveTo(PassStatus.CANCELLED);
    }

    private void moveTo(PassStatus next) {
        if (!status.canMoveTo(next)) {
            throw new IllegalStateException("cannot move boarding pass from " + status + " to " + next);
        }
        status = next;
    }
}