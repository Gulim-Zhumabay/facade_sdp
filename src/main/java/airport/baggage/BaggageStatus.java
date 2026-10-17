package airport.baggage;

public enum BaggageStatus {
    ACCEPTED,
    CLEARED,
    REJECTED,
    RELEASED,
    RETURNED;

    public boolean canMoveTo(BaggageStatus next) {
        return switch (this) {
            case ACCEPTED -> next == CLEARED || next == REJECTED || next == RELEASED || next == RETURNED;
            case CLEARED -> next == RELEASED || next == RETURNED;
            case REJECTED, RELEASED, RETURNED -> false;
        };
    }
}