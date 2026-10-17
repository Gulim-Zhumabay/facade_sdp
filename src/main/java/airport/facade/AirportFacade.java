package airport.facade;

import airport.baggage.BaggageHandler;
import airport.baggage.BaggageItem;
import airport.baggage.BaggageStatus;
import airport.checkin.BoardingPass;
import airport.checkin.BoardingPassIssuer;
import airport.checkin.Kiosk;
import airport.gate.BoardingRecord;
import airport.gate.BoardingStatus;
import airport.gate.GateController;

import java.time.LocalDate;
import java.util.Optional;

public class AirportFacade {
    private final Kiosk kiosk;
    private final BoardingPassIssuer issuer;
    private final BaggageHandler baggage;
    private final GateController gate;
    private final TripRepository trips;
    private final TripCompensator compensator;

    public AirportFacade(Kiosk kiosk, BoardingPassIssuer issuer, BaggageHandler baggage,
                         GateController gate, TripRepository trips, TripCompensator compensator) {
        this.kiosk = kiosk;
        this.issuer = issuer;
        this.baggage = baggage;
        this.gate = gate;
        this.trips = trips;
        this.compensator = compensator;
    }

    public TripRecord travel(String passengerName, String flightNumber, double baggageWeightKg,
                             String baggageContents, String passportNumber, LocalDate passportExpiry,
                             int declaredCashUsd) {
        TripRecord trip = checkInWithBaggage(passengerName, flightNumber, baggageWeightKg, baggageContents);
        return passGate(trip.getId(), passportNumber, passportExpiry, declaredCashUsd);
    }

    public TripRecord checkInWithBaggage(String passengerName, String flightNumber, double baggageWeightKg,
                                         String baggageContents) {
        BoardingPass pass = issuePass(passengerName, flightNumber);
        TripRecord trip = TripRecord.createNew(pass.getId());
        trips.save(trip);
        Optional<String> problem = handleBaggage(trip, baggageWeightKg, baggageContents);
        if (problem.isPresent()) {
            compensator.unwind(trip, problem.get());
            throw new BaggageStepFailedException(trip.getId(), problem.get());
        }
        trip.baggageCleared();
        trips.save(trip);
        return trip;
    }

    public TripRecord passGate(String tripId, String passportNumber, LocalDate passportExpiry,
                               int declaredCashUsd) {
        TripRecord trip = trips.findById(tripId).orElseThrow(() -> new TripNotFoundException(tripId));
        if (trip.getStatus() != TripStatus.PENDING_C) {
            throw new TripStateException(tripId, "trip " + tripId + " is " + trip.getStatus()
                    + ", it is not waiting for the gate");
        }
        Optional<String> problem = handleGate(trip, passportNumber, passportExpiry, declaredCashUsd);
        if (problem.isPresent()) {
            compensator.unwind(trip, problem.get());
            throw new GateStepFailedException(trip.getId(), problem.get());
        }
        trip.completed();
        trips.save(trip);
        return trip;
    }

    private BoardingPass issuePass(String passengerName, String flightNumber) {
        try {
            BoardingPass pass = kiosk.startCheckIn(passengerName, flightNumber);
            return issuer.issue(pass);
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException(e.getMessage(), e);
        }
    }

    private Optional<String> handleBaggage(TripRecord trip, double weightKg, String contents) {
        try {
            BaggageItem item = baggage.accept(trip.getPassId(), weightKg, contents);
            trip.attachBaggage(item.getId());
            trips.save(trip);
            BaggageItem screened = baggage.screen(item.getId());
            if (screened.getStatus() == BaggageStatus.REJECTED) {
                return Optional.of("baggage rejected by security screening");
            }
            return Optional.empty();
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Optional.of(e.getMessage());
        }
    }

    private Optional<String> handleGate(TripRecord trip, String passportNumber, LocalDate passportExpiry,
                                        int declaredCashUsd) {
        try {
            BoardingRecord record = gate.board(trip.getPassId(), passportNumber, passportExpiry, declaredCashUsd);
            trip.attachBoarding(record.getId());
            trips.save(trip);
            if (record.getStatus() == BoardingStatus.DENIED) {
                return Optional.of(record.getNote());
            }
            return Optional.empty();
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Optional.of(e.getMessage());
        }
    }
}