package airport.baggage;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public class SecurityScreening {
    private static final Set<String> BANNED = Set.of(
            "knife", "knives", "gun", "pistol", "explosives", "fireworks", "gasoline",
            "нож", "пистолет", "бензин", "фейерверк");

    public Optional<String> findBannedItem(String contents) {
        String[] words = contents.toLowerCase(Locale.ROOT).split("[^\\p{L}]+");
        for (String word : words) {
            if (BANNED.contains(word)) {
                return Optional.of(word);
            }
        }
        return Optional.empty();
    }
}