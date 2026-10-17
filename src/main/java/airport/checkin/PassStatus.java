package airport.checkin;

public enum PassStatus {
    PENDING,
    ISSUED,
    CANCELLED;

    public boolean canMoveTo(PassStatus next) {
        return switch (this) {
            case PENDING -> next == ISSUED || next == CANCELLED;
            case ISSUED -> next == CANCELLED;
            case CANCELLED -> false;
        };
    }
}