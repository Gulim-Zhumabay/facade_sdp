package airport.baggage;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SecurityScreeningTest {
    private final SecurityScreening screening = new SecurityScreening();

    @Test
    void bannedWordIsFound() {
        assertEquals(Optional.of("knife"), screening.findBannedItem("clothes and a knife"));
    }

    @Test
    void capitalLettersDoNotHideBannedWord() {
        assertEquals(Optional.of("knife"), screening.findBannedItem("A KNIFE"));
    }

    @Test
    void punctuationDoesNotHideBannedWord() {
        assertEquals(Optional.of("fireworks"), screening.findBannedItem("books,fireworks!"));
    }

    @Test
    void russianBannedWordIsFound() {
        assertEquals(Optional.of("нож"), screening.findBannedItem("Одежда и Нож"));
    }

    @Test
    void cleanContentsGiveNothing() {
        assertEquals(Optional.empty(), screening.findBannedItem("laptop, clothes"));
    }

    @Test
    void bannedWordInsideAnotherWordIsNotFound() {
        assertEquals(Optional.empty(), screening.findBannedItem("knifeless"));
    }
}