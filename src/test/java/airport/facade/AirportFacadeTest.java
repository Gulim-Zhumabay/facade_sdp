package airport.facade;

import airport.baggage.BaggageStatus;
import airport.checkin.PassStatus;
import airport.gate.BoardingStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static airport.facade.AirportDesk.EXPIRED;
import static airport.facade.AirportDesk.VALID_UNTIL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AirportFacadeTest {
    @TempDir
    Path dir;

    private AirportDesk desk;

    @BeforeEach
    void openDesk() {
        desk = new AirportDesk(dir);
    }

    @Test
    void goodPassengerCompletesTheWholeChain() {
        String tripId = desk.passengerBoards();

        desk.assertTripIs(tripId, TripStatus.COMPLETED);
        desk.assertPassIs(tripId, PassStatus.ISSUED);
        desk.assertBaggageIs(tripId, BaggageStatus.CLEARED);
        desk.assertGateRecordIs(tripId, BoardingStatus.BOARDED);
    }

    @Test
    void baggageFailingAfterCheckInUndoesOnlyThePass() {
        String tripId = desk.tripWhereBaggageHasAKnife();

        desk.assertTripIs(tripId, TripStatus.COMPENSATED);
        desk.assertPassIs(tripId, PassStatus.CANCELLED);
        desk.assertBaggageIs(tripId, BaggageStatus.REJECTED);
        desk.assertNoGateRecord(tripId);
        desk.assertTripNoteContains(tripId, "security screening");
    }

    @Test
    void tooHeavyBaggageUndoesThePassAndSavesNoBaggage() {
        String tripId = desk.tripWhereBaggageIsTooHeavy();

        desk.assertTripIs(tripId, TripStatus.COMPENSATED);
        desk.assertPassIs(tripId, PassStatus.CANCELLED);
        desk.assertNoBaggageSaved();
        desk.assertNoGateRecord(tripId);
    }

    @Test
    void gateFailingAfterCheckInAndBaggageUndoesBaggageThenPass() {
        String tripId = desk.tripWhereGateRefusesAnExpiredPassport();

        desk.assertTripIs(tripId, TripStatus.COMPENSATED);
        desk.assertBaggageIs(tripId, BaggageStatus.RETURNED);
        desk.assertPassIs(tripId, PassStatus.CANCELLED);
        desk.assertGateRecordIs(tripId, BoardingStatus.DENIED);
        desk.assertTripNoteContains(tripId, "passport is expired");
    }

    @Test
    void tooMuchCashAtTheGateUndoesBaggageAndPass() {
        String tripId = desk.tripWhereGateRefusesTooMuchCash();

        desk.assertTripIs(tripId, TripStatus.COMPENSATED);
        desk.assertBaggageIs(tripId, BaggageStatus.RETURNED);
        desk.assertPassIs(tripId, PassStatus.CANCELLED);
        desk.assertGateRecordIs(tripId, BoardingStatus.DENIED);
    }

    @Test
    void badPassportNumberAtTheGateUndoesBaggageAndPassWithoutAGateRecord() {
        String tripId = desk.tripWhereGateGetsABadPassportNumber();

        desk.assertTripIs(tripId, TripStatus.COMPENSATED);
        desk.assertBaggageIs(tripId, BaggageStatus.RETURNED);
        desk.assertPassIs(tripId, PassStatus.CANCELLED);
        desk.assertNoGateRecord(tripId);
    }

    @Test
    void badNameFailsBeforeAnythingIsSaved() {
        InvalidRequestException failure = assertThrows(InvalidRequestException.class,
                () -> desk.passengers().travel("", "KC101", 12.5, "clothes", "N1234567", VALID_UNTIL, 500));

        assertTrue(failure.getMessage().contains("passenger name"));
        desk.assertNothingIsSaved();
    }

    @Test
    void badFlightNumberFailsBeforeAnythingIsSaved() {
        assertThrows(InvalidRequestException.class,
                () -> desk.passengers().checkInWithBaggage("Aigerim", "kc1", 12.5, "clothes"));

        desk.assertNothingIsSaved();
    }

    @Test
    void checkInWithBaggageAloneLeavesTheTripWaitingForTheGate() {
        String tripId = desk.passengerWaitsAtTheGate();

        desk.assertTripIs(tripId, TripStatus.PENDING_C);
        desk.assertPassIs(tripId, PassStatus.ISSUED);
        desk.assertBaggageIs(tripId, BaggageStatus.CLEARED);
        desk.assertNoGateRecord(tripId);
    }

    @Test
    void waitingTripIsStillWaitingAfterARestartAndCanFinish() {
        String tripId = desk.passengerWaitsAtTheGate();

        desk.restart();
        desk.assertTripIs(tripId, TripStatus.PENDING_C);
        TripRecord finished = desk.passengers().passGate(tripId, "N1234567", VALID_UNTIL, 500);

        assertEquals(TripStatus.COMPLETED, finished.getStatus());
        desk.assertTripIs(tripId, TripStatus.COMPLETED);
        desk.assertGateRecordIs(tripId, BoardingStatus.BOARDED);
    }

    @Test
    void gateFailureAfterARestartStillUndoesBothEarlierSteps() {
        String tripId = desk.passengerWaitsAtTheGate();

        desk.restart();
        GateStepFailedException failure = assertThrows(GateStepFailedException.class,
                () -> desk.passengers().passGate(tripId, "N1234567", EXPIRED, 500));

        assertEquals(tripId, failure.getTripId());
        desk.assertTripIs(tripId, TripStatus.COMPENSATED);
        desk.assertBaggageIs(tripId, BaggageStatus.RETURNED);
        desk.assertPassIs(tripId, PassStatus.CANCELLED);
    }

    @Test
    void passingTheGateForAnUnknownTripFails() {
        assertThrows(TripNotFoundException.class,
                () -> desk.passengers().passGate("missing", "N1234567", VALID_UNTIL, 500));
    }

    @Test
    void passingTheGateForAFinishedTripFails() {
        String tripId = desk.passengerBoards();

        assertThrows(TripStateException.class,
                () -> desk.passengers().passGate(tripId, "N1234567", VALID_UNTIL, 500));
        desk.assertTripIs(tripId, TripStatus.COMPLETED);
    }

    @Test
    void passingTheGateForAnUndoneTripFails() {
        String tripId = desk.tripWhereBaggageHasAKnife();

        assertThrows(TripStateException.class,
                () -> desk.passengers().passGate(tripId, "N1234567", VALID_UNTIL, 500));
        desk.assertTripIs(tripId, TripStatus.COMPENSATED);
    }

    @Test
    void failureMessagesTellWhatWasUndone() {
        BaggageStepFailedException baggageFailure = assertThrows(BaggageStepFailedException.class,
                () -> desk.passengers().travel("Aigerim", "KC101", 12.5, "a knife", "N1234567", VALID_UNTIL, 0));
        GateStepFailedException gateFailure = assertThrows(GateStepFailedException.class,
                () -> desk.passengers().travel("Aigerim", "KC101", 12.5, "books", "N1234567", EXPIRED, 0));

        assertTrue(baggageFailure.getMessage().contains("boarding pass cancelled"));
        assertTrue(gateFailure.getMessage().contains("baggage returned"));
    }
}