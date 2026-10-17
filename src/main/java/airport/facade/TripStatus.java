package airport.facade;

public enum TripStatus {
    PENDING_B,
    PENDING_C,
    COMPLETED,
    COMPENSATED;

    public boolean canMoveTo(TripStatus next) {
        return switch (this) {
            case PENDING_B -> next == PENDING_C || next == COMPENSATED;
            case PENDING_C -> next == COMPLETED || next == COMPENSATED;
            case COMPLETED, COMPENSATED -> false;
        };
    }

    public boolean isPending() {
        return this == PENDING_B || this == PENDING_C;
    }
}