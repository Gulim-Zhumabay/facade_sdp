package airport.facade;

import airport.baggage.BaggageItem;
import airport.checkin.BoardingPass;
import airport.gate.BoardingRecord;

import java.util.Optional;

public record TripReport(TripRecord trip, Optional<BoardingPass> pass, Optional<BaggageItem> baggage,
                         Optional<BoardingRecord> boarding) {
}