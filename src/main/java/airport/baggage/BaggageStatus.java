package airport.baggage;

public enum BaggageStatus {
    ACCEPTED,
    CLEARED,
    REJECTED,
    RELEASED;

    public boolean canMoveTo(BaggageStatus next) {
        return switch (this) {
            case ACCEPTED -> next == CLEARED || next == REJECTED || next == RELEASED;
            case CLEARED -> next == RELEASED;
            case REJECTED, RELEASED -> false;
        };
    }
}