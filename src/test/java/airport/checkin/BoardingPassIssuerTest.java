package airport.checkin;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BoardingPassIssuerTest {
    @TempDir
    Path dir;

    private BoardingPassRepository repositoryOnDisk() {
        return new BoardingPassRepository(dir.resolve("passes.csv"));
    }

    private BoardingPassIssuer issuer() {
        return new BoardingPassIssuer(repositoryOnDisk());
    }

    private void assertSavedStatus(String passId, PassStatus expected) {
        BoardingPass saved = repositoryOnDisk().findById(passId).orElseThrow();
        assertEquals(expected, saved.getStatus());
    }

    @Test
    void issuingSavesTheIssuedPass() {
        BoardingPass pass = BoardingPass.createNew("Aigerim", "KC101");

        BoardingPass issued = issuer().issue(pass);

        assertEquals(PassStatus.ISSUED, issued.getStatus());
        assertSavedStatus(pass.getId(), PassStatus.ISSUED);
    }

    @Test
    void issuingTwiceFailsAndLeavesTheSavedPassAlone() {
        BoardingPass pass = BoardingPass.createNew("Aigerim", "KC101");
        issuer().issue(pass);

        assertThrows(IllegalStateException.class, () -> issuer().issue(pass));
        assertSavedStatus(pass.getId(), PassStatus.ISSUED);
    }

    @Test
    void cancellingWorksFromWhatIsOnDisk() {
        BoardingPass pass = BoardingPass.createNew("Aigerim", "KC101");
        issuer().issue(pass);

        BoardingPass cancelled = issuer().cancel(pass.getId());

        assertEquals(PassStatus.CANCELLED, cancelled.getStatus());
        assertSavedStatus(pass.getId(), PassStatus.CANCELLED);
    }

    @Test
    void cancelledPassCannotBeIssuedAgain() {
        BoardingPass pass = BoardingPass.createNew("Aigerim", "KC101");
        issuer().issue(pass);
        BoardingPass cancelled = issuer().cancel(pass.getId());

        assertThrows(IllegalStateException.class, () -> issuer().issue(cancelled));
        assertSavedStatus(pass.getId(), PassStatus.CANCELLED);
    }

    @Test
    void cancellingTwiceFails() {
        BoardingPass pass = BoardingPass.createNew("Aigerim", "KC101");
        issuer().issue(pass);
        issuer().cancel(pass.getId());

        assertThrows(IllegalStateException.class, () -> issuer().cancel(pass.getId()));
    }

    @Test
    void cancellingUnknownPassFails() {
        assertThrows(IllegalArgumentException.class, () -> issuer().cancel("missing"));
    }
}