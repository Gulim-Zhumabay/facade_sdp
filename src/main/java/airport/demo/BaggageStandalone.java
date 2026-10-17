package airport.demo;

import airport.baggage.BaggageHandler;
import airport.baggage.BaggageItem;
import airport.baggage.BaggageRepository;
import airport.baggage.SecurityScreening;

import java.nio.file.Path;

public class BaggageStandalone {
    public static void main(String[] args) {
        BaggageRepository repository =
                new BaggageRepository(Path.of("data", "standalone", "baggage.csv"));
        BaggageHandler handler = new BaggageHandler(repository, new SecurityScreening());

        BaggageItem clean = handler.accept("demo-owner", 12.5, "laptop, clothes");
        System.out.println("accepted:  " + describe(clean));
        System.out.println("screened:  " + describe(handler.screen(clean.getId())));
        System.out.println("released:  " + describe(handler.release(clean.getId())));

        BaggageItem risky = handler.accept("demo-owner", 8.0, "clothes and a knife");
        System.out.println("accepted:  " + describe(risky));
        System.out.println("screened:  " + describe(handler.screen(risky.getId())));

        try {
            handler.accept("demo-owner", 45.0, "books");
        } catch (IllegalArgumentException e) {
            System.out.println("rejected:  " + e.getMessage());
        }

        try {
            handler.release(risky.getId());
        } catch (IllegalStateException e) {
            System.out.println("rejected:  " + e.getMessage());
        }

        System.out.println("baggage on disk:");
        for (BaggageItem saved : repository.findAll()) {
            System.out.println("  " + describe(saved));
        }
    }

    private static String describe(BaggageItem item) {
        return item.getId() + " " + item.getWeightKg() + "kg [" + item.getContents() + "] " + item.getStatus();
    }
}