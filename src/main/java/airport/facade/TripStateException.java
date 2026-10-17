package airport.facade;

public class TripStateException extends TripChainException {
    public TripStateException(String tripId, String message) {
        super(tripId, message);
    }
}