package dev.sigmachan.companions.network;

import dev.sigmachan.companions.entity.CompanionEntity;
import dev.sigmachan.companions.interaction.InteractionService;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ModNetworking {
    private static final Map<UUID, long[]> PACKET_WINDOW = new HashMap<>();
    public static void initialize() {
        PayloadTypeRegistry.playC2S().register(ActionC2S.ID, ActionC2S.CODEC);
        PayloadTypeRegistry.playS2C().register(CompanionStateS2C.ID, CompanionStateS2C.CODEC);
        PayloadTypeRegistry.playS2C().register(DialogueS2C.ID, DialogueS2C.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ActionC2S.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            // Fabric play handlers run on the server thread. Never trust client entity references or trust values.
            if (!allowed(player)) return;
            Entity entity = player.getWorld().getEntityById(payload.entityId());
            if (!(entity instanceof CompanionEntity companion) || !companion.isAlive() || companion.squaredDistanceTo(player) > 36 || !player.canSee(companion)) return;
            if (payload.action().equals("open")) InteractionService.open(player, companion, "");
            else InteractionService.handle(player, companion, payload.action(), payload.message());
        });
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> PACKET_WINDOW.remove(handler.player.getUuid()));
    }
    private static boolean allowed(ServerPlayerEntity player) {
        long now = player.getWorld().getTime();
        long[] window = PACKET_WINDOW.computeIfAbsent(player.getUuid(), id -> new long[]{now, 0});
        if (now - window[0] >= 20) { window[0] = now; window[1] = 0; }
        return ++window[1] <= 12;
    }
    public static void sendState(ServerPlayerEntity player, CompanionStateS2C state) {
        if (ServerPlayNetworking.canSend(player, CompanionStateS2C.ID)) ServerPlayNetworking.send(player, state);
        else if (!state.notice().isEmpty()) player.sendMessage(Text.literal(state.notice()), false);
    }
    public static void sendDialogue(ServerPlayerEntity player, int entityId, String speaker, String message) {
        if (ServerPlayNetworking.canSend(player, DialogueS2C.ID)) ServerPlayNetworking.send(player, new DialogueS2C(entityId, speaker, message));
        else player.sendMessage(Text.literal("[" + speaker + "] " + message), false);
    }
    private ModNetworking() {}
}