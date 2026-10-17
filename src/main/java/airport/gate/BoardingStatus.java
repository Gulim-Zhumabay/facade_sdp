package airport.gate;

public enum BoardingStatus {
    PENDING,
    BOARDED,
    DENIED,
    OFFLOADED;

    public boolean canMoveTo(BoardingStatus next) {
        return switch (this) {
            case PENDING -> next == BOARDED || next == DENIED;
            case BOARDED -> next == OFFLOADED;
            case DENIED, OFFLOADED -> false;
        };
    }
}