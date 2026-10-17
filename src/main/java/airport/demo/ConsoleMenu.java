package airport.demo;

import airport.facade.AirportSystem;
import airport.facade.TripChainException;
import airport.facade.TripRecord;
import airport.facade.TripReport;

import java.io.PrintStream;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class ConsoleMenu {
    private final AirportSystem system;
    private final Scanner input;
    private final PrintStream out;

    public ConsoleMenu(AirportSystem system, Scanner input, PrintStream out) {
        this.system = system;
        this.input = input;
        this.out = out;
    }

    public void run() {
        try {
            while (true) {
                printMenu();
                String choice = ask("choice");
                if (choice.equals("0")) {
                    out.println("bye");
                    return;
                }
                runChoice(choice);
            }
        } catch (NoSuchElementException e) {
            out.println("input closed, bye");
        }
    }

    private void printMenu() {
        out.println();
        out.println("=== Airport operations ===");
        out.println("1  Full trip: check-in, baggage, gate (A -> B -> C)");
        out.println("2  Part one: check-in and baggage (A -> B)");
        out.println("3  Part two: gate for a waiting trip (C)");
        out.println("4  Show all trips");
        out.println("5  Inspect one trip");
        out.println("6  Recover after a restart");
        out.println("7  Undo a waiting trip by hand");
        out.println("8  Demo: baggage fails after check-in (A undone)");
        out.println("9  Demo: gate fails after check-in and baggage (B and A undone)");
        out.println("0  Exit");
    }

    private void runChoice(String choice) {
        try {
            switch (choice) {
                case "1" -> fullTrip();
                case "2" -> partOne();
                case "3" -> partTwo();
                case "4" -> showAll();
                case "5" -> inspect(ask("trip id"));
                case "6" -> recover();
                case "7" -> undo();
                case "8" -> demoBaggageFails();
                case "9" -> demoGateFails();
                default -> out.println("unknown choice");
            }
        } catch (TripChainException e) {
            out.println("FAILED: " + e.getMessage());
            if (e.getTripId() != null) {
                inspect(e.getTripId());
            }
        } catch (NoSuchElementException e) {
            throw e;
        } catch (RuntimeException e) {
            out.println("bad input: " + e.getMessage());
        }
    }

    private void fullTrip() {
        String name = ask("passenger name");
        String flight = ask("flight number, like KC101");
        double weight = Double.parseDouble(ask("baggage weight in kg"));
        String contents = ask("baggage contents");
        String passport = ask("passport number");
        LocalDate expiry = LocalDate.parse(ask("passport expiry, like 2030-05-01"));
        int cash = Integer.parseInt(ask("declared cash in usd"));
        TripRecord trip = system.passengers().travel(name, flight, weight, contents, passport, expiry, cash);
        out.println("DONE");
        inspect(trip.getId());
    }

    private void partOne() {
        String name = ask("passenger name");
        String flight = ask("flight number, like KC101");
        double weight = Double.parseDouble(ask("baggage weight in kg"));
        String contents = ask("baggage contents");
        TripRecord trip = system.passengers().checkInWithBaggage(name, flight, weight, contents);
        out.println("DONE, waiting for the gate");
        inspect(trip.getId());
    }

    private void partTwo() {
        String tripId = ask("trip id");
        String passport = ask("passport number");
        LocalDate expiry = LocalDate.parse(ask("passport expiry, like 2030-05-01"));
        int cash = Integer.parseInt(ask("declared cash in usd"));
        TripRecord trip = system.passengers().passGate(tripId, passport, expiry, cash);
        out.println("DONE");
        inspect(trip.getId());
    }

    private void showAll() {
        List<TripRecord> trips = system.admin().listTrips();
        if (trips.isEmpty()) {
            out.println("no trips yet");
        }
        for (TripRecord trip : trips) {
            out.println(describe(trip));
        }
    }

    private void recover() {
        List<TripRecord> changed = system.admin().recover();
        if (changed.isEmpty()) {
            out.println("nothing to recover");
        }
        for (TripRecord trip : changed) {
            out.println("recovered: " + describe(trip));
        }
    }

    private void undo() {
        TripRecord trip = system.admin().compensate(ask("trip id"));
        out.println("undone: " + describe(trip));
        inspect(trip.getId());
    }

    private void demoBaggageFails() {
        out.println("check-in is fine, baggage has a knife in it");
        system.passengers().travel("Aigerim", "KC101", 12.5, "clothes and a knife",
                "N1234567", LocalDate.of(2099, 1, 1), 500);
    }

    private void demoGateFails() {
        out.println("check-in and baggage are fine, passport is expired");
        system.passengers().travel("Aigerim", "KC101", 12.5, "clothes and a laptop",
                "N1234567", LocalDate.of(2020, 1, 1), 500);
    }

    private void inspect(String tripId) {
        TripReport report = system.admin().inspect(tripId);
        out.println(describe(report.trip()));
        out.println("  pass:    " + report.pass()
                .map(p -> p.getId() + " " + p.getPassengerName() + " " + p.getFlightNumber() + " " + p.getStatus())
                .orElse("none"));
        out.println("  baggage: " + report.baggage()
                .map(b -> b.getId() + " " + b.getWeightKg() + "kg [" + b.getContents() + "] " + b.getStatus())
                .orElse("none"));
        out.println("  gate:    " + report.boarding()
                .map(r -> r.getId() + " " + r.getStatus() + " (" + r.getNote() + ")")
                .orElse("none"));
    }

    private String describe(TripRecord trip) {
        return "trip " + trip.getId() + " " + trip.getStatus() + " (" + trip.getNote() + ")";
    }

    private String ask(String label) {
        out.print(label + ": ");
        out.flush();
        return input.nextLine().trim();
    }
}