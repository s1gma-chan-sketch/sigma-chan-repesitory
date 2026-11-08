package dev.sigmachan.companions.entity;

import dev.sigmachan.companions.CompanionProfile;
import dev.sigmachan.companions.SigmaCompanions;
import dev.sigmachan.companions.network.GestureS2C;
import dev.sigmachan.companions.network.ModNetworking;
import dev.sigmachan.companions.relationship.Bond;
import dev.sigmachan.companions.relationship.Gesture;
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
import net.minecraft.fluid.Fluids;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
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
    private final CompanionNeeds needs = new CompanionNeeds();
    private UUID owner;
    private BlockPos home;
    private int dialogueTurn;
    private long nextAmbientTick = 1200;
    private Gesture pendingGesture = Gesture.NONE;
    private UUID interactionTarget;
    private BlockPos needTarget;
    private int approachTicks;
    private boolean rewardTrust;

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
        super.initDataTracker(builder);
        builder.add(PROFILE, 0).add(MODE, 0).add(GESTURE, 0).add(GESTURE_TICKS, 0);
    }

    @Override protected void initGoals() {
        goalSelector.add(0, new SwimGoal(this));
        goalSelector.add(1, new StayGoal());
        goalSelector.add(2, new ProtectGoal());
        goalSelector.add(3, new MeleeAttackGoal(this, 1.05, true) {
            @Override public boolean canStart() { return !isBusy() && super.canStart(); }
            @Override public boolean shouldContinue() { return !isBusy() && super.shouldContinue(); }
        });
        goalSelector.add(4, new FollowGoal());
        goalSelector.add(5, new ReturnHomeGoal());
        goalSelector.add(6, new WanderAroundFarGoal(this, 0.7) {
            @Override public boolean canStart() { return mode() == Mode.WANDER && !isBusy() && super.canStart(); }
            @Override public boolean shouldContinue() { return mode() == Mode.WANDER && !isBusy() && super.shouldContinue(); }
        });
        goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 7) {
            @Override public boolean canStart() { return !isBusy() && super.canStart(); }
            @Override public boolean shouldContinue() { return !isBusy() && super.shouldContinue(); }
        });
        goalSelector.add(8, new LookAroundGoal(this) {
            @Override public boolean canStart() { return !isBusy() && super.canStart(); }
        });
        targetSelector.add(1, new RevengeGoal(this));
    }

    public CompanionProfile profile() { return CompanionProfile.values()[Math.floorMod(dataTracker.get(PROFILE), CompanionProfile.values().length)]; }
    public void setProfile(CompanionProfile value) { dataTracker.set(PROFILE, value.ordinal()); setCustomName(Text.literal(value.displayName())); }
    public Mode mode() { return Mode.values()[Math.floorMod(dataTracker.get(MODE), Mode.values().length)]; }
    public void setMode(Mode value) { dataTracker.set(MODE, value.ordinal()); cancelPending(); navigation.stop(); }
    public Gesture gesture() { return Gesture.byId(dataTracker.get(GESTURE)); }
    public int gestureTicks() { return dataTracker.get(GESTURE_TICKS); }
    public boolean isBusy() { return gesture() != Gesture.NONE || pendingGesture != Gesture.NONE; }
    public CompanionNeeds needs() { return needs; }
    public UUID owner() { return owner; }
    public void setOwner(UUID value) { owner = value; }
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

    /** Approach first; the reward and first-person feedback happen only when the gesture actually starts. */
    public boolean requestSocialGesture(Gesture gesture, ServerPlayerEntity player) {
        if (isBusy() || !gesture.social()) return false;
        interactionTarget = player.getUuid(); needTarget = null; rewardTrust = true;
        if (horizontalDistanceTo(player) <= 1.9 && Math.abs(player.getY() - getY()) < 2) {
            startGesture(gesture, player, true);
        } else {
            pendingGesture = gesture; approachTicks = 0; navigation.stop();
        }
        return true;
    }
    public void playGesture(Gesture gesture) { if (!isBusy()) startGesture(gesture, null, false); }
    public boolean drinkNearbyWater() {
        if (isBusy() || findWater() == null) return false;
        needs.drink(); playGesture(Gesture.DRINK); return true;
    }
    private void cancelPending() { pendingGesture = Gesture.NONE; interactionTarget = null; needTarget = null; approachTicks = 0; rewardTrust = false; }

    private void startGesture(Gesture gesture, ServerPlayerEntity player, boolean reward) {
        cancelPending(); navigation.stop(); setTarget(null);
        dataTracker.set(GESTURE, gesture.id()); dataTracker.set(GESTURE_TICKS, gesture.duration());
        if (player != null) {
            face(player);
            ModNetworking.sendGesture(player, new GestureS2C(getId(), gesture.id(), gesture.duration()));
            if (reward) {
                int amount = switch (gesture) { case HUG -> 2; case PET, HIGH_FIVE, COMFORT -> 1; default -> 0; };
                bond(player.getUuid()).addTrust(amount);
            }
            ModNetworking.sendDialogue(player, getId(), profile().displayName(), profile().reaction(gesture == Gesture.HIGH_FIVE ? "high_five" : gesture.name().toLowerCase(Locale.ROOT)));
        }
        if (getWorld() instanceof ServerWorld world) {
            var particle = gesture == Gesture.EAT || gesture == Gesture.DRINK ? ParticleTypes.HAPPY_VILLAGER : gesture == Gesture.REST ? ParticleTypes.NOTE : ParticleTypes.NOTE;
            world.spawnParticles(particle, getX(), getY() + 1.5, getZ(), gesture.social() ? 5 : 2, 0.25, 0.2, 0.25, 0.02);
            var sound = switch (gesture) {
                case EAT -> SoundEvents.ENTITY_GENERIC_EAT;
                case DRINK -> SoundEvents.ENTITY_GENERIC_DRINK;
                case HIGH_FIVE -> SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP;
                case HUG, PET, COMFORT -> SoundEvents.ENTITY_VILLAGER_YES;
                default -> SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP;
            };
            world.playSound(null, getBlockPos(), sound, SoundCategory.PLAYERS, 0.35f, gesture == Gesture.HIGH_FIVE ? 1.4f : 1.1f);
        }
    }

    private void finishGesture(Gesture gesture) {
        dataTracker.set(GESTURE, Gesture.NONE.id()); dataTracker.set(GESTURE_TICKS, 0);
        if (gesture == Gesture.REST) needs.finishRest();
        if (gesture == Gesture.EAT || gesture == Gesture.DRINK || gesture == Gesture.REST) {
            String line = switch (gesture) { case EAT -> profile().reaction("snack"); case DRINK -> "Спасибо, вода очень кстати."; default -> "Я немного отдохнула. Так лучше."; };
            broadcastNearby(line);
        }
    }

    @Override public void tick() {
        super.tick();
        if (getWorld().isClient) return;
        Gesture active = gesture();
        int remaining = gestureTicks();
        if (remaining > 0) {
            remaining--; dataTracker.set(GESTURE_TICKS, remaining);
            if (remaining == 0) finishGesture(active);
        }
        if (pendingGesture != Gesture.NONE) tickApproach();
        if (age % 20 == 0) {
            needs.tick(getVelocity().horizontalLengthSquared() > 0.0001);
            if (needs.consumeDigestedNotice()) broadcastNearby("Перекус усвоился. Теперь хочется воды и немного отдыха.");
        }
        if (!isBusy() && age % 40 == 0) tickAutonomy();
        if (getWorld().getTime() >= nextAmbientTick) {
            nextAmbientTick = getWorld().getTime() + 1200 + random.nextInt(600);
            if (SigmaHolder.ambientEnabled() && !isBusy()) {
                String hint = needs.hint();
                broadcastNearby(hint != null ? hint : profile().ambient(age / 20));
            }
        }
        if (getTarget() != null && (getTarget().isRemoved() || !getTarget().isAlive() || squaredDistanceTo(getTarget()) > 576)) setTarget(null);
    }

    private void tickApproach() {
        approachTicks++;
        if (approachTicks > 200) { cancelPending(); navigation.stop(); broadcastNearby("Не могу подойти поближе. Попробуй освободить проход."); return; }
        if (interactionTarget != null) {
            if (!(getWorld() instanceof ServerWorld world)) { cancelPending(); return; }
            ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(interactionTarget);
            if (player == null || !player.isAlive() || player.getWorld() != getWorld() || squaredDistanceTo(player) > 400) { cancelPending(); navigation.stop(); return; }
            face(player);
            if (horizontalDistanceTo(player) <= 1.9 && Math.abs(player.getY() - getY()) < 2) startGesture(pendingGesture, player, rewardTrust);
            else if (approachTicks % 10 == 0) navigation.startMovingTo(player, 0.9);
        } else if (needTarget != null) {
            double distance = Math.hypot(needTarget.getX() + 0.5 - getX(), needTarget.getZ() + 0.5 - getZ());
            if (distance <= 2.2 && Math.abs(needTarget.getY() - getY()) < 3) { Gesture g = pendingGesture; cancelPending(); startGesture(g, null, false); }
            else if (approachTicks % 10 == 0) navigation.startMovingTo(needTarget.getX() + 0.5, needTarget.getY(), needTarget.getZ() + 0.5, 0.8);
        } else cancelPending();
    }

    private void tickAutonomy() {
        if (needs.hydration() < 45) {
            BlockPos water = findWater();
            if (water != null) { pendingGesture = Gesture.DRINK; needTarget = water; interactionTarget = null; approachTicks = 0; navigation.stop(); return; }
        }
        if (needs.energy() < 30 && mode() != Mode.FOLLOW) {
            if (home != null && squaredDistanceTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5) > 16) {
                pendingGesture = Gesture.REST; needTarget = home; interactionTarget = null; approachTicks = 0; navigation.stop();
            } else playGesture(Gesture.REST);
        }
    }

    private BlockPos findWater() {
        BlockPos center = getBlockPos(); BlockPos.Mutable cursor = new BlockPos.Mutable();
        BlockPos best = null; double bestDistance = Double.MAX_VALUE;
        for (int dx = -5; dx <= 5; dx++) for (int dz = -5; dz <= 5; dz++) for (int dy = -2; dy <= 2; dy++) {
            cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
            if (getWorld().getFluidState(cursor).isOf(Fluids.WATER) && getWorld().getBlockState(cursor.up()).isAir()) {
                double distance = cursor.getSquaredDistance(center);
                if (distance < bestDistance) { bestDistance = distance; best = cursor.toImmutable(); }
            }
        }
        return best;
    }

    private void face(PlayerEntity player) {
        getLookControl().lookAt(player, 30, 30);
        double dx = player.getX() - getX(), dz = player.getZ() - getZ();
        float yaw = (float) (MathHelper.atan2(dz, dx) * 180.0 / Math.PI) - 90.0f;
        setYaw(MathHelper.lerp(0.35f, getYaw(), yaw)); setBodyYaw(getYaw()); setHeadYaw(getYaw());
    }
    private double horizontalDistanceTo(PlayerEntity player) { return Math.hypot(player.getX() - getX(), player.getZ() - getZ()); }
    private void broadcastNearby(String message) {
        if (!(getWorld() instanceof ServerWorld world)) return;
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.squaredDistanceTo(this) < 144 && bonds.containsKey(player.getUuid())) ModNetworking.sendDialogue(player, getId(), profile().displayName(), message);
        }
    }

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
        if (hand == Hand.MAIN_HAND) {
            var stack = serverPlayer.getMainHandStack();
            if (stack.contains(net.minecraft.component.DataComponentTypes.FOOD)) dev.sigmachan.companions.interaction.InteractionService.handle(serverPlayer, this, "snack", "");
            else if (profile().likes(stack.getItem())) dev.sigmachan.companions.interaction.InteractionService.handle(serverPlayer, this, "gift", "");
            else dev.sigmachan.companions.interaction.InteractionService.open(serverPlayer, this, "");
        }
        return ActionResult.SUCCESS;
    }

    @Override public boolean damage(DamageSource source, float amount) {
        if (source.getAttacker() instanceof PlayerEntity player && bonds.get(player.getUuid()) != null && bonds.get(player.getUuid()).trust() >= Bond.FRIEND) return false;
        boolean result = super.damage(source, amount);
        if (result && source.getAttacker() instanceof Monster && source.getAttacker() instanceof net.minecraft.entity.mob.MobEntity attacker) { cancelPending(); dataTracker.set(GESTURE, Gesture.NONE.id()); dataTracker.set(GESTURE_TICKS, 0); setTarget(attacker); }
        return result;
    }

    @Override public boolean canImmediatelyDespawn(double distanceSquared) { return false; }
    @Override public boolean cannotDespawn() { return true; }

    @Override public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("SigmaSchema", 2);
        nbt.putString("Profile", profile().id()); nbt.putString("Mode", mode().name());
        if (owner != null) nbt.putUuid("Owner", owner);
        if (home != null) nbt.putIntArray("Home", new int[]{home.getX(), home.getY(), home.getZ()});
        NbtCompound needsTag = new NbtCompound(); needs.write(needsTag); nbt.put("Needs", needsTag);
        NbtList list = new NbtList();
        bonds.forEach((id, bond) -> {
            NbtCompound entry = new NbtCompound();
            entry.putUuid("Player", id); entry.putInt("Trust", bond.trust()); entry.putBoolean("AiConsent", bond.aiConsent());
            entry.putLong("GiftDay", bond.lastGiftDay()); entry.putInt("GiftCount", bond.giftsToday());
            entry.putString("LastTopic", bond.lastTopic()); entry.putInt("TopicTurns", bond.topicTurns());
            NbtList memories = new NbtList(); bond.memories().forEach(memory -> memories.add(NbtString.of(memory)));
            entry.put("Memories", memories); list.add(entry);
        });
        nbt.put("Bonds", list);
    }

    @Override public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        setProfile(CompanionProfile.byId(nbt.getString("Profile")).orElse(CompanionProfile.KONATA));
        try { setMode(Mode.valueOf(nbt.getString("Mode"))); } catch (IllegalArgumentException e) { setMode(Mode.WANDER); }
        owner = nbt.containsUuid("Owner") ? nbt.getUuid("Owner") : null;
        home = null; int[] pos = nbt.getIntArray("Home");
        if (pos.length == 3) home = new BlockPos(pos[0], pos[1], pos[2]);
        needs.read(nbt.getCompound("Needs"));
        bonds.clear(); cooldowns.clear(); cancelPending();
        dataTracker.set(GESTURE, Gesture.NONE.id()); dataTracker.set(GESTURE_TICKS, 0);
        NbtList list = nbt.getList("Bonds", NbtCompound.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound entry = list.getCompound(i);
            if (!entry.containsUuid("Player")) continue;
            Bond bond = new Bond(); bond.setTrust(entry.getInt("Trust")); bond.setAiConsent(entry.getBoolean("AiConsent"));
            bond.restoreGiftCounter(entry.getLong("GiftDay"), entry.getInt("GiftCount"));
            bond.restoreTopic(entry.getString("LastTopic"), entry.getInt("TopicTurns"));
            entry.getList("Memories", NbtCompound.STRING_TYPE).forEach(element -> bond.restoreMemory(element.asString()));
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
        @Override public boolean canStart() { return mode() == Mode.STAY && !isBusy(); }
        @Override public boolean shouldContinue() { return canStart(); }
        @Override public void start() { navigation.stop(); }
        @Override public void tick() { navigation.stop(); }
    }
    private class FollowGoal extends Goal {
        private PlayerEntity player; private int update;
        FollowGoal() { setControls(EnumSet.of(Control.MOVE, Control.LOOK)); }
        @Override public boolean canStart() { player = ownerPlayer(); return mode() == Mode.FOLLOW && !isBusy() && player != null && squaredDistanceTo(player) > 16; }
        @Override public boolean shouldContinue() { return mode() == Mode.FOLLOW && !isBusy() && player != null && player.isAlive() && squaredDistanceTo(player) > 9; }
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
                if (getWorld().getBlockState(pos).isSolidBlock(getWorld(), pos) && getWorld().isAir(pos.up()) && getWorld().isAir(pos.up(2))
                        && getWorld().isSpaceEmpty(getBoundingBox().offset(pos.getX() + 0.5 - getX(), pos.getY() + 1 - getY(), pos.getZ() + 0.5 - getZ()))) {
                    requestTeleport(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5); navigation.stop(); return true;
                }
            }
            return false;
        }
    }
    private class ReturnHomeGoal extends Goal {
        ReturnHomeGoal() { setControls(EnumSet.of(Control.MOVE)); }
        @Override public boolean canStart() { return eligible() && squaredDistanceTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5) > 16; }
        @Override public boolean shouldContinue() { return canStart(); }
        private boolean eligible() { return home != null && mode() == Mode.WANDER && !isBusy() && (getWorld().isNight() || getWorld().hasRain(home)); }
        @Override public void tick() { if (home != null && getWorld().getTime() % 20 == 0) navigation.startMovingTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 0.85); }
        @Override public void stop() { navigation.stop(); }
    }
    private class ProtectGoal extends Goal {
        private net.minecraft.entity.mob.MobEntity threat;
        ProtectGoal() { setControls(EnumSet.of(Control.TARGET)); }
        @Override public boolean canStart() {
            ServerPlayerEntity player = ownerPlayer();
            if (player == null || isBusy() || mode() == Mode.STAY || bond(player.getUuid()).trust() < Bond.CLOSE) return false;
            if (player.getAttacker() instanceof Monster monster && monster instanceof net.minecraft.entity.mob.MobEntity mob
                    && mob.isAlive() && squaredDistanceTo(mob) < 256 && player.squaredDistanceTo(mob) < 256) { threat = mob; return true; }
            return false;
        }
        @Override public void start() { setTarget(threat); }
        @Override public boolean shouldContinue() { return threat != null && threat.isAlive() && getTarget() == threat && squaredDistanceTo(threat) < 256; }
        @Override public void stop() { threat = null; setTarget(null); }
    }
}