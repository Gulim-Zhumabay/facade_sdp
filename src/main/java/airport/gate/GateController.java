package airport.gate;

import java.time.LocalDate;
import java.util.Optional;

public class GateController {
    private static final String REF_PATTERN = "[A-Za-z0-9\\-]{1,36}";
    private static final String PASSPORT_PATTERN = "[A-Z0-9]{6,9}";

    private final BoardingRepository repository;
    private final BorderControl borderControl;

    public GateController(BoardingRepository repository, BorderControl borderControl) {
        this.repository = repository;
        this.borderControl = borderControl;
    }

    public BoardingRecord board(String passRef, String passportNumber, LocalDate passportExpiry,
                                int declaredCashUsd) {
        if (passRef == null || !passRef.matches(REF_PATTERN)) {
            throw new IllegalArgumentException("pass reference must be 1-36 letters, digits or hyphens");
        }
        if (passportNumber == null || !passportNumber.matches(PASSPORT_PATTERN)) {
            throw new IllegalArgumentException("passport number must be 6-9 capital letters or digits");
        }
        if (passportExpiry == null) {
            throw new IllegalArgumentException("passport expiry date is required");
        }
        if (declaredCashUsd < 0) {
            throw new IllegalArgumentException("declared cash must not be negative");
        }
        BoardingRecord record = BoardingRecord.createNew(passRef, passportNumber, passportExpiry, declaredCashUsd);
        Optional<String> problem = borderControl.findProblem(passportExpiry, declaredCashUsd);
        if (problem.isPresent()) {
            record.deny(problem.get());
        } else {
            record.board();
        }
        repository.save(record);
        return record;
    }

    public BoardingRecord offload(String recordId) {
        BoardingRecord record = repository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("boarding record not found: " + recordId));
        record.offload();
        repository.save(record);
        return record;
    }
}