package airport.checkin;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KioskTest {
    private final Kiosk kiosk = new Kiosk();

    @Test
    void validDataGivesPendingPass() {
        BoardingPass pass = kiosk.startCheckIn("Aigerim", "KC101");

        assertEquals(PassStatus.PENDING, pass.getStatus());
        assertEquals("Aigerim", pass.getPassengerName());
        assertEquals("KC101", pass.getFlightNumber());
    }

    @Test
    void nameIsTrimmed() {
        BoardingPass pass = kiosk.startCheckIn("  john smith  ", "KC1234");

        assertEquals("john smith", pass.getPassengerName());
    }

    @Test
    void hyphenAndApostropheAreAllowedInNames() {
        assertDoesNotThrow(() -> kiosk.startCheckIn("Мария-Анна", "AB123"));
        assertDoesNotThrow(() -> kiosk.startCheckIn("O'Brien", "AB123"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "john2", "bad;name", "bad\nname"})
    void badNameIsRejected(String name) {
        assertThrows(IllegalArgumentException.class, () -> kiosk.startCheckIn(name, "KC101"));
    }

    @Test
    void nullNameIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> kiosk.startCheckIn(null, "KC101"));
    }

    @Test
    void nameLongerThanFiftyCharactersIsRejected() {
        assertDoesNotThrow(() -> kiosk.startCheckIn("a".repeat(50), "KC101"));
        assertThrows(IllegalArgumentException.class, () -> kiosk.startCheckIn("a".repeat(51), "KC101"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "kc101", "KC10", "KC12345", "K1010", "KC101 "})
    void badFlightIsRejected(String flight) {
        assertThrows(IllegalArgumentException.class, () -> kiosk.startCheckIn("Aigerim", flight));
    }

    @Test
    void nullFlightIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> kiosk.startCheckIn("Aigerim", null));
    }
}