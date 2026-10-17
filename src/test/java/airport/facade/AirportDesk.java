package airport.facade;

import airport.baggage.BaggageHandler;
import airport.baggage.BaggageRepository;
import airport.baggage.BaggageStatus;
import airport.baggage.SecurityScreening;
import airport.checkin.BoardingPass;
import airport.checkin.BoardingPassIssuer;
import airport.checkin.BoardingPassRepository;
import airport.checkin.Kiosk;
import airport.checkin.PassStatus;
import airport.gate.BoardingRecord;
import airport.gate.BoardingRepository;
import airport.gate.BoardingStatus;
import airport.gate.BorderControl;
import airport.gate.GateController;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AirportDesk {
    static final LocalDate VALID_UNTIL = LocalDate.of(2099, 1, 1);
    static final LocalDate EXPIRED = LocalDate.of(2020, 1, 1);

    private final Path dir;
    private AirportSystem system;

    AirportDesk(Path dir) {
        this.dir = dir;
        this.system = AirportSystem.openAt(dir);
    }

    AirportFacade passengers() {
        return system.passengers();
    }

    AirportAdminFacade admin() {
        return system.admin();
    }

    void restart() {
        system = AirportSystem.openAt(dir);
    }

    String passengerBoards() {
        return passengers().travel("Aigerim", "KC101", 12.5, "clothes and a laptop",
                "N1234567", VALID_UNTIL, 500).getId();
    }

    String passengerWaitsAtTheGate() {
        return passengers().checkInWithBaggage("Aigerim", "KC101", 12.5, "clothes and a laptop").getId();
    }

    String tripWhereBaggageHasAKnife() {
        BaggageStepFailedException failure = assertThrows(BaggageStepFailedException.class,
                () -> passengers().travel("Aigerim", "KC101", 12.5, "clothes and a knife",
                        "N1234567", VALID_UNTIL, 500));
        return failure.getTripId();
    }

    String tripWhereBaggageIsTooHeavy() {
        BaggageStepFailedException failure = assertThrows(BaggageStepFailedException.class,
                () -> passengers().travel("Aigerim", "KC101", 45.0, "books",
                        "N1234567", VALID_UNTIL, 500));
        return failure.getTripId();
    }

    String tripWhereGateRefusesAnExpiredPassport() {
        GateStepFailedException failure = assertThrows(GateStepFailedException.class,
                () -> passengers().travel("Aigerim", "KC101", 12.5, "clothes and a laptop",
                        "N1234567", EXPIRED, 500));
        return failure.getTripId();
    }

    String tripWhereGateRefusesTooMuchCash() {
        GateStepFailedException failure = assertThrows(GateStepFailedException.class,
                () -> passengers().travel("Aigerim", "KC101", 12.5, "clothes and a laptop",
                        "N1234567", VALID_UNTIL, 50000));
        return failure.getTripId();
    }

    String tripWhereGateGetsABadPassportNumber() {
        GateStepFailedException failure = assertThrows(GateStepFailedException.class,
                () -> passengers().travel("Aigerim", "KC101", 12.5, "clothes and a laptop",
                        "bad", VALID_UNTIL, 500));
        return failure.getTripId();
    }

    String tripInterruptedRightAfterCheckIn() {
        BoardingPass pass = passIssuedDirectly();
        TripRecord trip = TripRecord.createNew(pass.getId());
        savedTrips().save(trip);
        return trip.getId();
    }

    String tripInterruptedWhileBaggageWasBeingScreened() {
        BoardingPass pass = passIssuedDirectly();
        BaggageHandler handler = new BaggageHandler(savedBaggage(), new SecurityScreening());
        TripRecord trip = TripRecord.createNew(pass.getId());
        trip.attachBaggage(handler.accept(pass.getId(), 12.5, "clothes").getId());
        savedTrips().save(trip);
        return trip.getId();
    }

    void gateBoardsThePassengerButTheTripIsNotUpdated(String tripId) {
        gateChecksThePassenger(tripId, VALID_UNTIL);
    }

    void gateDeniesThePassengerButTheTripIsNotUpdated(String tripId) {
        gateChecksThePassenger(tripId, EXPIRED);
    }

    void assertTripIs(String tripId, TripStatus expected) {
        assertEquals(expected, savedTrip(tripId).getStatus());
    }

    void assertTripNoteContains(String tripId, String text) {
        String note = savedTrip(tripId).getNote();
        assertTrue(note.contains(text), "note was: " + note);
    }

    void assertPassIs(String tripId, PassStatus expected) {
        TripRecord trip = savedTrip(tripId);
        PassStatus actual = savedPasses().findById(trip.getPassId()).orElseThrow().getStatus();
        assertEquals(expected, actual);
    }

    void assertBaggageIs(String tripId, BaggageStatus expected) {
        TripRecord trip = savedTrip(tripId);
        BaggageStatus actual = savedBaggage().findById(trip.getBaggageId()).orElseThrow().getStatus();
        assertEquals(expected, actual);
    }

    void assertNoBaggageSaved() {
        assertTrue(savedBaggage().findAll().isEmpty());
    }

    void assertGateRecordIs(String tripId, BoardingStatus expected) {
        List<BoardingRecord> records = gateRecordsOf(savedTrip(tripId));
        assertEquals(1, records.size());
        assertEquals(expected, records.get(0).getStatus());
    }

    void assertNoGateRecord(String tripId) {
        assertTrue(gateRecordsOf(savedTrip(tripId)).isEmpty());
    }

    void assertNothingIsSaved() {
        assertTrue(savedTrips().findAll().isEmpty());
        assertTrue(savedPasses().findAll().isEmpty());
        assertTrue(savedBaggage().findAll().isEmpty());
        assertTrue(savedBoardings().findAll().isEmpty());
    }

    private BoardingPass passIssuedDirectly() {
        BoardingPass pass = new Kiosk().startCheckIn("Aigerim", "KC101");
        return new BoardingPassIssuer(savedPasses()).issue(pass);
    }

    private void gateChecksThePassenger(String tripId, LocalDate expiry) {
        TripRecord trip = savedTrip(tripId);
        new GateController(savedBoardings(), new BorderControl())
                .board(trip.getPassId(), "N1234567", expiry, 500);
    }

    private List<BoardingRecord> gateRecordsOf(TripRecord trip) {
        return savedBoardings().findAll().stream()
                .filter(record -> record.getPassRef().equals(trip.getPassId()))
                .toList();
    }

    private TripRecord savedTrip(String tripId) {
        return savedTrips().findById(tripId).orElseThrow();
    }

    private TripRepository savedTrips() {
        return new TripRepository(dir.resolve("trips.csv"));
    }

    private BoardingPassRepository savedPasses() {
        return new BoardingPassRepository(dir.resolve("boarding_passes.csv"));
    }

    private BaggageRepository savedBaggage() {
        return new BaggageRepository(dir.resolve("baggage.csv"));
    }

    private BoardingRepository savedBoardings() {
        return new BoardingRepository(dir.resolve("boarding.csv"));
    }
}