package airport.baggage;

import java.util.UUID;

public class BaggageItem {
    private final String id;
    private final String ownerRef;
    private final double weightKg;
    private final String contents;
    private BaggageStatus status;

    public BaggageItem(String id, String ownerRef, double weightKg, String contents, BaggageStatus status) {
        this.id = id;
        this.ownerRef = ownerRef;
        this.weightKg = weightKg;
        this.contents = contents;
        this.status = status;
    }

    public static BaggageItem createNew(String ownerRef, double weightKg, String contents) {
        String id = UUID.randomUUID().toString().substring(0, 8);
        return new BaggageItem(id, ownerRef, weightKg, contents, BaggageStatus.ACCEPTED);
    }

    public String getId() {
        return id;
    }

    public String getOwnerRef() {
        return ownerRef;
    }

    public double getWeightKg() {
        return weightKg;
    }

    public String getContents() {
        return contents;
    }

    public BaggageStatus getStatus() {
        return status;
    }

    public void clear() {
        moveTo(BaggageStatus.CLEARED);
    }

    public void reject() {
        moveTo(BaggageStatus.REJECTED);
    }

    public void release() {
        moveTo(BaggageStatus.RELEASED);
    }
    public void returnToOwner() {
        moveTo(BaggageStatus.RETURNED);
    }

    private void moveTo(BaggageStatus next) {
        if (!status.canMoveTo(next)) {
            throw new IllegalStateException("cannot move baggage from " + status + " to " + next);
        }
        status = next;
    }
}