package airport.facade;

import airport.baggage.BaggageHandler;
import airport.baggage.BaggageRepository;
import airport.baggage.SecurityScreening;
import airport.checkin.BoardingPassIssuer;
import airport.checkin.BoardingPassRepository;
import airport.checkin.Kiosk;
import airport.gate.BoardingRepository;
import airport.gate.BorderControl;
import airport.gate.GateController;

import java.nio.file.Path;

public class AirportSystem {
    private final AirportFacade passengers;
    private final AirportAdminFacade admin;

    private AirportSystem(AirportFacade passengers, AirportAdminFacade admin) {
        this.passengers = passengers;
        this.admin = admin;
    }

    public static AirportSystem openAt(Path dir) {
        BoardingPassRepository passes = new BoardingPassRepository(dir.resolve("boarding_passes.csv"));
        BoardingPassIssuer issuer = new BoardingPassIssuer(passes);
        BaggageRepository baggageItems = new BaggageRepository(dir.resolve("baggage.csv"));
        BaggageHandler baggage = new BaggageHandler(baggageItems, new SecurityScreening());
        BoardingRepository boardings = new BoardingRepository(dir.resolve("boarding.csv"));
        GateController gate = new GateController(boardings, new BorderControl());
        TripRepository trips = new TripRepository(dir.resolve("trips.csv"));
        TripCompensator compensator = new TripCompensator(issuer, passes, baggage, baggageItems, trips);
        AirportFacade passengers = new AirportFacade(new Kiosk(), issuer, baggage, gate, trips, compensator);
        AirportAdminFacade admin = new AirportAdminFacade(trips, passes, baggageItems, boardings, compensator);
        return new AirportSystem(passengers, admin);
    }

    public AirportFacade passengers() {
        return passengers;
    }

    public AirportAdminFacade admin() {
        return admin;
    }
}