package airport.facade;

public class TripChainException extends RuntimeException {
    private final String tripId;

    public TripChainException(String message) {
        this(null, message, null);
    }

    public TripChainException(String tripId, String message) {
        this(tripId, message, null);
    }

    public TripChainException(String tripId, String message, Throwable cause) {
        super(message, cause);
        this.tripId = tripId;
    }

    public String getTripId() {
        return tripId;
    }
}