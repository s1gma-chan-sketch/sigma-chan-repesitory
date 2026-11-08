package dev.sigmachan.companions.client.mixin;

import dev.sigmachan.companions.client.FirstPersonGesture;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds a small first-person hand motion during friendly gestures without cancelling vanilla rendering. */
@Environment(EnvType.CLIENT)
@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {
    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"))
    private void sigma$pushGesture(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress,
                                   ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        matrices.push();
        if (!FirstPersonGesture.active() || player.isUsingItem()) return;
        boolean main = hand == Hand.MAIN_HAND;
        if (!main && FirstPersonGesture.gesture() != dev.sigmachan.companions.relationship.Gesture.HUG) return;
        Arm arm = main ? player.getMainArm() : player.getMainArm().getOpposite();
        float side = arm == Arm.RIGHT ? 1 : -1;
        float env = FirstPersonGesture.envelope(tickDelta);
        switch (FirstPersonGesture.gesture()) {
            case WAVE -> { matrices.translate(side * 0.12f * env, 0.18f * env, -0.12f * env); matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * 24 * env)); }
            case HIGH_FIVE -> { matrices.translate(side * 0.05f * env, 0.3f * env, -0.28f * env); matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-38 * env)); }
            case HUG -> { matrices.translate(-side * 0.22f * env, 0.16f * env, -0.3f * env); matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-22 * env)); matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-side * 14 * env)); }
            case PET -> { matrices.translate(0, 0.22f * env, -0.34f * env); matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-18 * env)); }
            case COMFORT -> { matrices.translate(0, 0.1f * env, -0.2f * env); matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(10 * env)); }
            default -> {}
        }
    }
    @Inject(method = "renderFirstPersonItem", at = @At("RETURN"))
    private void sigma$popGesture(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress,
                                  ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        matrices.pop();
    }
}