package dev.sigmachan.companions;

import dev.sigmachan.companions.dialogue.LocalDialogue;
import dev.sigmachan.companions.relationship.Bond;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LocalDialogueTest {
    @Test void russianDialogueWorksOffline() {
        String reply = LocalDialogue.reply(CompanionProfile.MIO, new Bond(), "Привет, кто ты?", "library", false, 1);
        assertTrue(reply.contains("Мио")); assertTrue(reply.contains("26"));
    }
    @Test void explicitMemoryIsStored() {
        Bond bond = new Bond();
        LocalDialogue.reply(CompanionProfile.AKARI, bond, "запомни я люблю сады", "plains", false, 1);
        assertEquals("я люблю сады", bond.memories().get(0));
        assertTrue(LocalDialogue.reply(CompanionProfile.AKARI, bond, "Что помнишь?", "plains", false, 2).contains("сады"));
    }
    @Test void romanceReplyRespectsRelationship() {
        Bond bond = new Bond();
        assertTrue(LocalDialogue.reply(CompanionProfile.REI, bond, "хочу свидание", "plains", false, 1).contains("доверие"));
    }
}