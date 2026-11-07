package ru.sigmachan.luckystar.entity;

import java.util.UUID;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAIAvoidEntity;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAIWanderAvoidWater;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/** A non-combat companion. Relationship state is per companion, not global. */
public abstract class StarFriend extends EntityCreature {
    private UUID friendId;
    private int friendship;
    private boolean staying;
    private long nextGiftTime;

    protected StarFriend(World world) {
        super(world);
        setSize(0.6F, 1.8F);
        experienceValue = 0;
        enablePersistence();
    }

    protected abstract String characterKey();

    @Override protected void initEntityAI() {
        tasks.addTask(0, new EntityAISwimming(this));
        tasks.addTask(1, new EntityAIAvoidEntity<EntityMob>(this, EntityMob.class, 8.0F, 1.0D, 1.2D));
        tasks.addTask(2, new FollowFriend());
        tasks.addTask(3, new EntityAIWanderAvoidWater(this, 0.7D) {
            @Override public boolean shouldExecute() { return !staying && super.shouldExecute(); }
            @Override public boolean shouldContinueExecuting() { return !staying && super.shouldContinueExecuting(); }
        });
        tasks.addTask(4, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
        tasks.addTask(5, new EntityAILookIdle(this));
    }

    @Override protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(20.0D);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.27D);
        getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(32.0D);
    }

    @Override protected boolean canDespawn() { return false; }

    private boolean isFriend(EntityPlayer player) {
        return friendId != null && friendId.equals(player.getUniqueID());
    }

    private void say(EntityPlayer player, String key, Object... args) {
        player.sendMessage(new TextComponentTranslation("luckystar.dialogue." + key, args));
    }

    private TextComponentTranslation characterName() {
        return new TextComponentTranslation("luckystar.character." + characterKey());
    }

    @Override protected boolean processInteract(EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        Item item = stack.getItem();
        boolean flower = item == Item.getItemFromBlock(Blocks.RED_FLOWER)
                || item == Item.getItemFromBlock(Blocks.YELLOW_FLOWER);
        boolean cake = item == Items.CAKE;
        if (!stack.isEmpty() && !flower && !cake) return super.processInteract(player, hand);
        // Interactions and inventory changes happen on the logical server only.
        if (world.isRemote) return true;
        if (flower || cake) {
            if (friendId != null && !isFriend(player)) {
                say(player, "already_friends", characterName());
                return true;
            }
            if (cake && friendId == null) {
                say(player, "flower_first", characterName());
                return true;
            }
            if (world.getTotalWorldTime() < nextGiftTime) {
                say(player, "cooldown", characterName());
                return true;
            }
            if (friendship >= 100) {
                say(player, "max", characterName());
                return true;
            }
            if (friendId == null) friendId = player.getUniqueID();
            friendship = Math.min(100, friendship + (cake ? 10 : 5));
            nextGiftTime = world.getTotalWorldTime() + 200;
            if (!player.capabilities.isCreativeMode) stack.shrink(1);
            ((WorldServer) world).spawnParticle(EnumParticleTypes.HEART, posX, posY + height + 0.2D,
                    posZ, 5, 0.3D, 0.2D, 0.3D, 0.0D);
            say(player, "gift", characterName(), friendship);
            return true;
        }
        if (player.isSneaking()) {
            if (!isFriend(player)) {
                say(player, "flower_first", characterName());
            } else {
                staying = !staying;
                getNavigator().clearPath();
                say(player, staying ? "stay" : "follow", characterName());
            }
        } else if (friendId == null) {
            say(player, "hello", characterName());
        } else if (!isFriend(player)) {
            say(player, "already_friends", characterName());
        } else {
            String level = friendship >= 60 ? "close" : friendship >= 25 ? "friend" : "new";
            say(player, "status_" + level, characterName(), friendship,
                    new TextComponentTranslation("luckystar.mode." + (staying ? "stay" : "follow")));
        }
        return true;
    }

    @Override public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        if (friendId != null) tag.setUniqueId("Friend", friendId);
        tag.setInteger("Friendship", friendship);
        tag.setBoolean("Staying", staying);
        tag.setLong("NextGiftTime", nextGiftTime);
    }

    @Override public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        friendId = tag.hasUniqueId("Friend") ? tag.getUniqueId("Friend") : null;
        friendship = Math.max(0, Math.min(100, tag.getInteger("Friendship")));
        staying = tag.getBoolean("Staying");
        nextGiftTime = tag.getLong("NextGiftTime");
    }

    private class FollowFriend extends EntityAIBase {
        private EntityPlayer target;
        private int recalculate;
        FollowFriend() { setMutexBits(3); }
        @Override public boolean shouldExecute() {
            if (staying || friendId == null) return false;
            target = world.getPlayerEntityByUUID(friendId);
            return target != null && !target.isSpectator() && target.isEntityAlive()
                    && getDistanceSq(target) > 9.0D && getDistanceSq(target) < 1024.0D;
        }
        @Override public boolean shouldContinueExecuting() {
            return !staying && target != null && target.isEntityAlive() && !target.isSpectator()
                    && getDistanceSq(target) > 4.0D && getDistanceSq(target) < 1024.0D;
        }
        @Override public void startExecuting() { recalculate = 0; }
        @Override public void resetTask() { target = null; getNavigator().clearPath(); }
        @Override public void updateTask() {
            getLookHelper().setLookPositionWithEntity(target, 10.0F, getVerticalFaceSpeed());
            if (--recalculate <= 0) {
                recalculate = 10;
                getNavigator().tryMoveToEntityLiving(target, 1.0D);
            }
        }
    }

    public static class Konata extends StarFriend {
        public Konata(World world) { super(world); }
        @Override protected String characterKey() { return "konata"; }
    }
    public static class Kagami extends StarFriend {
        public Kagami(World world) { super(world); }
        @Override protected String characterKey() { return "kagami"; }
    }
    public static class Tsukasa extends StarFriend {
        public Tsukasa(World world) { super(world); }
        @Override protected String characterKey() { return "tsukasa"; }
    }
    public static class Miyuki extends StarFriend {
        public Miyuki(World world) { super(world); }
        @Override protected String characterKey() { return "miyuki"; }
    }
}
