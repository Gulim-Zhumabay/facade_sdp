package airport.gate;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BorderControlTest {
    private final BorderControl control = new BorderControl();
    private final LocalDate tomorrow = LocalDate.now().plusDays(1);

    @Test
    void validPassportAndSmallCashGiveNoProblem() {
        assertEquals(Optional.empty(), control.findProblem(tomorrow, 500));
    }

    @Test
    void expiredPassportIsAProblem() {
        assertEquals(Optional.of("passport is expired"), control.findProblem(LocalDate.of(2020, 1, 1), 0));
    }

    @Test
    void passportThatExpiresTodayIsAProblem() {
        assertEquals(Optional.of("passport is expired"), control.findProblem(LocalDate.now(), 0));
    }

    @Test
    void cashAtTheLimitIsAllowed() {
        assertEquals(Optional.empty(), control.findProblem(tomorrow, 10000));
    }

    @Test
    void cashAboveTheLimitIsAProblem() {
        assertEquals(Optional.of("declared cash is above 10000 usd"), control.findProblem(tomorrow, 10001));
    }

    @Test
    void expiredPassportIsReportedBeforeCash() {
        assertEquals(Optional.of("passport is expired"), control.findProblem(LocalDate.of(2020, 1, 1), 50000));
    }
}