package dev.sigmachan.companions.client;

import dev.sigmachan.companions.CompanionProfile;
import dev.sigmachan.companions.entity.CompanionEntity;
import dev.sigmachan.companions.registry.ModRegistry;
import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;

/** Original volumetric anime models with independently animated hair and limbs. */
public class CompanionModel extends EntityModel<CompanionEntity> {
    public static final EntityModelLayer LAYER = new EntityModelLayer(ModRegistry.id("companion"), "main");
    private final ModelPart head, hairBack, hairLeft, hairRight, torso, leftArm, rightArm, leftLeg, rightLeg;

    public CompanionModel(ModelPart root) {
        head = root.getChild("head");
        hairBack = head.getChild("hair_back");
        hairLeft = head.getChild("hair_left");
        hairRight = head.getChild("hair_right");
        torso = root.getChild("torso");
        leftArm = root.getChild("left_arm"); rightArm = root.getChild("right_arm");
        leftLeg = root.getChild("left_leg"); rightLeg = root.getChild("right_leg");
    }
    public static TexturedModelData getTexturedModelData() {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();
        ModelPartData head = root.addChild("head", new ModelPartBuilder().uv(0, 0).cuboid(-4, -8, -4, 8, 8, 8), ModelTransform.NONE);
        head.addChild("hair_cap", cube(64, 0, -4.5f, -8.6f, -4.5f, 9, 4, 9), ModelTransform.NONE);
        head.addChild("fringe", cube(104, 0, -4.2f, -7.6f, -4.65f, 8, 3, 1), ModelTransform.NONE);
        head.addChild("hair_back", cube(0, 64, -4.5f, -5, 3.4f, 9, 13, 3), ModelTransform.NONE);
        head.addChild("hair_left", cube(32, 64, 3.6f, -5, -3, 2, 10, 5), ModelTransform.NONE);
        head.addChild("hair_right", cube(56, 64, -5.6f, -5, -3, 2, 10, 5), ModelTransform.NONE);
        head.addChild("ribbon", cube(104, 8, -6.4f, -6, 1, 2, 3, 3), ModelTransform.NONE);
        root.addChild("torso", cube(0, 16, -4, 0, -2.5f, 8, 11, 5), ModelTransform.NONE);
        root.addChild("left_arm", cube(32, 16, -1, -1, -2, 3, 11, 4), ModelTransform.pivot(5, 1, 0), ModelTransform.NONE);
        root.addChild("right_arm", cube(48, 16, -2, -1, -2, 3, 11, 4), ModelTransform.pivot(-5, 1, 0), ModelTransform.NONE);
        root.addChild("left_leg", cube(64, 32, -2, 0, -2, 4, 13, 4), ModelTransform.pivot(2, 11, 0), ModelTransform.NONE);
        root.addChild("right_leg", cube(80, 32, -2, 0, -2, 4, 13, 4), ModelTransform.pivot(-2, 11, 0), ModelTransform.NONE);
        return TexturedModelData.of(data, 128, 128);
    }
    private static ModelPartBuilder cube(int u, int v, float x, float y, float z, int w, int h, int d) {
        return new ModelPartBuilder().uv(u, v).cuboid(x, y, z, w, h, d);
    }
    @Override public void setAngles(CompanionEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        reset(head, 0, 0); reset(torso, 0, 0);
        reset(hairBack, 0, 0); reset(hairLeft, 0, 0); reset(hairRight, 0, 0);
        reset(leftArm, 5, 1); reset(rightArm, -5, 1);
        reset(leftLeg, 2, 11); reset(rightLeg, -2, 11);
        head.yaw = headYaw * MathHelper.RADIANS_PER_DEGREE;
        head.pitch = headPitch * MathHelper.RADIANS_PER_DEGREE;
        float walk = MathHelper.cos(limbAngle * 0.6662f) * Math.min(1, limbDistance) * 0.65f;
        leftLeg.pitch = walk; rightLeg.pitch = -walk;
        leftArm.pitch = -walk * 0.7f; rightArm.pitch = walk * 0.7f;
        leftArm.roll = 0.04f + MathHelper.sin(animationProgress * 0.045f) * 0.025f;
        rightArm.roll = -leftArm.roll;
        hairBack.pitch = 0.04f + MathHelper.sin(animationProgress * 0.07f) * 0.025f + limbDistance * 0.05f;
        hairLeft.roll = MathHelper.sin(animationProgress * 0.06f) * 0.02f;
        hairRight.roll = -hairLeft.roll;
        if (entity.mode() == CompanionEntity.Mode.STAY) {
            leftLeg.pitch = -0.35f; rightLeg.pitch = -0.35f;
            leftArm.pitch = -0.3f; rightArm.pitch = -0.3f;
        }
        if (entity.gestureTicks() > 0) {
            float envelope = Math.min(1, entity.gestureTicks() / 8.0f);
            switch (entity.gesture()) {
                case 1 -> { head.roll = -0.13f * envelope; rightArm.pitch = -0.7f * envelope; }
                case 2 -> { leftArm.pitch = -1.35f * envelope; rightArm.pitch = -1.35f * envelope; leftArm.yaw = -0.3f * envelope; rightArm.yaw = 0.3f * envelope; torso.pitch = -0.09f * envelope; }
                case 3 -> { head.pitch += 0.18f * envelope; head.roll = 0.1f * envelope; }
                case 4 -> { rightArm.pitch = -1.8f * envelope; rightArm.yaw = -0.2f * envelope; head.roll = -0.1f * envelope; }
                case 5 -> { torso.pitch = -0.12f * envelope; head.pitch += 0.08f * envelope; head.yaw += 0.12f * envelope; }
                default -> { }
            }
        }
        CompanionProfile profile = entity.profile();
        if (profile == CompanionProfile.MIO) { hairBack.yScale = 0.65f; hairLeft.yScale = 0.7f; hairRight.yScale = 0.7f; }
        else if (profile == CompanionProfile.REI) { hairBack.yScale = 0.85f; hairBack.pitch -= 0.25f; hairLeft.yScale = 0.5f; hairRight.yScale = 1.1f; }
    }
    private static void reset(ModelPart part, float x, float y) { part.resetTransform(); part.pivotX = x; part.pivotY = y; }
    @Override public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, int color) {
        head.render(matrices, vertices, light, overlay, color);
        torso.render(matrices, vertices, light, overlay, color);
        leftArm.render(matrices, vertices, light, overlay, color); rightArm.render(matrices, vertices, light, overlay, color);
        leftLeg.render(matrices, vertices, light, overlay, color); rightLeg.render(matrices, vertices, light, overlay, color);
    }
}