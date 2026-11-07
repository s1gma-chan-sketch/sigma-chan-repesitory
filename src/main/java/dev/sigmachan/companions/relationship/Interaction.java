package dev.sigmachan.companions.relationship;

import java.util.Locale;
import java.util.Optional;

public enum Interaction {
    TALK(0, 40), COMPLIMENT(0, 200), GIFT(0, 40),
    HUG(Bond.FRIEND, 100), PET(Bond.FRIEND, 100), COMFORT(Bond.CLOSE, 200),
    CONSENT(0, 40), DATE(Bond.ROMANCE, 200), KISS(Bond.ROMANCE, 100),
    MARRY(Bond.MARRIAGE, 100), SEPARATE(0, 100),
    FOLLOW(Bond.FRIEND, 20), STAY(Bond.FRIEND, 20), WANDER(Bond.FRIEND, 20),
    HOME(Bond.CLOSE, 40), AI_CONSENT(0, 40);

    private final int minimumTrust;
    private final int cooldownTicks;
    Interaction(int minimumTrust, int cooldownTicks) {
        this.minimumTrust = minimumTrust;
        this.cooldownTicks = cooldownTicks;
    }
    public int minimumTrust() { return minimumTrust; }
    public int cooldownTicks() { return cooldownTicks; }
    public static Optional<Interaction> parse(String value) {
        try { return Optional.of(valueOf(value.toUpperCase(Locale.ROOT))); }
        catch (IllegalArgumentException | NullPointerException e) { return Optional.empty(); }
    }
}