package airport.checkin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BoardingPassTest {

    @Test
    void newPassStartsAsPending() {
        BoardingPass pass = BoardingPass.createNew("Aigerim", "KC101");

        assertEquals(PassStatus.PENDING, pass.getStatus());
        assertEquals(8, pass.getId().length());
    }

    @Test
    void pendingPassCanBeIssued() {
        BoardingPass pass = BoardingPass.createNew("Aigerim", "KC101");

        pass.issue();

        assertEquals(PassStatus.ISSUED, pass.getStatus());
    }

    @Test
    void pendingPassCanBeCancelled() {
        BoardingPass pass = BoardingPass.createNew("Aigerim", "KC101");

        pass.cancel();

        assertEquals(PassStatus.CANCELLED, pass.getStatus());
    }

    @Test
    void issuedPassCanBeCancelled() {
        BoardingPass pass = BoardingPass.createNew("Aigerim", "KC101");
        pass.issue();

        pass.cancel();

        assertEquals(PassStatus.CANCELLED, pass.getStatus());
    }

    @Test
    void issuedPassCannotBeIssuedAgain() {
        BoardingPass pass = BoardingPass.createNew("Aigerim", "KC101");
        pass.issue();

        assertThrows(IllegalStateException.class, pass::issue);
        assertEquals(PassStatus.ISSUED, pass.getStatus());
    }

    @Test
    void cancelledPassCannotBeIssued() {
        BoardingPass pass = BoardingPass.createNew("Aigerim", "KC101");
        pass.cancel();

        assertThrows(IllegalStateException.class, pass::issue);
        assertEquals(PassStatus.CANCELLED, pass.getStatus());
    }

    @Test
    void cancelledPassCannotBeCancelledAgain() {
        BoardingPass pass = BoardingPass.createNew("Aigerim", "KC101");
        pass.cancel();

        assertThrows(IllegalStateException.class, pass::cancel);
    }

    @Test
    void passRestoredFromStorageKeepsItsData() {
        BoardingPass pass = new BoardingPass("abc12345", "Aigerim", "KC101", PassStatus.ISSUED);

        assertEquals("abc12345", pass.getId());
        assertEquals("Aigerim", pass.getPassengerName());
        assertEquals("KC101", pass.getFlightNumber());
        assertEquals(PassStatus.ISSUED, pass.getStatus());
    }
}