package ru.sigmachan.luckystar.client;

import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import ru.sigmachan.luckystar.CommonProxy;
import ru.sigmachan.luckystar.LuckyStarMod;
import ru.sigmachan.luckystar.entity.StarFriend;

public class ClientProxy extends CommonProxy {
    @Override public void registerRenderers() {
        RenderingRegistry.registerEntityRenderingHandler(StarFriend.Konata.class,
                manager -> new FriendRenderer<>(manager, "konata", 0));
        RenderingRegistry.registerEntityRenderingHandler(StarFriend.Kagami.class,
                manager -> new FriendRenderer<>(manager, "kagami", 1));
        RenderingRegistry.registerEntityRenderingHandler(StarFriend.Tsukasa.class,
                manager -> new FriendRenderer<>(manager, "tsukasa", 2));
        RenderingRegistry.registerEntityRenderingHandler(StarFriend.Miyuki.class,
                manager -> new FriendRenderer<>(manager, "miyuki", 3));
    }

    private static class FriendRenderer<T extends StarFriend> extends RenderLiving<T> {
        private final ResourceLocation texture;
        FriendRenderer(net.minecraft.client.renderer.entity.RenderManager manager, String name, int hairstyle) {
            super(manager, new FriendModel(hairstyle), 0.4F);
            texture = new ResourceLocation(LuckyStarMod.ID, "textures/entity/" + name + ".png");
        }
        @Override protected ResourceLocation getEntityTexture(T entity) { return texture; }
    }
}
