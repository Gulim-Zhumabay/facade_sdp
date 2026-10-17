package airport.baggage;

import java.util.Optional;

public class BaggageHandler {
    private static final double MAX_WEIGHT_KG = 30.0;
    private static final String OWNER_PATTERN = "[A-Za-z0-9\\-]{1,36}";
    private static final String CONTENTS_PATTERN = "[^;\\r\\n]{1,100}";

    private final BaggageRepository repository;
    private final SecurityScreening screening;

    public BaggageHandler(BaggageRepository repository, SecurityScreening screening) {
        this.repository = repository;
        this.screening = screening;
    }

    public BaggageItem accept(String ownerRef, double weightKg, String contents) {
        if (ownerRef == null || !ownerRef.matches(OWNER_PATTERN)) {
            throw new IllegalArgumentException("owner reference must be 1-36 letters, digits or hyphens");
        }
        if (!(weightKg > 0) || weightKg > MAX_WEIGHT_KG) {
            throw new IllegalArgumentException("baggage weight must be above 0 and at most " + MAX_WEIGHT_KG + " kg");
        }
        if (contents == null || contents.isBlank() || !contents.matches(CONTENTS_PATTERN)) {
            throw new IllegalArgumentException("contents must be 1-100 characters without ';' or line breaks");
        }
        BaggageItem item = BaggageItem.createNew(ownerRef, weightKg, contents.trim());
        repository.save(item);
        return item;
    }

    public BaggageItem screen(String itemId) {
        BaggageItem item = find(itemId);
        Optional<String> banned = screening.findBannedItem(item.getContents());
        if (banned.isPresent()) {
            item.reject();
        } else {
            item.clear();
        }
        repository.save(item);
        return item;
    }

    public BaggageItem release(String itemId) {
        BaggageItem item = find(itemId);
        item.release();
        repository.save(item);
        return item;
    }

    private BaggageItem find(String itemId) {
        return repository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("baggage not found: " + itemId));
    }
}