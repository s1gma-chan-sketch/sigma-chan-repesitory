package dev.sigmachan.companions.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import dev.sigmachan.companions.CompanionProfile;
import dev.sigmachan.companions.entity.CompanionEntity;
import dev.sigmachan.companions.interaction.InteractionService;
import dev.sigmachan.companions.registry.ModRegistry;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class CompanionCommands {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) -> {
            var root = CommandManager.literal("sigma");
            var summon = CommandManager.literal("summon").requires(source -> source.hasPermissionLevel(2));
            for (CompanionProfile profile : CompanionProfile.values()) {
                summon.then(CommandManager.literal(profile.id()).executes(context -> summon(context.getSource(), profile)));
            }
            root.then(summon);
            root.then(CommandManager.literal("menu").executes(context -> target(context.getSource(), null)));
            root.then(CommandManager.literal("follow").executes(context -> target(context.getSource(), "follow")));
            root.then(CommandManager.literal("stay").executes(context -> target(context.getSource(), "stay")));
            root.then(CommandManager.literal("wander").executes(context -> target(context.getSource(), "wander")));
            root.then(CommandManager.literal("home").executes(context -> target(context.getSource(), "home")));
            for (String action : new String[]{"wave", "high_five", "hug", "pet", "snack", "drink", "rest", "status"}) {
                root.then(CommandManager.literal(action).executes(context -> target(context.getSource(), action.equals("status") ? null : action)));
            }
            root.then(CommandManager.literal("forget").executes(context -> {
                ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                dev.sigmachan.companions.SigmaCompanions.ai().forget(player.getUuid());
                context.getSource().sendFeedback(() -> Text.literal("Краткая история внешнего ИИ удалена. Явные воспоминания «запомни …» остались; они хранятся в спутнике."), false);
                return 1;
            }));
            root.then(CommandManager.literal("talk").then(CommandManager.argument("message", StringArgumentType.greedyString()).executes(context -> {
                CompanionEntity companion = nearest(context.getSource());
                InteractionService.handle(context.getSource().getPlayerOrThrow(), companion, "talk", StringArgumentType.getString(context, "message"));
                return 1;
            })));
            root.then(CommandManager.literal("at").then(CommandManager.argument("target", EntityArgumentType.entity()).executes(context -> {
                if (!(EntityArgumentType.getEntity(context, "target") instanceof CompanionEntity companion)) { context.getSource().sendError(Text.literal("Выбери спутника Sigma Companions.")); return 0; }
                InteractionService.open(context.getSource().getPlayerOrThrow(), companion, "");
                return 1;
            })));
            dispatcher.register(root);
        });
    }
    private static CompanionEntity nearest(ServerCommandSource source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        return player.getWorld().getEntitiesByClass(CompanionEntity.class, player.getBoundingBox().expand(6), entity -> entity.isAlive() && player.canSee(entity))
                .stream().min(java.util.Comparator.comparingDouble(player::squaredDistanceTo)).orElseThrow(() -> new com.mojang.brigadier.exceptions.SimpleCommandExceptionType(Text.literal("Подойди к спутнику на расстояние до 6 блоков: /sigma summon konata")).create());
    }
    private static int target(ServerCommandSource source, String action) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        CompanionEntity companion = nearest(source);
        if (action == null) InteractionService.open(source.getPlayerOrThrow(), companion, "Добро пожаловать! Подарки дарят предметом в руке; общение — через меню или /sigma talk.");
        else InteractionService.handle(source.getPlayerOrThrow(), companion, action, "");
        return 1;
    }
    private static int summon(ServerCommandSource source, CompanionProfile profile) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        CompanionEntity companion = ModRegistry.COMPANION.create(player.getWorld());
        if (companion == null) return 0;
        companion.refreshPositionAndAngles(player.getX() + 1, player.getY(), player.getZ(), player.getYaw(), 0);
        companion.setProfile(profile); companion.setOwner(player.getUuid()); companion.bond(player.getUuid());
        if (!player.getWorld().isSpaceEmpty(companion.getBoundingBox())) { source.sendError(Text.literal("Рядом недостаточно места. Освободи два блока над землёй.")); return 0; }
        player.getWorld().spawnEntity(companion);
        source.sendFeedback(() -> Text.literal("Приглашена " + profile.displayName() + ". ПКМ — меню; Shift+ПКМ — следовать/ждать."), false);
        return 1;
    }
    private CompanionCommands() {}
}