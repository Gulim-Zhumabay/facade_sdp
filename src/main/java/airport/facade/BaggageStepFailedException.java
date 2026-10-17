package airport.facade;

public class BaggageStepFailedException extends TripChainException {
    public BaggageStepFailedException(String tripId, String reason) {
        super(tripId, "baggage step failed, boarding pass cancelled: " + reason);
    }
}