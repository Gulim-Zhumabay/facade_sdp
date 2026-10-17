package airport.facade;

import airport.baggage.BaggageHandler;
import airport.baggage.BaggageRepository;
import airport.baggage.BaggageStatus;
import airport.checkin.BoardingPassIssuer;
import airport.checkin.BoardingPassRepository;
import airport.checkin.PassStatus;

public class TripCompensator {
    private final BoardingPassIssuer issuer;
    private final BoardingPassRepository passes;
    private final BaggageHandler baggage;
    private final BaggageRepository baggageItems;
    private final TripRepository trips;

    public TripCompensator(BoardingPassIssuer issuer, BoardingPassRepository passes,
                           BaggageHandler baggage, BaggageRepository baggageItems, TripRepository trips) {
        this.issuer = issuer;
        this.passes = passes;
        this.baggage = baggage;
        this.baggageItems = baggageItems;
        this.trips = trips;
    }

    public void unwind(TripRecord trip, String reason) {
        returnBaggage(trip);
        cancelPass(trip);
        trip.compensated(reason);
        trips.save(trip);
    }

    private void returnBaggage(TripRecord trip) {
        if (trip.getBaggageId() == null) {
            return;
        }
        baggageItems.findById(trip.getBaggageId())
                .filter(item -> item.getStatus().canMoveTo(BaggageStatus.RETURNED))
                .ifPresent(item -> baggage.returnToOwner(item.getId()));
    }

    private void cancelPass(TripRecord trip) {
        passes.findById(trip.getPassId())
                .filter(pass -> pass.getStatus().canMoveTo(PassStatus.CANCELLED))
                .ifPresent(pass -> issuer.cancel(pass.getId()));
    }
}