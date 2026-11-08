package dev.sigmachan.companions;

import dev.sigmachan.companions.ai.DialogueService;
import dev.sigmachan.companions.command.CompanionCommands;
import dev.sigmachan.companions.config.CompanionConfig;
import dev.sigmachan.companions.entity.CompanionEntity;
import dev.sigmachan.companions.network.ModNetworking;
import dev.sigmachan.companions.registry.ModRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SigmaCompanions implements ModInitializer {
    public static final String MOD_ID = "sigma_companions";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static CompanionConfig config = new CompanionConfig();
    private static DialogueService dialogue;
    public static CompanionConfig config() { return config; }
    public static DialogueService ai() { return dialogue; }

    @Override public void onInitialize() {
        ModRegistry.initialize();
        ModNetworking.initialize();
        CompanionCommands.register();
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            config = CompanionConfig.load(FabricLoader.getInstance().getConfigDir());
            CompanionEntity.SigmaHolder.setAmbient(config.ambientDialogue);
            dialogue = new DialogueService(config, server);
            LOGGER.info("Sigma Companions ready. External AI: {} / {}. Keys are read only from server environment.", config.aiEnabled, config.provider);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> { if (dialogue != null) dialogue.forget(handler.player.getUuid()); });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> { if (dialogue != null) dialogue.close(); dialogue = null; });
        LOGGER.info("Sigma Companions initialized: Lucky Star fan companions, friendship-only interactions and abstract needs.");
    }
}