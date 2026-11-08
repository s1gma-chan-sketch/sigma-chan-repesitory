package dev.sigmachan.companions;

import dev.sigmachan.companions.entity.CompanionNeeds;
import net.minecraft.nbt.NbtCompound;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CompanionNeedsTest {
    @Test void snackEffectIsDelayedAndNotExcretory() {
        CompanionNeeds needs = new CompanionNeeds();
        assertTrue(needs.offerSnack(24, 5));
        assertFalse(needs.offerSnack(24, 5));
        int before = needs.satiety();
        needs.tick(false); needs.tick(false); needs.tick(false); needs.tick(false);
        assertTrue(needs.busyDigesting()); assertFalse(needs.consumeDigestedNotice());
        assertEquals(before - 1, needs.satiety(), 1);
        needs.tick(false);
        assertFalse(needs.busyDigesting()); assertTrue(needs.consumeDigestedNotice()); assertFalse(needs.consumeDigestedNotice());
        assertTrue(needs.satiety() > before);
        assertTrue(needs.hydration() < 80);
    }
    @Test void drinkAndRestRecoverWithoutExceedingBounds() {
        CompanionNeeds needs = new CompanionNeeds();
        for (int i = 0; i < 2000; i++) needs.tick(true);
        assertEquals(0, needs.satiety()); assertEquals(0, needs.hydration()); assertEquals(0, needs.energy());
        needs.drink(); needs.finishRest();
        assertEquals(35, needs.hydration()); assertEquals(20, needs.energy());
        for (int i = 0; i < 10; i++) { needs.drink(); needs.finishRest(); }
        assertEquals(100, needs.hydration()); assertEquals(100, needs.energy());
    }
    @Test void nbtRoundTripPreservesPendingDigestion() {
        CompanionNeeds needs = new CompanionNeeds(); needs.offerSnack(18, 90); needs.tick(false);
        NbtCompound tag = new NbtCompound(); needs.write(tag);
        CompanionNeeds restored = new CompanionNeeds(); restored.read(tag);
        assertEquals(needs.satiety(), restored.satiety()); assertEquals(needs.hydration(), restored.hydration());
        assertEquals(needs.energy(), restored.energy()); assertEquals(89, restored.digestionSeconds());
    }
    @Test void hintsPrioritiseWaterThenRestThenFood() {
        CompanionNeeds needs = new CompanionNeeds();
        assertNull(needs.hint());
        NbtCompound tag = new NbtCompound(); tag.putDouble("Hydration", 20); tag.putDouble("Energy", 20); tag.putDouble("Satiety", 20);
        needs.read(tag); assertTrue(needs.hint().contains("воды"));
        tag.putDouble("Hydration", 80); needs.read(tag); assertTrue(needs.hint().contains("отдохнуть"));
        tag.putDouble("Energy", 80); needs.read(tag); assertTrue(needs.hint().contains("перекуса"));
    }
}