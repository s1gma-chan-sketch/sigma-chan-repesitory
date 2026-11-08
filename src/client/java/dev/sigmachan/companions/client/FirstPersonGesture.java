package dev.sigmachan.companions.client;

import dev.sigmachan.companions.relationship.Gesture;
import net.minecraft.util.math.MathHelper;

/** Client-only visual feedback for the interacting player's own hand. Never changes server state. */
public final class FirstPersonGesture {
    private static Gesture gesture = Gesture.NONE;
    private static int remaining, duration;
    private FirstPersonGesture() {}
    public static void start(int id, int ticks) {
        gesture = Gesture.byId(id); duration = Math.clamp(ticks, 1, 200); remaining = duration;
        if (!gesture.social()) reset();
    }
    public static void tick() { if (remaining > 0 && --remaining == 0) reset(); }
    public static void reset() { gesture = Gesture.NONE; remaining = 0; duration = 0; }
    public static boolean active() { return remaining > 0 && gesture.social(); }
    public static Gesture gesture() { return gesture; }
    public static float envelope(float tickDelta) {
        if (!active()) return 0;
        float progress = MathHelper.clamp(1.0f - (remaining - tickDelta) / duration, 0, 1);
        return MathHelper.sin(progress * MathHelper.PI);
    }
}