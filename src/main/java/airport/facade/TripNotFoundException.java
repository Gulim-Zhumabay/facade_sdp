package airport.facade;

public class TripNotFoundException extends TripChainException {
    public TripNotFoundException(String tripId) {
        super(tripId, "trip not found: " + tripId);
    }
}