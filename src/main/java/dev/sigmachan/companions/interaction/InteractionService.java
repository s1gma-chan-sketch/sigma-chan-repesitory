package dev.sigmachan.companions.interaction;

import dev.sigmachan.companions.SigmaCompanions;
import dev.sigmachan.companions.entity.CompanionEntity;
import dev.sigmachan.companions.network.CompanionStateS2C;
import dev.sigmachan.companions.network.ModNetworking;
import dev.sigmachan.companions.registry.ModRegistry;
import dev.sigmachan.companions.relationship.Bond;
import dev.sigmachan.companions.relationship.Interaction;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

public final class InteractionService {
    private InteractionService() {}

    public static void open(ServerPlayerEntity player, CompanionEntity companion, String notice) {
        if (!valid(player, companion)) return;
        Bond bond = companion.bond(player.getUuid());
        BlockPos home = companion.home();
        ModNetworking.sendState(player, new CompanionStateS2C(companion.getId(), companion.profile().id(), bond.trust(), bond.stage(),
                companion.mode().name(), bond.romanceConsent(), bond.dating(), bond.married(), bond.aiConsent(), companion.isOwner(player.getUuid()),
                SigmaCompanions.ai().available(bond), home == null ? "Не отмечен" : home.toShortString(), notice));
    }

    public static void handle(ServerPlayerEntity player, CompanionEntity companion, String actionId, String message) {
        if (!valid(player, companion)) return;
        var parsed = Interaction.parse(actionId);
        if (parsed.isEmpty()) return;
        Interaction action = parsed.get();
        Bond bond = companion.bond(player.getUuid());
        long now = companion.getWorld().getTime();
        if (!companion.cooldownReady(player.getUuid(), actionId, now, action.cooldownTicks())) {
            if (action != Interaction.TALK) open(player, companion, "Не спеши: предыдущее действие ещё завершается.");
            return;
        }
        if (bond.trust() < action.minimumTrust()) { open(player, companion, "Нужно доверие " + action.minimumTrust() + "/100. Общайся, дари подарки и помогай друг другу."); return; }
        boolean ownerOnly = action == Interaction.FOLLOW || action == Interaction.STAY || action == Interaction.WANDER || action == Interaction.HOME
                || action == Interaction.CONSENT || action == Interaction.DATE || action == Interaction.KISS || action == Interaction.MARRY || action == Interaction.SEPARATE;
        if (ownerOnly && !companion.isOwner(player.getUuid())) { open(player, companion, "Это действие доступно только пригласившему игроку."); return; }
        String notice = "";
        ServerWorld world = (ServerWorld) companion.getWorld();
        switch (action) {
            case TALK -> {
                String clean = message.replaceAll("[\\p{Cntrl}]", "").trim();
                if (clean.isEmpty() || clean.length() > 240) return;
                if (bond.addTrust(1)) notice = "Доверие +1";
                SigmaCompanions.ai().talk(player, companion, clean);
            }
            case COMPLIMENT -> { bond.addTrust(2); companion.playGesture(1); notice = "«Спасибо! Мне приятно». Доверие +2"; }
            case GIFT -> {
                ItemStack stack = player.getMainHandStack();
                if (!companion.profile().likes(stack.getItem())) { open(player, companion, "Возьми любимый подарок: " + companion.profile().favoriteGift().getName().getString() + ", торт или сладкие ягоды."); return; }
                long day = world.getTime() / 24000L;
                if (!bond.canGift(day)) { open(player, companion, "Сегодня уже три подарка. Важно внимание, а не количество вещей."); return; }
                bond.recordGift(day);
                stack.decrementUnlessCreative(1, player);
                bond.addTrust(stack.getItem() == companion.profile().favoriteGift() ? 8 : 5);
                companion.playGesture(1);
                notice = "Подарок принят! Доверие выросло. Лимит: 3 подарка в игровой день.";
            }
            case HUG -> { bond.addTrust(2); companion.playGesture(2); notice = companion.profile().displayName() + " отвечает тёплым объятием. Доверие +2"; }
            case PET -> { bond.addTrust(1); companion.playGesture(3); notice = companion.profile().displayName() + " улыбается и наклоняет голову. Доверие +1"; }
            case COMFORT -> {
                companion.playGesture(4);
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 100, 0));
                notice = companion.profile().displayName() + " бережно гладит тебя по волосам и поддерживает. Регенерация I на 5 секунд.";
            }
            case CONSENT -> {
                if (bond.married()) { open(player, companion, "Сначала заверши семейный союз кнопкой «Расстаться»."); return; }
                bond.setRomanceConsent(!bond.romanceConsent());
                if (!bond.romanceConsent() && companion.partner() != null && companion.partner().equals(player.getUuid())) companion.setPartner(null);
                notice = bond.romanceConsent() ? "Взаимная романтическая симпатия включена. Это не согласие на другие действия автоматически." : "Романтические действия выключены. Дружба сохранена.";
            }
            case DATE -> {
                if (companion.partner() != null && !player.getUuid().equals(companion.partner())) { open(player, companion, "У спутника уже есть романтический партнёр."); return; }
                if (!bond.startDate()) { open(player, companion, "Нужны доверие 55+, включённое согласие и отсутствие другого союза."); return; }
                companion.setPartner(player.getUuid()); bond.addTrust(5); companion.playGesture(2);
                notice = "Вы договорились узнавать друг друга ближе. Первое свидание началось!";
            }
            case KISS -> {
                if (!bond.romanceConsent() || !bond.dating()) { open(player, companion, "Поцелуй доступен только во взаимных романтических отношениях."); return; }
                companion.playGesture(5); bond.addTrust(1); notice = "Короткий нежный поцелуй. Без откровенных сцен.";
            }
            case MARRY -> {
                if (!bond.dating() || !bond.romanceConsent() || bond.trust() < Bond.MARRIAGE || bond.married()) { open(player, companion, "Нужны доверие 80+, свидание и включённое согласие."); return; }
                ItemStack ring = player.getMainHandStack().isOf(ModRegistry.PROMISE_RING) ? player.getMainHandStack() : player.getOffHandStack();
                if (!ring.isOf(ModRegistry.PROMISE_RING)) { open(player, companion, "Для церемонии возьми кольцо обещания в основную или дополнительную руку."); return; }
                if (!bond.marry()) return;
                ring.decrementUnlessCreative(1, player);
                companion.playGesture(2);
                if (companion.home() == null) companion.setHome(player.getBlockPos());
                notice = "Вы создали семью! Общий дом отмечен. Семья строится на заботе; откровенных сцен и размножения в моде нет.";
            }
            case SEPARATE -> {
                bond.separate();
                if (player.getUuid().equals(companion.partner())) companion.setPartner(null);
                notice = "Союз завершён. Согласие сброшено, доверие не выше 40. Ты можешь спокойно общаться дальше.";
            }
            case FOLLOW -> { companion.setMode(CompanionEntity.Mode.FOLLOW); notice = "Режим: следовать за тобой."; }
            case STAY -> { companion.setMode(CompanionEntity.Mode.STAY); notice = "Режим: ждать на месте."; }
            case WANDER -> { companion.setMode(CompanionEntity.Mode.WANDER); notice = "Режим: гулять рядом, вечером возвращаться домой."; }
            case HOME -> {
                companion.setHome(player.getBlockPos());
                notice = "Дом отмечен: " + player.getBlockPos().toShortString() + ". Возвращается вечером в режиме прогулки.";
            }
            case AI_CONSENT -> {
                if (!SigmaCompanions.ai().configured()) { open(player, companion, "Внешний ИИ выключен владельцем сервера или не настроен. Работают локальные реплики."); return; }
                bond.setAiConsent(!bond.aiConsent());
                notice = bond.aiConsent() ? "Разрешено отправлять текст этого диалога и игровой контекст выбранному провайдеру. Возможна платная тарификация API." : "Отправка внешнему ИИ выключена. Локальный диалог доступен.";
            }
        }
        if (action == Interaction.HUG || action == Interaction.KISS || action == Interaction.MARRY || action == Interaction.GIFT) {
            world.spawnParticles(ParticleTypes.HEART, companion.getX(), companion.getY() + 1.8, companion.getZ(), 5, 0.3, 0.2, 0.3, 0.01);
            world.playSound(null, companion.getBlockPos(), SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.3f, 1.6f);
        }
        open(player, companion, notice);
    }

    private static boolean valid(ServerPlayerEntity player, CompanionEntity companion) {
        return player.isAlive() && !player.isSpectator() && companion.isAlive() && companion.getWorld() == player.getWorld() && companion.squaredDistanceTo(player) <= 36;
    }
}