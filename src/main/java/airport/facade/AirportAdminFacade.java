package airport.facade;

import airport.baggage.BaggageItem;
import airport.baggage.BaggageRepository;
import airport.checkin.BoardingPassRepository;
import airport.gate.BoardingRecord;
import airport.gate.BoardingRepository;
import airport.gate.BoardingStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AirportAdminFacade {
    private final TripRepository trips;
    private final BoardingPassRepository passes;
    private final BaggageRepository baggageItems;
    private final BoardingRepository boardings;
    private final TripCompensator compensator;

    public AirportAdminFacade(TripRepository trips, BoardingPassRepository passes, BaggageRepository baggageItems,
                              BoardingRepository boardings, TripCompensator compensator) {
        this.trips = trips;
        this.passes = passes;
        this.baggageItems = baggageItems;
        this.boardings = boardings;
        this.compensator = compensator;
    }

    public List<TripRecord> listTrips() {
        return trips.findAll();
    }

    public TripReport inspect(String tripId) {
        TripRecord trip = find(tripId);
        Optional<BaggageItem> baggage = trip.getBaggageId() == null
                ? Optional.empty()
                : baggageItems.findById(trip.getBaggageId());
        return new TripReport(trip, passes.findById(trip.getPassId()), baggage, boardingFor(trip));
    }

    public TripRecord compensate(String tripId) {
        TripRecord trip = find(tripId);
        if (!trip.getStatus().isPending()) {
            throw new TripStateException(tripId, "trip " + tripId + " is " + trip.getStatus() + ", nothing to undo");
        }
        Optional<BoardingRecord> boarding = boardingFor(trip);
        if (boarding.isPresent() && boarding.get().getStatus() == BoardingStatus.BOARDED) {
            throw new TripStateException(tripId, "passenger of trip " + tripId + " already boarded, run recovery instead");
        }
        compensator.unwind(trip, "undone by administrator");
        return trip;
    }

    public List<TripRecord> recover() {
        List<TripRecord> changed = new ArrayList<>();
        for (TripRecord trip : trips.findAll()) {
            if (recoverOne(trip)) {
                changed.add(trip);
            }
        }
        return changed;
    }

    private boolean recoverOne(TripRecord trip) {
        if (trip.getStatus() == TripStatus.PENDING_B) {
            compensator.unwind(trip, "interrupted before baggage was cleared");
            return true;
        }
        if (trip.getStatus() != TripStatus.PENDING_C) {
            return false;
        }
        Optional<BoardingRecord> boarding = boardingFor(trip);
        if (boarding.isEmpty()) {
            return false;
        }
        if (boarding.get().getStatus() == BoardingStatus.BOARDED) {
            trip.attachBoarding(boarding.get().getId());
            trip.completed();
            trips.save(trip);
            return true;
        }
        compensator.unwind(trip, boarding.get().getNote());
        return true;
    }

    private TripRecord find(String tripId) {
        return trips.findById(tripId).orElseThrow(() -> new TripNotFoundException(tripId));
    }

    private Optional<BoardingRecord> boardingFor(TripRecord trip) {
        List<BoardingRecord> mine = boardings.findAll().stream()
                .filter(record -> record.getPassRef().equals(trip.getPassId()))
                .toList();
        Optional<BoardingRecord> boarded = mine.stream()
                .filter(record -> record.getStatus() == BoardingStatus.BOARDED)
                .findFirst();
        if (boarded.isPresent()) {
            return boarded;
        }
        return mine.isEmpty() ? Optional.empty() : Optional.of(mine.get(mine.size() - 1));
    }
}