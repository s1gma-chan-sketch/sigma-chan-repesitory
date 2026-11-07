package dev.sigmachan.companions.client;

import dev.sigmachan.companions.CompanionProfile;
import dev.sigmachan.companions.network.ActionC2S;
import dev.sigmachan.companions.network.CompanionStateS2C;
import dev.sigmachan.companions.network.DialogueS2C;
import dev.sigmachan.companions.registry.ModRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class SigmaCompanionsClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        EntityModelLayerRegistry.registerModelLayer(CompanionModel.LAYER, CompanionModel::getTexturedModelData);
        EntityRendererRegistry.register(ModRegistry.COMPANION, CompanionRenderer::new);
        ClientPlayNetworking.registerGlobalReceiver(CompanionStateS2C.ID, (payload, context) -> context.client().execute(() -> {
            if (context.client().currentScreen instanceof CompanionScreen screen && screen.entityId() == payload.entityId()) screen.update(payload);
            else context.client().setScreen(new CompanionScreen(payload));
        }));
        ClientPlayNetworking.registerGlobalReceiver(DialogueS2C.ID, (payload, context) -> context.client().execute(() -> {
            if (context.client().currentScreen instanceof CompanionScreen screen && screen.entityId() == payload.entityId()) screen.addReply(payload.message());
            else context.client().inGameHud.getChatHud().addMessage(Text.literal("[" + payload.speaker() + "] " + payload.message()));
        }));
        KeyBinding menu = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.sigma_companions.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, "category.sigma_companions"));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (menu.wasPressed()) {
                if (client.crosshairTarget instanceof net.minecraft.util.hit.EntityHitResult hit && hit.getEntity() instanceof dev.sigmachan.companions.entity.CompanionEntity companion && ClientPlayNetworking.canSend(ActionC2S.ID)) {
                    ClientPlayNetworking.send(new ActionC2S(companion.getId(), "open", ""));
                } else if (client.player != null) client.player.sendMessage(Text.literal("Наведи прицел на спутника рядом и нажми G, либо используй ПКМ / /sigma menu."), true);
            }
        });
    }
}