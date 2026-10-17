package airport.facade;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TripRecordTest {

    @Test
    void newTripWaitsForBaggage() {
        TripRecord trip = TripRecord.createNew("pass-1");

        assertEquals(TripStatus.PENDING_B, trip.getStatus());
        assertEquals(8, trip.getId().length());
        assertEquals("pass-1", trip.getPassId());
        assertNull(trip.getBaggageId());
        assertNull(trip.getBoardingId());
    }

    @Test
    void tripWalksThroughTheHappyPath() {
        TripRecord trip = TripRecord.createNew("pass-1");
        trip.attachBaggage("bag-1");

        trip.baggageCleared();
        assertEquals(TripStatus.PENDING_C, trip.getStatus());
        trip.attachBoarding("gate-1");
        trip.completed();

        assertEquals(TripStatus.COMPLETED, trip.getStatus());
        assertEquals("bag-1", trip.getBaggageId());
        assertEquals("gate-1", trip.getBoardingId());
    }

    @Test
    void tripCanBeUndoneFromBothPendingStatuses() {
        TripRecord early = TripRecord.createNew("pass-1");
        TripRecord late = TripRecord.createNew("pass-2");
        late.baggageCleared();

        early.compensated("reason one");
        late.compensated("reason two");

        assertEquals(TripStatus.COMPENSATED, early.getStatus());
        assertEquals(TripStatus.COMPENSATED, late.getStatus());
        assertEquals("undone: reason one", early.getNote());
    }

    @Test
    void tripCannotJumpOverTheBaggageStep() {
        TripRecord trip = TripRecord.createNew("pass-1");

        assertThrows(IllegalStateException.class, trip::completed);
        assertEquals(TripStatus.PENDING_B, trip.getStatus());
    }

    @Test
    void finishedTripsCannotMoveAnywhere() {
        TripRecord completed = TripRecord.createNew("pass-1");
        completed.baggageCleared();
        completed.completed();
        TripRecord undone = TripRecord.createNew("pass-2");
        undone.compensated("reason");

        assertThrows(IllegalStateException.class, () -> completed.compensated("late"));
        assertThrows(IllegalStateException.class, undone::baggageCleared);
        assertThrows(IllegalStateException.class, () -> undone.compensated("again"));
    }

    @Test
    void semicolonsAndLineBreaksAreRemovedFromTheNote() {
        TripRecord trip = TripRecord.createNew("pass-1");

        trip.compensated("bad; reason\nwith\rbreaks");

        assertEquals("undone: bad, reason with breaks", trip.getNote());
    }

    @Test
    void missingNoteBecomesEmpty() {
        TripRecord trip = new TripRecord("id000001", "pass-1", null, null, TripStatus.PENDING_B, null);

        assertEquals("", trip.getNote());
    }

    @Test
    void onlyPendingStatusesArePending() {
        assertTrue(TripStatus.PENDING_B.isPending());
        assertTrue(TripStatus.PENDING_C.isPending());
        assertFalse(TripStatus.COMPLETED.isPending());
        assertFalse(TripStatus.COMPENSATED.isPending());
    }

    @Test
    void statusesAllowOnlyTheLegalMoves() {
        assertTrue(TripStatus.PENDING_B.canMoveTo(TripStatus.PENDING_C));
        assertTrue(TripStatus.PENDING_B.canMoveTo(TripStatus.COMPENSATED));
        assertFalse(TripStatus.PENDING_B.canMoveTo(TripStatus.COMPLETED));
        assertTrue(TripStatus.PENDING_C.canMoveTo(TripStatus.COMPLETED));
        assertTrue(TripStatus.PENDING_C.canMoveTo(TripStatus.COMPENSATED));
        assertFalse(TripStatus.PENDING_C.canMoveTo(TripStatus.PENDING_B));
        assertFalse(TripStatus.COMPLETED.canMoveTo(TripStatus.COMPENSATED));
        assertFalse(TripStatus.COMPENSATED.canMoveTo(TripStatus.PENDING_B));
    }
}