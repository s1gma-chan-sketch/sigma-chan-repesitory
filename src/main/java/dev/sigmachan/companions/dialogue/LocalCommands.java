package dev.sigmachan.companions.dialogue;

import dev.sigmachan.companions.relationship.Interaction;

import java.util.Optional;

/** Deterministic, owner-only movement phrases. Never populated from external AI output. */
public final class LocalCommands {
    private LocalCommands() {}
    public static Optional<Interaction> parse(String message) {
        String text = DialogueIntent.normalize(message);
        if (text.matches("^(иди за мной|следуй за мной|следуй|за мной|follow me|follow)$")) return Optional.of(Interaction.FOLLOW);
        if (text.matches("^(подожди|стой|останься здесь|жди здесь|ждать|stay|wait)$")) return Optional.of(Interaction.STAY);
        if (text.matches("^(гуляй|погуляй|wander)$")) return Optional.of(Interaction.WANDER);
        if (text.matches("^(отметь дом|это дом|дом здесь|home here)$")) return Optional.of(Interaction.HOME);
        return Optional.empty();
    }
}
