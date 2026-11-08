package dev.sigmachan.companions;

import dev.sigmachan.companions.relationship.Bond;
import dev.sigmachan.companions.relationship.Gesture;
import dev.sigmachan.companions.relationship.Interaction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RelationshipTest {
    @Test void trustIsClampedAndFriendshipOnly() {
        Bond bond = new Bond();
        bond.setTrust(500); assertEquals(100, bond.trust()); assertEquals("Лучшие друзья", bond.stage());
        bond.setTrust(-50); assertEquals(0, bond.trust()); assertEquals("Знакомство", bond.stage());
        assertFalse(bond.addTrust(0));
        bond.setTrust(15); assertEquals("Друзья", bond.stage());
        bond.setTrust(35); assertEquals("Близкие друзья", bond.stage());
    }
    @Test void giftLimitResetsOnNewDay() {
        Bond bond = new Bond();
        for (int i = 0; i < 3; i++) { assertTrue(bond.canGift(7)); bond.recordGift(7); }
        assertFalse(bond.canGift(7)); assertThrows(IllegalStateException.class, () -> bond.recordGift(7));
        assertTrue(bond.canGift(8)); bond.recordGift(8); assertEquals(1, bond.giftsToday());
    }
    @Test void memoriesAreBoundedAndClearable() {
        Bond bond = new Bond();
        for (int i = 0; i < 20; i++) bond.remember("memory " + i);
        assertEquals(6, bond.memories().size()); assertEquals("memory 19", bond.memories().get(5));
        assertThrows(UnsupportedOperationException.class, () -> bond.memories().add("bad"));
        bond.clearMemories(); assertTrue(bond.memories().isEmpty());
    }
    @Test void unknownAndRomanticActionsAreRejected() {
        assertTrue(Interaction.parse("execute_shell").isEmpty());
        assertTrue(Interaction.parse("HUG").isPresent());
        assertTrue(Interaction.parse(null).isEmpty());
        for (String removed : new String[]{"kiss", "date", "marry", "separate", "consent"}) assertTrue(Interaction.parse(removed).isEmpty(), removed);
    }
    @Test void negativeWorldTimeDoesNotBreakGiftCounter() {
        Bond restored = new Bond(); restored.restoreGiftCounter(Long.MIN_VALUE, 0);
        assertTrue(restored.canGift(-1)); restored.recordGift(-1); assertEquals(1, restored.giftsToday());
    }
    @Test void topicAndRecentRepliesAreBounded() {
        Bond bond = new Bond(); bond.setTopic("GAMES"); bond.setTopic("GAMES"); bond.setTopic("FOOD");
        assertEquals("FOOD", bond.lastTopic()); assertEquals(1, bond.topicTurns());
        for (int i = 0; i < 10; i++) bond.rememberReply("r" + i);
        assertEquals(5, bond.recentReplies().size()); assertEquals("r9", bond.recentReplies().get(4));
    }
    @Test void legacyKissGestureIdIsNotAnimated() {
        assertEquals(Gesture.NONE, Gesture.byId(5));
        assertEquals(Gesture.HUG, Gesture.byId(2));
        assertTrue(Gesture.HUG.duration() > 0);
        assertFalse(Gesture.EAT.social());
    }
}