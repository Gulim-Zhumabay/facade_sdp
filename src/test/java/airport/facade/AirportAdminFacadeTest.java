package airport.facade;

import airport.baggage.BaggageStatus;
import airport.checkin.PassStatus;
import airport.gate.BoardingStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AirportAdminFacadeTest {
    @TempDir
    Path dir;

    private AirportDesk desk;

    @BeforeEach
    void openDesk() {
        desk = new AirportDesk(dir);
    }

    @Test
    void listShowsEveryTrip() {
        desk.passengerBoards();
        desk.tripWhereBaggageHasAKnife();

        assertEquals(2, desk.admin().listTrips().size());
    }

    @Test
    void listIsEmptyAtTheStart() {
        assertTrue(desk.admin().listTrips().isEmpty());
    }

    @Test
    void inspectShowsWhatEverySubsystemHasOnDisk() {
        String tripId = desk.passengerBoards();

        TripReport report = desk.admin().inspect(tripId);

        assertEquals(TripStatus.COMPLETED, report.trip().getStatus());
        assertEquals(PassStatus.ISSUED, report.pass().orElseThrow().getStatus());
        assertEquals(BaggageStatus.CLEARED, report.baggage().orElseThrow().getStatus());
        assertEquals(BoardingStatus.BOARDED, report.boarding().orElseThrow().getStatus());
    }

    @Test
    void inspectOfAWaitingTripHasNoGateRecord() {
        String tripId = desk.passengerWaitsAtTheGate();

        TripReport report = desk.admin().inspect(tripId);

        assertEquals(TripStatus.PENDING_C, report.trip().getStatus());
        assertTrue(report.boarding().isEmpty());
    }

    @Test
    void inspectOfATripWithoutBaggageHasNoBaggage() {
        String tripId = desk.tripWhereBaggageIsTooHeavy();

        TripReport report = desk.admin().inspect(tripId);

        assertTrue(report.baggage().isEmpty());
        assertEquals(PassStatus.CANCELLED, report.pass().orElseThrow().getStatus());
    }

    @Test
    void inspectOfAnUnknownTripFails() {
        assertThrows(TripNotFoundException.class, () -> desk.admin().inspect("missing"));
    }

    @Test
    void undoingAWaitingTripReturnsBaggageThenCancelsThePass() {
        String tripId = desk.passengerWaitsAtTheGate();

        TripRecord undone = desk.admin().compensate(tripId);

        assertEquals(TripStatus.COMPENSATED, undone.getStatus());
        desk.assertTripIs(tripId, TripStatus.COMPENSATED);
        desk.assertBaggageIs(tripId, BaggageStatus.RETURNED);
        desk.assertPassIs(tripId, PassStatus.CANCELLED);
        desk.assertNoGateRecord(tripId);
        desk.assertTripNoteContains(tripId, "administrator");
    }

    @Test
    void undoingAFinishedTripFails() {
        String tripId = desk.passengerBoards();

        assertThrows(TripStateException.class, () -> desk.admin().compensate(tripId));
        desk.assertTripIs(tripId, TripStatus.COMPLETED);
    }

    @Test
    void undoingAnUndoneTripFails() {
        String tripId = desk.tripWhereBaggageHasAKnife();

        assertThrows(TripStateException.class, () -> desk.admin().compensate(tripId));
    }

    @Test
    void undoingAnUnknownTripFails() {
        assertThrows(TripNotFoundException.class, () -> desk.admin().compensate("missing"));
    }

    @Test
    void undoingATripWhosePassengerAlreadyBoardedIsRefused() {
        String tripId = desk.passengerWaitsAtTheGate();
        desk.gateBoardsThePassengerButTheTripIsNotUpdated(tripId);

        assertThrows(TripStateException.class, () -> desk.admin().compensate(tripId));
        desk.assertPassIs(tripId, PassStatus.ISSUED);
        desk.assertBaggageIs(tripId, BaggageStatus.CLEARED);
    }

    @Test
    void recoveryUndoesATripInterruptedRightAfterCheckIn() {
        String tripId = desk.tripInterruptedRightAfterCheckIn();

        desk.restart();
        List<TripRecord> recovered = desk.admin().recover();

        assertEquals(1, recovered.size());
        assertEquals(tripId, recovered.get(0).getId());
        desk.assertTripIs(tripId, TripStatus.COMPENSATED);
        desk.assertPassIs(tripId, PassStatus.CANCELLED);
        desk.assertNoBaggageSaved();
    }

    @Test
    void recoveryUndoesBaggageAndPassOfATripInterruptedDuringScreening() {
        String tripId = desk.tripInterruptedWhileBaggageWasBeingScreened();

        desk.restart();
        List<TripRecord> recovered = desk.admin().recover();

        assertEquals(1, recovered.size());
        desk.assertTripIs(tripId, TripStatus.COMPENSATED);
        desk.assertBaggageIs(tripId, BaggageStatus.RETURNED);
        desk.assertPassIs(tripId, PassStatus.CANCELLED);
    }

    @Test
    void recoveryFinishesATripWhosePassengerBoardedBeforeTheInterruption() {
        String tripId = desk.passengerWaitsAtTheGate();
        desk.gateBoardsThePassengerButTheTripIsNotUpdated(tripId);

        desk.restart();
        List<TripRecord> recovered = desk.admin().recover();

        assertEquals(1, recovered.size());
        desk.assertTripIs(tripId, TripStatus.COMPLETED);
        desk.assertGateRecordIs(tripId, BoardingStatus.BOARDED);
        desk.assertPassIs(tripId, PassStatus.ISSUED);
        desk.assertBaggageIs(tripId, BaggageStatus.CLEARED);
        assertNotNull(desk.admin().inspect(tripId).trip().getBoardingId());
    }

    @Test
    void recoveryUndoesATripWhoseGateDeniedThePassengerBeforeTheInterruption() {
        String tripId = desk.passengerWaitsAtTheGate();
        desk.gateDeniesThePassengerButTheTripIsNotUpdated(tripId);

        desk.restart();
        List<TripRecord> recovered = desk.admin().recover();

        assertEquals(1, recovered.size());
        desk.assertTripIs(tripId, TripStatus.COMPENSATED);
        desk.assertBaggageIs(tripId, BaggageStatus.RETURNED);
        desk.assertPassIs(tripId, PassStatus.CANCELLED);
        desk.assertGateRecordIs(tripId, BoardingStatus.DENIED);
        desk.assertTripNoteContains(tripId, "passport is expired");
    }

    @Test
    void recoveryLeavesATripThatIsJustWaitingForTheGate() {
        String tripId = desk.passengerWaitsAtTheGate();

        desk.restart();
        List<TripRecord> recovered = desk.admin().recover();

        assertTrue(recovered.isEmpty());
        desk.assertTripIs(tripId, TripStatus.PENDING_C);
        desk.assertPassIs(tripId, PassStatus.ISSUED);
    }

    @Test
    void recoveryIgnoresFinishedTrips() {
        desk.passengerBoards();
        desk.tripWhereGateRefusesAnExpiredPassport();

        assertTrue(desk.admin().recover().isEmpty());
    }
}