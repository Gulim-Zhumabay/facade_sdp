package airport.facade;

public class InvalidRequestException extends TripChainException {
    public InvalidRequestException(String message, Throwable cause) {
        super(null, message, cause);
    }
}