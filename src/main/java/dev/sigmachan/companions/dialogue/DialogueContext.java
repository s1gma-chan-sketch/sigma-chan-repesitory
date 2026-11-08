package dev.sigmachan.companions.dialogue;

/** Snapshot taken on the server thread before any optional external request. */
public record DialogueContext(String biome, boolean night, boolean raining, String needs, String heldItem,
                              String activity, int nearbyCompanions, int turn) {
    public DialogueContext(String biome, boolean night, int turn) {
        this(biome, night, false, "", "", "NONE", 0, turn);
    }
}