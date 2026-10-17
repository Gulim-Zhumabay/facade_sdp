package airport.demo;

import airport.gate.BoardingRecord;
import airport.gate.BoardingRepository;
import airport.gate.BorderControl;
import airport.gate.GateController;

import java.nio.file.Path;
import java.time.LocalDate;

public class GateStandalone {
    public static void main(String[] args) {
        BoardingRepository repository =
                new BoardingRepository(Path.of("data", "standalone", "boarding.csv"));
        GateController gate = new GateController(repository, new BorderControl());

        BoardingRecord ok = gate.board("demo-pass", "N1234567", LocalDate.of(2099, 1, 1), 500);
        System.out.println("boarding:  " + describe(ok));

        BoardingRecord expired = gate.board("demo-pass", "N7654321", LocalDate.of(2020, 1, 1), 0);
        System.out.println("boarding:  " + describe(expired));

        BoardingRecord rich = gate.board("demo-pass", "N1111111", LocalDate.of(2099, 1, 1), 50000);
        System.out.println("boarding:  " + describe(rich));

        System.out.println("offload:   " + describe(gate.offload(ok.getId())));

        try {
            gate.offload(expired.getId());
        } catch (IllegalStateException e) {
            System.out.println("rejected:  " + e.getMessage());
        }

        try {
            gate.board("demo-pass", "bad", LocalDate.of(2099, 1, 1), 0);
        } catch (IllegalArgumentException e) {
            System.out.println("rejected:  " + e.getMessage());
        }

        System.out.println("records on disk:");
        for (BoardingRecord saved : repository.findAll()) {
            System.out.println("  " + describe(saved));
        }
    }

    private static String describe(BoardingRecord r) {
        return r.getId() + " " + r.getPassportNumber() + " cash=" + r.getDeclaredCashUsd()
                + " " + r.getStatus() + " (" + r.getNote() + ")";
    }
}