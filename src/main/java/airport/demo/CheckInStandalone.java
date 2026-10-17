package airport.demo;

import airport.checkin.BoardingPass;
import airport.checkin.BoardingPassIssuer;
import airport.checkin.BoardingPassRepository;
import airport.checkin.Kiosk;

import java.nio.file.Path;

public class CheckInStandalone {
    public static void main(String[] args) {
        BoardingPassRepository repository =
                new BoardingPassRepository(Path.of("data", "standalone", "boarding_passes.csv"));
        Kiosk kiosk = new Kiosk();
        BoardingPassIssuer issuer = new BoardingPassIssuer(repository);

        BoardingPass pass = kiosk.startCheckIn("Aigerim", "KC101");
        System.out.println("started:   " + describe(pass));

        issuer.issue(pass);
        System.out.println("issued:    " + describe(pass));

        BoardingPass cancelled = issuer.cancel(pass.getId());
        System.out.println("cancelled: " + describe(cancelled));

        try {
            issuer.issue(cancelled);
        } catch (IllegalStateException e) {
            System.out.println("rejected:  " + e.getMessage());
        }

        try {
            kiosk.startCheckIn("", "KC101");
        } catch (IllegalArgumentException e) {
            System.out.println("rejected:  " + e.getMessage());
        }

        System.out.println("passes on disk:");
        for (BoardingPass saved : repository.findAll()) {
            System.out.println("  " + describe(saved));
        }
    }

    private static String describe(BoardingPass pass) {
        return pass.getId() + " " + pass.getPassengerName() + " " + pass.getFlightNumber() + " " + pass.getStatus();
    }
}