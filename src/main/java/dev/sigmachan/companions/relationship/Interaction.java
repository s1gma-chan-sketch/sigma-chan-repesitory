package dev.sigmachan.companions.relationship;

import java.util.Locale;
import java.util.Optional;

public enum Interaction {
    TALK(0, 20), COMPLIMENT(0, 200), GIFT(0, 40), WAVE(0, 60),
    HIGH_FIVE(0, 60), HUG(Bond.FRIEND, 100), PET(Bond.FRIEND, 100), COMFORT(Bond.CLOSE, 160),
    SNACK(0, 40), DRINK(0, 40), REST(0, 200),
    FOLLOW(Bond.FRIEND, 20), STAY(Bond.FRIEND, 20), WANDER(Bond.FRIEND, 20),
    HOME(Bond.CLOSE, 40), AI_CONSENT(0, 40);

    private final int minimumTrust, cooldownTicks;
    Interaction(int minimumTrust, int cooldownTicks) { this.minimumTrust = minimumTrust; this.cooldownTicks = cooldownTicks; }
    public int minimumTrust() { return minimumTrust; }
    public int cooldownTicks() { return cooldownTicks; }
    public boolean ownerOnly() { return this == FOLLOW || this == STAY || this == WANDER || this == HOME; }
    public static Optional<Interaction> parse(String value) {
        try { return Optional.of(valueOf(value.toUpperCase(Locale.ROOT))); }
        catch (IllegalArgumentException | NullPointerException e) { return Optional.empty(); }
    }
}