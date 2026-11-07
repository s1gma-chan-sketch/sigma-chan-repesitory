package dev.sigmachan.companions.entity;

import dev.sigmachan.companions.CompanionProfile;
import dev.sigmachan.companions.registry.ModRegistry;
import dev.sigmachan.companions.relationship.Bond;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import java.util.*;

public class CompanionEntity extends PathAwareEntity {
    public enum Mode { WANDER, FOLLOW, STAY }
    private static final TrackedData<Integer> PROFILE = DataTracker.registerData(CompanionEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> MODE = DataTracker.registerData(CompanionEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> GESTURE = DataTracker.registerData(CompanionEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> GESTURE_TICKS = DataTracker.registerData(CompanionEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private final Map<UUID, Bond> bonds = new HashMap<>();
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();
    private UUID owner;
    private UUID partner;
    private BlockPos home;
    private int dialogueTurn;
    private long nextAmbientTick;

    public CompanionEntity(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
        setPersistent();
        experiencePoints = 0;
    }

    public static DefaultAttributeContainer.Builder attributes() {
        return PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 30)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.27)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 4)
                .add(EntityAttributes.GENERIC_ARMOR, 2)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32);
    }

    @Override protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(PROFILE, 0).add(MODE, 0).add(GESTURE, 0).add(GESTURE_TICKS, 0);
    }

    @Override protected void initGoals() {
        goalSelector.add(0, new SwimGoal(this));
        goalSelector.add(1, new StayGoal());
        goalSelector.add(2, new ProtectGoal());
        goalSelector.add(3, new MeleeAttackGoal(this, 1.05, true));
        goalSelector.add(4, new FollowGoal());
        goalSelector.add(5, new ReturnHomeGoal());
        goalSelector.add(6, new WanderAroundFarGoal(this, 0.7) {
            @Override public boolean canStart() { return mode() == Mode.WANDER && super.canStart(); }
            @Override public boolean shouldContinue() { return mode() == Mode.WANDER && super.shouldContinue(); }
        });
        goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 7));
        goalSelector.add(8, new LookAroundGoal(this));
        targetSelector.add(1, new RevengeGoal(this));
    }

    public CompanionProfile profile() { return CompanionProfile.values()[Math.floorMod(dataTracker.get(PROFILE), CompanionProfile.values().length)]; }
    public void setProfile(CompanionProfile value) { dataTracker.set(PROFILE, value.ordinal()); setCustomName(Text.literal(value.displayName())); }
    public Mode mode() { return Mode.values()[Math.floorMod(dataTracker.get(MODE), Mode.values().length)]; }
    public void setMode(Mode value) { dataTracker.set(MODE, value.ordinal()); navigation.stop(); }
    public int gesture() { return dataTracker.get(GESTURE); }
    public int gestureTicks() { return dataTracker.get(GESTURE_TICKS); }
    public void playGesture(int id) { dataTracker.set(GESTURE, id); dataTracker.set(GESTURE_TICKS, 45); }
    public UUID owner() { return owner; }
    public void setOwner(UUID value) { owner = value; }
    public UUID partner() { return partner; }
    public void setPartner(UUID value) { partner = value; }
    public BlockPos home() { return home; }
    public void setHome(BlockPos value) { home = value == null ? null : value.toImmutable(); }
    public Bond bond(UUID player) { return bonds.computeIfAbsent(player, id -> new Bond()); }
    public int nextDialogueTurn() { return ++dialogueTurn; }
    public boolean isOwner(UUID player) { return player.equals(owner); }

    public boolean cooldownReady(UUID player, String action, long now, int ticks) {
        Map<String, Long> times = cooldowns.computeIfAbsent(player, id -> new HashMap<>());
        if (now < times.getOrDefault(action, Long.MIN_VALUE)) return false;
        times.put(action, now + ticks);
        return true;
    }

    @Override public void tick() {
        super.tick();
        int remaining = dataTracker.get(GESTURE_TICKS);
        if (remaining > 0) dataTracker.set(GESTURE_TICKS, remaining - 1);
        if (!getWorld().isClient && getWorld().getTime() >= nextAmbientTick) {
            nextAmbientTick = getWorld().getTime() + 1200 + random.nextInt(600);
            if (getWorld() instanceof ServerWorld serverWorld && SigmaHolder.ambientEnabled()) {
                for (ServerPlayerEntity player : serverWorld.getPlayers()) {
                    if (player.squaredDistanceTo(this) < 100 && bonds.containsKey(player.getUuid())) {
                        player.sendMessage(Text.literal("[" + profile().displayName() + "] " + profile().ambient(age / 20)), false);
                        break;
                    }
                }
            }
        }
        if (getTarget() != null && (getTarget().isRemoved() || !getTarget().isAlive() || squaredDistanceTo(getTarget()) > 576)) setTarget(null);
    }

    /** Small indirection avoids loading client classes or a global config from the entity constructor. */
    public static final class SigmaHolder {
        private static boolean ambient = true;
        public static void setAmbient(boolean value) { ambient = value; }
        public static boolean ambientEnabled() { return ambient; }
        private SigmaHolder() {}
    }

    @Override protected ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.SUCCESS;
        if (player.isSpectator()) return ActionResult.PASS;
        if (serverPlayer.isSneaking() && hand == Hand.MAIN_HAND) {
            if (isOwner(player.getUuid()) && bond(player.getUuid()).trust() >= Bond.FRIEND) {
                setMode(mode() == Mode.STAY ? Mode.FOLLOW : Mode.STAY);
                serverPlayer.sendMessage(Text.literal(profile().displayName() + ": " + (mode() == Mode.STAY ? "Подожду здесь." : "Иду за тобой!")), true);
            } else serverPlayer.sendMessage(Text.literal("Режимы меняет пригласивший игрок после достижения дружбы."), true);
            return ActionResult.SUCCESS;
        }
        if (hand == Hand.MAIN_HAND && profile().likes(serverPlayer.getMainHandStack().getItem())) {
            dev.sigmachan.companions.interaction.InteractionService.handle(serverPlayer, this, "gift", "");
            return ActionResult.SUCCESS;
        }
        if (hand == Hand.MAIN_HAND) dev.sigmachan.companions.interaction.InteractionService.open(serverPlayer, this, "");
        return ActionResult.SUCCESS;
    }

    @Override public boolean damage(DamageSource source, float amount) {
        if (source.getAttacker() instanceof PlayerEntity player && bonds.get(player.getUuid()) != null && bonds.get(player.getUuid()).trust() >= Bond.FRIEND) return false;
        boolean result = super.damage(source, amount);
        if (result && source.getAttacker() instanceof PathAwareEntity attacker && !(attacker instanceof PlayerEntity) && attacker instanceof Monster) setTarget(attacker);
        return result;
    }

    
    @Override public boolean canImmediatelyDespawn(double distanceSquared) { return false; }
    @Override public boolean cannotDespawn() { return true; }

    @Override public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putString("Profile", profile().id());
        nbt.putString("Mode", mode().name());
        if (owner != null) nbt.putUuid("Owner", owner);
        if (partner != null) nbt.putUuid("Partner", partner);
        if (home != null) nbt.putIntArray("Home", new int[]{home.getX(), home.getY(), home.getZ()});
        NbtList list = new NbtList();
        bonds.forEach((id, bond) -> {
            NbtCompound entry = new NbtCompound();
            entry.putUuid("Player", id);
            entry.putInt("Trust", bond.trust());
            entry.putBoolean("RomanceConsent", bond.romanceConsent());
            entry.putBoolean("Dating", bond.dating());
            entry.putBoolean("Married", bond.married());
            entry.putBoolean("AiConsent", bond.aiConsent());
            entry.putLong("GiftDay", bond.lastGiftDay());
            entry.putInt("GiftCount", bond.giftsToday());
            NbtList memories = new NbtList();
            bond.memories().forEach(memory -> memories.add(net.minecraft.nbt.NbtString.of(memory)));
            entry.put("Memories", memories);
            list.add(entry);
        });
        nbt.put("Bonds", list);
    }

    @Override public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        setProfile(CompanionProfile.byId(nbt.getString("Profile")).orElse(CompanionProfile.AKARI));
        try { setMode(Mode.valueOf(nbt.getString("Mode"))); } catch (IllegalArgumentException e) { setMode(Mode.WANDER); }
        owner = nbt.containsUuid("Owner") ? nbt.getUuid("Owner") : null;
        partner = nbt.containsUuid("Partner") ? nbt.getUuid("Partner") : null;
        home = null;
        int[] pos = nbt.getIntArray("Home");
        if (pos.length == 3) home = new BlockPos(pos[0], pos[1], pos[2]);
        bonds.clear();
        NbtList list = nbt.getList("Bonds", NbtCompound.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound entry = list.getCompound(i);
            if (!entry.containsUuid("Player")) continue;
            Bond bond = new Bond();
            bond.setTrust(entry.getInt("Trust"));
            bond.setRomanceConsent(entry.getBoolean("RomanceConsent"));
            if (entry.getBoolean("Dating")) bond.startDate();
            if (entry.getBoolean("Married")) bond.marry();
            bond.setAiConsent(entry.getBoolean("AiConsent"));
            bond.restoreGiftCounter(entry.getLong("GiftDay"), entry.getInt("GiftCount"));
            NbtList memories = entry.getList("Memories", NbtCompound.STRING_TYPE);
            memories.forEach(element -> bond.restoreMemory(element.asString()));
            bonds.put(entry.getUuid("Player"), bond);
        }
    }

    private ServerPlayerEntity ownerPlayer() {
        if (owner == null || !(getWorld() instanceof ServerWorld world)) return null;
        ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(owner);
        return player != null && player.getWorld() == world && player.isAlive() ? player : null;
    }

    private class StayGoal extends Goal {
        StayGoal() { setControls(EnumSet.of(Control.MOVE, Control.JUMP)); }
        @Override public boolean canStart() { return mode() == Mode.STAY; }
        @Override public boolean shouldContinue() { return canStart(); }
        @Override public void start() { navigation.stop(); }
        @Override public void tick() { navigation.stop(); }
    }

    private class FollowGoal extends Goal {
        private PlayerEntity player;
        private int update;
        FollowGoal() { setControls(EnumSet.of(Control.MOVE, Control.LOOK)); }
        @Override public boolean canStart() {
            player = ownerPlayer();
            return mode() == Mode.FOLLOW && player != null && squaredDistanceTo(player) > 16;
        }
        @Override public boolean shouldContinue() { return mode() == Mode.FOLLOW && player != null && player.isAlive() && squaredDistanceTo(player) > 9; }
        @Override public void stop() { player = null; navigation.stop(); }
        @Override public void tick() {
            if (player == null) return;
            getLookControl().lookAt(player, 30, 30);
            if (squaredDistanceTo(player) > 576 && tryTeleport()) return;
            if (--update <= 0) { update = 10; navigation.startMovingTo(player, 1.05); }
        }
        private boolean tryTeleport() {
            for (int i = 0; i < 12; i++) {
                int dx = random.nextInt(5) - 2, dz = random.nextInt(5) - 2;
                if (dx == 0 && dz == 0) continue;
                BlockPos pos = player.getBlockPos().add(dx, 0, dz);
                if (getWorld().getBlockState(pos).isSolidBlock(getWorld(), pos)
                        && getWorld().isAir(pos.up()) && getWorld().isAir(pos.up(2))
                        && getWorld().isSpaceEmpty(getBoundingBox().offset(pos.getX() + 0.5 - getX(), pos.getY() + 1 - getY(), pos.getZ() + 0.5 - getZ()))) {
                    requestTeleport(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
                    navigation.stop();
                    return true;
                }
            }
            return false;
        }
    }

    private class ReturnHomeGoal extends Goal {
        ReturnHomeGoal() { setControls(EnumSet.of(Control.MOVE)); }
        @Override public boolean canStart() { return eligible() && squaredDistanceTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5) > 16; }
        @Override public boolean shouldContinue() { return canStart(); }
        private boolean eligible() { return home != null && mode() == Mode.WANDER && (getWorld().isNight() || getWorld().hasRain(home)); }
        @Override public void tick() { if (home != null && getWorld().getTime() % 20 == 0) navigation.startMovingTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 0.85); }
        @Override public void stop() { navigation.stop(); }
    }

    private class ProtectGoal extends Goal {
        private net.minecraft.entity.mob.MobEntity threat;
        ProtectGoal() { setControls(EnumSet.of(Control.TARGET)); }
        @Override public boolean canStart() {
            ServerPlayerEntity player = ownerPlayer();
            if (player == null || mode() == Mode.STAY || bond(player.getUuid()).trust() < Bond.CLOSE) return false;
            if (player.getAttacker() instanceof Monster monster && monster instanceof net.minecraft.entity.mob.MobEntity mob
                    && mob.isAlive() && squaredDistanceTo(mob) < 256 && player.squaredDistanceTo(mob) < 256) { threat = mob; return true; }
            return false;
        }
        @Override public void start() { setTarget(threat); }
        @Override public boolean shouldContinue() { return threat != null && threat.isAlive() && getTarget() == threat && squaredDistanceTo(threat) < 256; }
        @Override public void stop() { threat = null; setTarget(null); }
    }
}