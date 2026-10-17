package airport;

import airport.demo.ConsoleMenu;
import airport.facade.AirportSystem;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        AirportSystem system = AirportSystem.openAt(Path.of("data", "app"));
        Scanner input = new Scanner(System.in, StandardCharsets.UTF_8);
        new ConsoleMenu(system, input, System.out).run();
    }
}