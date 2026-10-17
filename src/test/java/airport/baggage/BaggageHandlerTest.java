package airport.baggage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BaggageHandlerTest {
    @TempDir
    Path dir;

    private BaggageRepository repositoryOnDisk() {
        return new BaggageRepository(dir.resolve("baggage.csv"));
    }

    private BaggageHandler handler() {
        return new BaggageHandler(repositoryOnDisk(), new SecurityScreening());
    }

    private void assertSavedStatus(String itemId, BaggageStatus expected) {
        BaggageItem saved = repositoryOnDisk().findById(itemId).orElseThrow();
        assertEquals(expected, saved.getStatus());
    }

    @Test
    void acceptingSavesTheItem() {
        BaggageItem item = handler().accept("owner-1", 12.5, "  clothes  ");

        assertEquals(BaggageStatus.ACCEPTED, item.getStatus());
        assertEquals("clothes", item.getContents());
        assertSavedStatus(item.getId(), BaggageStatus.ACCEPTED);
    }

    @Test
    void badOwnerIsRejectedAndNothingIsSaved() {
        assertThrows(IllegalArgumentException.class, () -> handler().accept(null, 5, "clothes"));
        assertThrows(IllegalArgumentException.class, () -> handler().accept("", 5, "clothes"));
        assertThrows(IllegalArgumentException.class, () -> handler().accept("bad owner", 5, "clothes"));
        assertThrows(IllegalArgumentException.class, () -> handler().accept("a".repeat(37), 5, "clothes"));
        assertTrue(repositoryOnDisk().findAll().isEmpty());
    }

    @Test
    void weightLimitsAreChecked() {
        assertEquals(30.0, handler().accept("owner-1", 30.0, "clothes").getWeightKg());
        assertThrows(IllegalArgumentException.class, () -> handler().accept("owner-1", 30.1, "clothes"));
        assertThrows(IllegalArgumentException.class, () -> handler().accept("owner-1", 0, "clothes"));
        assertThrows(IllegalArgumentException.class, () -> handler().accept("owner-1", -1, "clothes"));
        assertThrows(IllegalArgumentException.class, () -> handler().accept("owner-1", Double.NaN, "clothes"));
        assertEquals(1, repositoryOnDisk().findAll().size());
    }

    @Test
    void badContentsAreRejectedAndNothingIsSaved() {
        assertThrows(IllegalArgumentException.class, () -> handler().accept("owner-1", 5, null));
        assertThrows(IllegalArgumentException.class, () -> handler().accept("owner-1", 5, "   "));
        assertThrows(IllegalArgumentException.class, () -> handler().accept("owner-1", 5, "bad;contents"));
        assertThrows(IllegalArgumentException.class, () -> handler().accept("owner-1", 5, "bad\ncontents"));
        assertThrows(IllegalArgumentException.class, () -> handler().accept("owner-1", 5, "a".repeat(101)));
        assertTrue(repositoryOnDisk().findAll().isEmpty());
    }

    @Test
    void screeningCleanBaggageClearsIt() {
        BaggageItem item = handler().accept("owner-1", 5, "laptop, clothes");

        BaggageItem screened = handler().screen(item.getId());

        assertEquals(BaggageStatus.CLEARED, screened.getStatus());
        assertSavedStatus(item.getId(), BaggageStatus.CLEARED);
    }

    @Test
    void screeningBannedBaggageRejectsIt() {
        BaggageItem item = handler().accept("owner-1", 5, "clothes and a knife");

        BaggageItem screened = handler().screen(item.getId());

        assertEquals(BaggageStatus.REJECTED, screened.getStatus());
        assertSavedStatus(item.getId(), BaggageStatus.REJECTED);
    }

    @Test
    void clearedBaggageCanBeReleased() {
        BaggageItem item = handler().accept("owner-1", 5, "clothes");
        handler().screen(item.getId());

        BaggageItem released = handler().release(item.getId());

        assertEquals(BaggageStatus.RELEASED, released.getStatus());
        assertSavedStatus(item.getId(), BaggageStatus.RELEASED);
    }

    @Test
    void rejectedBaggageCannotBeReleased() {
        BaggageItem item = handler().accept("owner-1", 5, "a knife");
        handler().screen(item.getId());

        assertThrows(IllegalStateException.class, () -> handler().release(item.getId()));
        assertSavedStatus(item.getId(), BaggageStatus.REJECTED);
    }

    @Test
    void clearedBaggageCanBeReturnedToOwner() {
        BaggageItem item = handler().accept("owner-1", 5, "clothes");
        handler().screen(item.getId());

        BaggageItem returned = handler().returnToOwner(item.getId());

        assertEquals(BaggageStatus.RETURNED, returned.getStatus());
        assertSavedStatus(item.getId(), BaggageStatus.RETURNED);
    }

    @Test
    void returningTwiceFails() {
        BaggageItem item = handler().accept("owner-1", 5, "clothes");
        handler().returnToOwner(item.getId());

        assertThrows(IllegalStateException.class, () -> handler().returnToOwner(item.getId()));
    }

    @Test
    void unknownBaggageFailsEverywhere() {
        assertThrows(IllegalArgumentException.class, () -> handler().screen("missing"));
        assertThrows(IllegalArgumentException.class, () -> handler().release("missing"));
        assertThrows(IllegalArgumentException.class, () -> handler().returnToOwner("missing"));
    }
}