package dev.sigmachan.companions.dialogue;

/** Snapshot taken on the server thread before any optional external request. */
public record DialogueContext(String biome, boolean night, boolean raining, String needs, String heldItem,
                              String activity, int nearbyCompanions, int turn,
                              String mode, String home, int playerHealth, int playerFood,
                              int torches, int foodItems, boolean hasWeapon, boolean hasShield,
                              int lightLevel, int nearbyHostiles) {
    public DialogueContext(String biome, boolean night, int turn) {
        this(biome, night, false, "", "", "NONE", 0, turn, "WANDER", "", 20, 20, 0, 0, false, false, 15, 0);
    }
    public DialogueContext(String biome, boolean night, boolean raining, String needs, String heldItem,
                           String activity, int nearbyCompanions, int turn) {
        this(biome, night, raining, needs, heldItem, activity, nearbyCompanions, turn, "WANDER", "", 20, 20, 0, 0, false, false, 15, 0);
    }
}
