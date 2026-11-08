package dev.sigmachan.companions.client;

import dev.sigmachan.companions.CompanionProfile;
import dev.sigmachan.companions.entity.CompanionEntity;
import dev.sigmachan.companions.registry.ModRegistry;
import dev.sigmachan.companions.relationship.Gesture;
import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;

import java.util.Map;

/** One shared rig with profile-specific geometry hidden/shown per entity; distinct hair, bows and proportions. */
public class CompanionModel extends EntityModel<CompanionEntity> {
    public static final EntityModelLayer LAYER = new EntityModelLayer(ModRegistry.id("companion"), "main");
    private final Map<String, ModelPart> parts;
    private final ModelPart head, torso, leftArm, rightArm, leftLeg, rightLeg, snack, cup;

    public CompanionModel(ModelPart root) {
        parts = CompanionModelData.bind(root);
        head = parts.get("head"); torso = parts.get("torso");
        leftArm = parts.get("left_arm"); rightArm = parts.get("right_arm");
        leftLeg = parts.get("left_leg"); rightLeg = parts.get("right_leg");
        snack = parts.get("snack"); cup = parts.get("cup");
    }
    public static TexturedModelData getTexturedModelData() { return CompanionModelData.build(); }

    @Override public void setAngles(CompanionEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        CompanionModelData.reset(parts);
        String prefix = entity.profile().id() + "_";
        for (var entry : parts.entrySet()) {
            String name = entry.getKey();
            boolean profilePart = name.startsWith("konata_") || name.startsWith("kagami_") || name.startsWith("tsukasa_");
            entry.getValue().visible = !profilePart || name.startsWith(prefix);
        }
        snack.visible = false; cup.visible = false;
        head.yaw = headYaw * MathHelper.RADIANS_PER_DEGREE;
        head.pitch = headPitch * MathHelper.RADIANS_PER_DEGREE;
        float walk = MathHelper.cos(limbAngle * 0.6662f) * Math.min(1, limbDistance) * 0.65f;
        leftLeg.pitch = walk; rightLeg.pitch = -walk;
        leftArm.pitch = -walk * 0.7f; rightArm.pitch = walk * 0.7f;
        leftArm.roll = 0.04f + MathHelper.sin(animationProgress * 0.045f) * 0.025f;
        rightArm.roll = -leftArm.roll;
        torso.pitch = MathHelper.sin(animationProgress * 0.05f) * 0.015f;

        ModelPart hairBack = parts.get(prefix + "hair_back"), hairLeft = parts.get(prefix + "hair_left"), hairRight = parts.get(prefix + "hair_right");
        if (hairBack != null) hairBack.pitch = 0.04f + MathHelper.sin(animationProgress * 0.07f) * 0.025f + Math.min(1, limbDistance) * 0.08f;
        if (hairLeft != null) hairLeft.roll = MathHelper.sin(animationProgress * 0.06f) * 0.02f;
        if (hairRight != null) hairRight.roll = hairLeft == null ? 0 : -hairLeft.roll;
        ModelPart tailLeft = parts.get(prefix + "tail_left"), tailRight = parts.get(prefix + "tail_right");
        if (tailLeft != null) tailLeft.roll = 0.08f + MathHelper.sin(animationProgress * 0.08f) * 0.05f + walk * 0.08f;
        if (tailRight != null) tailRight.roll = -0.08f - MathHelper.sin(animationProgress * 0.08f) * 0.05f - walk * 0.08f;
        ModelPart ahoge = parts.get(prefix + "ahoge");
        if (ahoge != null) ahoge.roll = MathHelper.sin(animationProgress * 0.1f) * 0.12f;

        Gesture gesture = entity.gesture();
        int remaining = entity.gestureTicks();
        float progress = gesture.duration() == 0 ? 0 : MathHelper.clamp(1.0f - remaining / (float) gesture.duration(), 0, 1);
        float env = MathHelper.sin(progress * MathHelper.PI);
        float sit = entity.mode() == CompanionEntity.Mode.STAY ? 0.3f : 0;
        if (gesture != Gesture.NONE && remaining > 0) {
            switch (gesture) {
                case WAVE -> { rightArm.pitch = -2.5f * env; rightArm.roll = MathHelper.sin(animationProgress * 0.55f) * 0.45f * env; head.roll = -0.08f * env; }
                case HIGH_FIVE -> { rightArm.pitch = -2.1f * env; rightArm.roll = -0.18f * env; torso.pitch = -0.06f * env; head.pitch -= 0.08f * env; }
                case HUG -> { leftArm.pitch = -1.35f * env; rightArm.pitch = -1.35f * env; leftArm.yaw = -0.45f * env; rightArm.yaw = 0.45f * env; torso.pitch = -0.1f * env; head.roll = 0.07f * env; }
                case PET -> { head.pitch += 0.24f * env; head.roll = 0.12f * env; leftArm.roll = 0.12f * env; rightArm.roll = -0.12f * env; }
                case COMFORT -> { rightArm.pitch = -1.75f * env; rightArm.yaw = -0.2f * env; head.roll = -0.1f * env; torso.pitch = -0.05f * env; }
                case EAT -> { snack.visible = true; rightArm.pitch = (-1.45f + MathHelper.sin(progress * MathHelper.PI * 6) * 0.16f) * env; head.pitch += 0.1f * env; }
                case DRINK -> { cup.visible = true; rightArm.pitch = (-1.8f + MathHelper.sin(progress * MathHelper.PI * 4) * 0.1f) * env; head.pitch -= 0.12f * env; }
                case REST -> { sit = 1; head.pitch += 0.06f; leftArm.pitch = -0.35f; rightArm.pitch = -0.35f; }
                default -> {}
            }
        }
        if (sit > 0) {
            leftLeg.pitch = -1.35f * sit; rightLeg.pitch = -1.35f * sit;
            head.pivotY += 6 * sit; torso.pivotY += 6 * sit; leftArm.pivotY += 6 * sit; rightArm.pivotY += 6 * sit;
            if (gesture != Gesture.REST) { leftArm.pitch = -0.3f * sit; rightArm.pitch = -0.3f * sit; }
        }
    }

    @Override public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, int color) {
        head.render(matrices, vertices, light, overlay, color);
        torso.render(matrices, vertices, light, overlay, color);
        leftArm.render(matrices, vertices, light, overlay, color); rightArm.render(matrices, vertices, light, overlay, color);
        leftLeg.render(matrices, vertices, light, overlay, color); rightLeg.render(matrices, vertices, light, overlay, color);
    }
}