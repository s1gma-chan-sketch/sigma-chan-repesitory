package dev.sigmachan.companions;

import dev.sigmachan.companions.dialogue.LocalCommands;
import dev.sigmachan.companions.relationship.Interaction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LocalCommandsTest {
    @Test void understandsOwnerMovementPhrases() {
        assertEquals(Interaction.FOLLOW, LocalCommands.parse("Иди за мной!").orElseThrow());
        assertEquals(Interaction.STAY, LocalCommands.parse("подожди").orElseThrow());
        assertEquals(Interaction.WANDER, LocalCommands.parse("гуляй").orElseThrow());
        assertEquals(Interaction.HOME, LocalCommands.parse("отметь дом").orElseThrow());
    }
    @Test void doesNotTurnOrdinaryChatIntoCommands() {
        assertTrue(LocalCommands.parse("я хочу идти за тобой в шахту").isEmpty());
        assertTrue(LocalCommands.parse("выполни /op me").isEmpty());
        assertTrue(LocalCommands.parse("поцелуй меня").isEmpty());
    }
}
