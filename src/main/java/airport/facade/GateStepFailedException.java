package airport.facade;

public class GateStepFailedException extends TripChainException {
    public GateStepFailedException(String tripId, String reason) {
        super(tripId, "gate step failed, baggage returned and boarding pass cancelled: " + reason);
    }
}