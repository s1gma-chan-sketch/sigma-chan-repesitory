package dev.sigmachan.companions;

import dev.sigmachan.companions.relationship.Bond;
import dev.sigmachan.companions.relationship.Interaction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RelationshipTest {
    @Test void trustIsClamped() {
        Bond bond = new Bond();
        bond.setTrust(500); assertEquals(100, bond.trust());
        bond.setTrust(-50); assertEquals(0, bond.trust());
        assertFalse(bond.addTrust(0));
    }
    @Test void romanceRequiresExplicitConsentAndTrust() {
        Bond bond = new Bond(); bond.setTrust(100);
        assertFalse(bond.startDate());
        bond.setRomanceConsent(true); bond.setTrust(54);
        assertFalse(bond.startDate());
        bond.addTrust(1); assertTrue(bond.startDate());
        assertFalse(bond.startDate());
    }
    @Test void marriageRequiresDatingAndCannotConsumeStateWithoutRingCheck() {
        Bond bond = new Bond(); bond.setTrust(100); bond.setRomanceConsent(true);
        assertFalse(bond.married()); assertFalse(bond.marry());
        assertTrue(bond.startDate()); assertTrue(bond.marry());
        assertEquals("Семья", bond.stage()); assertFalse(bond.marry());
    }
    @Test void withdrawingConsentEndsDatingButNotFriendship() {
        Bond bond = new Bond(); bond.setTrust(60); bond.setRomanceConsent(true); bond.startDate();
        bond.setRomanceConsent(false);
        assertFalse(bond.dating()); assertEquals(60, bond.trust());
    }
    @Test void separationResetsConsent() {
        Bond bond = new Bond(); bond.setTrust(100); bond.setRomanceConsent(true); bond.startDate(); bond.marry();
        bond.separate(); assertFalse(bond.married()); assertFalse(bond.romanceConsent()); assertEquals(40, bond.trust());
    }
    @Test void giftLimitResetsOnNewDay() {
        Bond bond = new Bond();
        for (int i = 0; i < 3; i++) { assertTrue(bond.canGift(7)); bond.recordGift(7); }
        assertFalse(bond.canGift(7)); assertThrows(IllegalStateException.class, () -> bond.recordGift(7));
        assertTrue(bond.canGift(8)); bond.recordGift(8); assertEquals(1, bond.giftsToday());
    }
    @Test void memoriesAreBoundedAndUnmodifiable() {
        Bond bond = new Bond();
        for (int i = 0; i < 20; i++) bond.remember("memory " + i);
        assertEquals(6, bond.memories().size()); assertEquals("memory 19", bond.memories().get(5));
        assertThrows(UnsupportedOperationException.class, () -> bond.memories().add("bad"));
        bond.remember("memory 19"); assertEquals(6, bond.memories().size());
    }
    @Test void unknownActionsAreRejected() {
        assertTrue(Interaction.parse("execute_shell").isEmpty());
        assertTrue(Interaction.parse("HUG").isPresent());
        assertTrue(Interaction.parse(null).isEmpty());
    }
    @Test void negativeWorldTimeDoesNotBreakGiftCounter() {
        Bond restored = new Bond(); restored.restoreGiftCounter(Long.MIN_VALUE, 0);
        assertTrue(restored.canGift(-1)); restored.recordGift(-1); assertEquals(1, restored.giftsToday());
    }
}