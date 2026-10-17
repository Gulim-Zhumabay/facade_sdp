package airport.checkin;

public class BoardingPassIssuer {
    private final BoardingPassRepository repository;

    public BoardingPassIssuer(BoardingPassRepository repository) {
        this.repository = repository;
    }

    public BoardingPass issue(BoardingPass pass) {
        pass.issue();
        repository.save(pass);
        return pass;
    }

    public BoardingPass cancel(String passId) {
        BoardingPass pass = repository.findById(passId)
                .orElseThrow(() -> new IllegalArgumentException("boarding pass not found: " + passId));
        pass.cancel();
        repository.save(pass);
        return pass;
    }
}