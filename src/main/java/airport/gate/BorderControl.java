package airport.gate;

import java.time.LocalDate;
import java.util.Optional;

public class BorderControl {
    private static final int CASH_LIMIT_USD = 10000;

    public Optional<String> findProblem(LocalDate passportExpiry, int declaredCashUsd) {
        if (!passportExpiry.isAfter(LocalDate.now())) {
            return Optional.of("passport is expired");
        }
        if (declaredCashUsd > CASH_LIMIT_USD) {
            return Optional.of("declared cash is above " + CASH_LIMIT_USD + " usd");
        }
        return Optional.empty();
    }
}