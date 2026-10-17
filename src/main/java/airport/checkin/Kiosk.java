package airport.checkin;

public class Kiosk {
    private static final String NAME_PATTERN = "[\\p{L}][\\p{L} '\\-]{0,49}";
    private static final String FLIGHT_PATTERN = "[A-Z]{2}\\d{3,4}";

    public BoardingPass startCheckIn(String passengerName, String flightNumber) {
        if (passengerName == null || !passengerName.trim().matches(NAME_PATTERN)) {
            throw new IllegalArgumentException(
                    "passenger name must be 1-50 letters, spaces, hyphens or apostrophes");
        }
        if (flightNumber == null || !flightNumber.matches(FLIGHT_PATTERN)) {
            throw new IllegalArgumentException("flight number must look like KC101");
        }
        return BoardingPass.createNew(passengerName.trim(), flightNumber);
    }
}