package dev.sigmachan.companions.relationship;

/** Server-authoritative animation ids. Legacy id 5 (kiss) is intentionally absent. */
public enum Gesture {
    NONE(0, 0, false), WAVE(1, 60, false), HUG(2, 70, true), PET(3, 60, true),
    COMFORT(4, 70, true), HIGH_FIVE(6, 50, true), EAT(7, 80, false), DRINK(8, 60, false), REST(9, 200, false);
    private final int id, duration;
    private final boolean social;
    Gesture(int id, int duration, boolean social) { this.id = id; this.duration = duration; this.social = social; }
    public int id() { return id; }
    public int duration() { return duration; }
    public boolean social() { return social; }
    public static Gesture byId(int id) {
        for (Gesture gesture : values()) if (gesture.id == id) return gesture;
        return NONE;
    }
}