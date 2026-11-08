package dev.sigmachan.companions.item;

import dev.sigmachan.companions.CompanionProfile;
import dev.sigmachan.companions.SigmaCompanions;
import dev.sigmachan.companions.entity.CompanionEntity;
import dev.sigmachan.companions.registry.ModRegistry;
import net.minecraft.entity.SpawnReason;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.List;

public class InvitationItem extends Item {
    private final CompanionProfile profile;
    public InvitationItem(CompanionProfile profile, Settings settings) { super(settings); this.profile = profile; }

    @Override public ActionResult useOnBlock(ItemUsageContext context) {
        if (!(context.getPlayer() instanceof ServerPlayerEntity player) || !(context.getWorld() instanceof ServerWorld world)) return ActionResult.SUCCESS;
        BlockPos pos = context.getBlockPos().offset(context.getSide());
        Box box = new Box(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1.7, pos.getZ() + 1);
        if (!world.isSpaceEmpty(box) || !world.getBlockState(pos.down()).isSolidBlock(world, pos.down())) {
            player.sendMessage(Text.literal("Нужны твёрдый блок и два свободных блока над ним."), true);
            return ActionResult.FAIL;
        }
        long nearby = world.getEntitiesByClass(CompanionEntity.class, player.getBoundingBox().expand(64), entity -> player.getUuid().equals(entity.owner())).size();
        if (nearby >= SigmaCompanions.config().maxCompanionsPerPlayer) {
            player.sendMessage(Text.literal("Достигнут лимит ваших спутников рядом (настройка сервера)."), false);
            return ActionResult.FAIL;
        }
        CompanionEntity companion = ModRegistry.COMPANION.create(world);
        if (companion == null) return ActionResult.FAIL;
        companion.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player.getYaw(), 0);
        companion.setProfile(profile); companion.setOwner(player.getUuid());
        companion.initialize(world, world.getLocalDifficulty(pos), SpawnReason.MOB_SUMMONED, null);
        world.spawnEntity(companion);
        context.getStack().decrementUnlessCreative(1, player);
        player.sendMessage(Text.literal(profile.displayName() + ": " + profile.introduction()), false);
        return ActionResult.CONSUME;
    }

    @Override public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(profile.displayName() + " · " + profile.interests()));
        tooltip.add(Text.translatable("item.sigma_companions.invitation.tooltip"));
    }
}