package dev.sigmachan.companions.interaction;

import dev.sigmachan.companions.SigmaCompanions;
import dev.sigmachan.companions.entity.CompanionEntity;
import dev.sigmachan.companions.network.CompanionStateS2C;
import dev.sigmachan.companions.network.ModNetworking;
import dev.sigmachan.companions.relationship.Bond;
import dev.sigmachan.companions.relationship.Gesture;
import dev.sigmachan.companions.relationship.Interaction;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

public final class InteractionService {
    private InteractionService() {}

    public static void open(ServerPlayerEntity player, CompanionEntity companion, String notice) {
        if (!valid(player, companion)) return;
        Bond bond = companion.bond(player.getUuid());
        BlockPos home = companion.home();
        var needs = companion.needs();
        ModNetworking.sendState(player, new CompanionStateS2C(companion.getId(), companion.profile().id(), bond.trust(), bond.stage(),
                companion.mode().name(), bond.aiConsent(), companion.isOwner(player.getUuid()), SigmaCompanions.ai().available(bond),
                home == null ? "Не отмечен" : home.toShortString(), notice,
                needs.satiety(), needs.hydration(), needs.energy(), needs.digestionSeconds(), companion.gesture().name()));
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
        if (action.ownerOnly() && !companion.isOwner(player.getUuid())) { open(player, companion, "Это действие доступно только пригласившему игроку."); return; }
        String notice = "";
        ServerWorld world = (ServerWorld) companion.getWorld();
        switch (action) {
            case TALK -> {
                String clean = message.replaceAll("[\\p{Cntrl}]", "").trim();
                if (clean.isEmpty() || clean.length() > 240) return;
                var localCommand = dev.sigmachan.companions.dialogue.LocalCommands.parse(clean);
                if (localCommand.isPresent()) {
                    Interaction command = localCommand.get();
                    if (!companion.isOwner(player.getUuid())) { open(player, companion, "Режимы меняет только пригласивший игрок."); return; }
                    if (bond.trust() < command.minimumTrust()) { open(player, companion, "Нужно доверие " + command.minimumTrust() + "/100."); return; }
                    switch (command) {
                        case FOLLOW -> { companion.setMode(CompanionEntity.Mode.FOLLOW); notice = "Иду за тобой."; }
                        case STAY -> { companion.setMode(CompanionEntity.Mode.STAY); notice = "Подожду здесь."; }
                        case WANDER -> { companion.setMode(CompanionEntity.Mode.WANDER); notice = "Погуляю рядом."; }
                        case HOME -> { companion.setHome(player.getBlockPos()); notice = "Дом отмечен: " + player.getBlockPos().toShortString(); }
                        default -> {}
                    }
                    open(player, companion, notice); return;
                }
                if (bond.addTrust(1)) notice = "Доверие +1";
                SigmaCompanions.ai().talk(player, companion, clean);
            }
            case COMPLIMENT -> { bond.addTrust(2); companion.playGesture(Gesture.WAVE); notice = companion.profile().reaction("wave") + " Доверие +2"; }
            case WAVE -> { companion.playGesture(Gesture.WAVE); notice = companion.profile().reaction("wave"); }
            case GIFT -> {
                if (companion.isBusy()) { open(player, companion, "Подожди, пока закончится текущее действие. Подарок не потрачен."); return; }
                ItemStack stack = player.getMainHandStack();
                if (!companion.profile().likes(stack.getItem())) { open(player, companion, "Возьми любимый подарок: " + companion.profile().favoriteGift().getName().getString() + ", торт, сладкие ягоды или шоколадный корнет."); return; }
                long day = world.getTime() / 24000L;
                if (!bond.canGift(day)) { open(player, companion, "Сегодня уже три подарка. Важно внимание, а не количество вещей."); return; }
                boolean food = stack.contains(DataComponentTypes.FOOD);
                if (food && companion.needs().busyDigesting()) { open(player, companion, "Предыдущий перекус ещё усваивается. Подарок не съеден."); return; }
                bond.recordGift(day); stack.decrementUnlessCreative(1, player);
                bond.addTrust(stack.getItem() == companion.profile().favoriteGift() ? 8 : 5);
                if (food) { companion.needs().offerSnack(18, SigmaCompanions.config().digestionSeconds); companion.playGesture(Gesture.EAT); }
                else companion.playGesture(Gesture.WAVE);
                notice = "Подарок принят! Доверие выросло. Лимит: 3 подарка в игровой день.";
            }
            case SNACK -> {
                ItemStack stack = player.getMainHandStack();
                var food = stack.get(DataComponentTypes.FOOD);
                if (food == null) { open(player, companion, "Возьми в основную руку любую еду или шоколадный корнет."); return; }
                if (companion.needs().busyDigesting()) { open(player, companion, "Предыдущий перекус ещё усваивается. Еда не потрачена."); return; }
                if (companion.isBusy()) { open(player, companion, "Подожди, пока закончится текущее действие."); return; }
                var item = stack.getItem();
                stack.decrementUnlessCreative(1, player);
                companion.needs().offerSnack(food.nutrition() * 3, SigmaCompanions.config().digestionSeconds);
                companion.playGesture(Gesture.EAT);
                if (companion.profile().likes(item)) bond.addTrust(1);
                notice = companion.profile().reaction("snack") + " Эффект перекуса проявится через " + SigmaCompanions.config().digestionSeconds + " с.";
            }
            case DRINK -> {
                if (companion.isBusy()) { open(player, companion, "Подожди, пока закончится текущее действие."); return; }
                if (!companion.drinkNearbyWater()) { open(player, companion, "Нужен источник воды рядом со спутницей (до 5 блоков)."); return; }
                notice = companion.profile().shortName() + " делает несколько глотков. Вода восстановится через пару секунд.";
            }
            case REST -> {
                if (companion.isBusy()) { open(player, companion, "Подожди, пока закончится текущее действие."); return; }
                companion.playGesture(Gesture.REST);
                notice = companion.profile().shortName() + " садится отдохнуть на 10 секунд. Бодрость восстановится после отдыха.";
            }
            case HIGH_FIVE, HUG, PET, COMFORT -> {
                Gesture gesture = switch (action) { case HIGH_FIVE -> Gesture.HIGH_FIVE; case HUG -> Gesture.HUG; case PET -> Gesture.PET; default -> Gesture.COMFORT; };
                if (!companion.requestSocialGesture(gesture, player)) { open(player, companion, "Спутница сейчас занята. Попробуй через пару секунд."); return; }
                notice = companion.profile().shortName() + " подходит ближе…";
            }
            case FOLLOW -> { companion.setMode(CompanionEntity.Mode.FOLLOW); notice = "Режим: следовать за тобой."; }
            case STAY -> { companion.setMode(CompanionEntity.Mode.STAY); notice = "Режим: ждать на месте."; }
            case WANDER -> { companion.setMode(CompanionEntity.Mode.WANDER); notice = "Режим: гулять рядом, вечером возвращаться домой."; }
            case HOME -> { companion.setHome(player.getBlockPos()); notice = "Дом отмечен: " + player.getBlockPos().toShortString() + ". Возвращается вечером в режиме прогулки."; }
            case AI_CONSENT -> {
                if (!SigmaCompanions.ai().configured()) { open(player, companion, "Внешний ИИ выключен владельцем сервера или не настроен. Работают локальные реплики."); return; }
                bond.setAiConsent(!bond.aiConsent());
                notice = bond.aiConsent() ? "Разрешено отправлять текст этого диалога и игровой контекст выбранному провайдеру. Возможна платная тарификация API." : "Отправка внешнему ИИ выключена. Локальный диалог доступен.";
            }
        }
        open(player, companion, notice);
    }

    private static boolean valid(ServerPlayerEntity player, CompanionEntity companion) {
        return player.isAlive() && !player.isSpectator() && companion.isAlive() && companion.getWorld() == player.getWorld() && companion.squaredDistanceTo(player) <= 36;
    }
}