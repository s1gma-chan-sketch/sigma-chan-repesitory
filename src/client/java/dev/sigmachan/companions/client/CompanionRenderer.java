package dev.sigmachan.companions.client;

import dev.sigmachan.companions.entity.CompanionEntity;
import dev.sigmachan.companions.registry.ModRegistry;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class CompanionRenderer extends MobEntityRenderer<CompanionEntity, CompanionModel> {
    public CompanionRenderer(EntityRendererFactory.Context context) { super(context, new CompanionModel(context.getPart(CompanionModel.LAYER)), 0.3f); }
    @Override public Identifier getTexture(CompanionEntity entity) { return ModRegistry.id("textures/entity/" + entity.profile().id() + ".png"); }
    @Override protected void scale(CompanionEntity entity, MatrixStack matrices, float tickDelta) {
        float scale = entity.profile().scale();
        matrices.scale(scale, scale, scale);
    }
}