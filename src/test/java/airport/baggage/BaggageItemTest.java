package airport.baggage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BaggageItemTest {

    @Test
    void newItemStartsAsAccepted() {
        BaggageItem item = BaggageItem.createNew("owner-1", 12.5, "clothes");

        assertEquals(BaggageStatus.ACCEPTED, item.getStatus());
        assertEquals(8, item.getId().length());
        assertEquals("owner-1", item.getOwnerRef());
        assertEquals(12.5, item.getWeightKg());
        assertEquals("clothes", item.getContents());
    }

    @Test
    void acceptedItemCanBeCleared() {
        BaggageItem item = BaggageItem.createNew("owner-1", 5, "clothes");

        item.clear();

        assertEquals(BaggageStatus.CLEARED, item.getStatus());
    }

    @Test
    void acceptedItemCanBeRejected() {
        BaggageItem item = BaggageItem.createNew("owner-1", 5, "clothes");

        item.reject();

        assertEquals(BaggageStatus.REJECTED, item.getStatus());
    }

    @Test
    void acceptedItemCanBeReleased() {
        BaggageItem item = BaggageItem.createNew("owner-1", 5, "clothes");

        item.release();

        assertEquals(BaggageStatus.RELEASED, item.getStatus());
    }

    @Test
    void clearedItemCanBeReleased() {
        BaggageItem item = BaggageItem.createNew("owner-1", 5, "clothes");
        item.clear();

        item.release();

        assertEquals(BaggageStatus.RELEASED, item.getStatus());
    }

    @Test
    void acceptedItemCanBeReturnedToOwner() {
        BaggageItem item = BaggageItem.createNew("owner-1", 5, "clothes");

        item.returnToOwner();

        assertEquals(BaggageStatus.RETURNED, item.getStatus());
    }

    @Test
    void clearedItemCanBeReturnedToOwner() {
        BaggageItem item = BaggageItem.createNew("owner-1", 5, "clothes");
        item.clear();

        item.returnToOwner();

        assertEquals(BaggageStatus.RETURNED, item.getStatus());
    }

    @Test
    void rejectedItemCannotMoveAnywhere() {
        BaggageItem item = BaggageItem.createNew("owner-1", 5, "clothes");
        item.reject();

        assertThrows(IllegalStateException.class, item::clear);
        assertThrows(IllegalStateException.class, item::release);
        assertThrows(IllegalStateException.class, item::returnToOwner);
        assertEquals(BaggageStatus.REJECTED, item.getStatus());
    }

    @Test
    void releasedItemCannotBeReturned() {
        BaggageItem item = BaggageItem.createNew("owner-1", 5, "clothes");
        item.release();

        assertThrows(IllegalStateException.class, item::returnToOwner);
        assertEquals(BaggageStatus.RELEASED, item.getStatus());
    }

    @Test
    void returnedItemCannotMoveAnywhere() {
        BaggageItem item = BaggageItem.createNew("owner-1", 5, "clothes");
        item.returnToOwner();

        assertThrows(IllegalStateException.class, item::clear);
        assertThrows(IllegalStateException.class, item::returnToOwner);
        assertEquals(BaggageStatus.RETURNED, item.getStatus());
    }

    @Test
    void itemRestoredFromStorageKeepsItsData() {
        BaggageItem item = new BaggageItem("abc12345", "owner-1", 7.5, "books", BaggageStatus.CLEARED);

        assertEquals("abc12345", item.getId());
        assertEquals("owner-1", item.getOwnerRef());
        assertEquals(7.5, item.getWeightKg());
        assertEquals("books", item.getContents());
        assertEquals(BaggageStatus.CLEARED, item.getStatus());
    }
}