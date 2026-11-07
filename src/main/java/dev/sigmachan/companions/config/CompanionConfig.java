package dev.sigmachan.companions.config;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/** Server-owned configuration. Never serialize API keys or send this object to clients. */
public final class CompanionConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("sigma_companions/config");
    public boolean aiEnabled = false;
    public String provider = "local";
    public String openaiModel = "gpt-5.4-mini";
    public String anthropicModel = "claude-sonnet-4-6";
    public int timeoutSeconds = 12;
    public int requestsPerMinute = 6;
    public int maxCompanionsPerPlayer = 8;
    public boolean ambientDialogue = true;

    public void validate() {
        if (provider == null) provider = "local";
        provider = provider.toLowerCase(Locale.ROOT);
        if (!provider.equals("local") && !provider.equals("openai") && !provider.equals("anthropic")) {
            LOGGER.warn("Unknown AI provider; using local dialogue");
            provider = "local";
        }
        if (openaiModel == null || openaiModel.isBlank()) openaiModel = "gpt-5.4-mini";
        if (anthropicModel == null || anthropicModel.isBlank()) anthropicModel = "claude-sonnet-4-6";
        timeoutSeconds = Math.clamp(timeoutSeconds, 3, 30);
        requestsPerMinute = Math.clamp(requestsPerMinute, 1, 30);
        maxCompanionsPerPlayer = Math.clamp(maxCompanionsPerPlayer, 1, 32);
    }

    public static CompanionConfig load(Path directory) {
        Path file = directory.resolve("sigma-companions.json");
        CompanionConfig config = new CompanionConfig();
        try {
            Files.createDirectories(directory);
            if (Files.exists(file)) {
                CompanionConfig parsed = new GsonBuilder().create().fromJson(Files.readString(file, StandardCharsets.UTF_8), CompanionConfig.class);
                if (parsed != null) config = parsed;
            } else {
                Files.writeString(file, new GsonBuilder().setPrettyPrinting().create().toJson(config), StandardCharsets.UTF_8);
            }
        } catch (IOException | JsonParseException e) {
            LOGGER.warn("Could not read {}. External AI disabled; local dialogue remains available.", file.getFileName());
            config = new CompanionConfig();
        }
        config.validate();
        return config;
    }
}