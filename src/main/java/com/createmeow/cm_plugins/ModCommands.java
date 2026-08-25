package com.createmeow.cm_plugins;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Collection;
import java.util.Set;

public class ModCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        // ========== /spatial command ==========
        dispatcher.register(Commands.literal("spatial")
                .executes(ctx -> {
                    CommandSourceStack source = ctx.getSource();
                    if (source.getPlayer() instanceof ServerPlayer player) {
                        SpatialInventoryManager.openSpatialInventory(player);
                    } else {
                        source.sendFailure(Component.literal("§c只有玩家可以使用此命令"));
                    }
                    return Command.SINGLE_SUCCESS;
                })
                .then(Commands.literal("add")
                        .executes(ctx -> {
                            CommandSourceStack source = ctx.getSource();
                            if (source.getPlayer() instanceof ServerPlayer player) {
                                SpatialInventoryManager.handleAddCommand(player);
                            } else {
                                source.sendFailure(Component.literal("§c只有玩家可以使用此命令"));
                            }
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("cancel")
                        .executes(ctx -> {
                            CommandSourceStack source = ctx.getSource();
                            if (source.getPlayer() instanceof ServerPlayer player) {
                                SpatialInventoryManager.handleCancelCommand(player);
                            } else {
                                source.sendFailure(Component.literal("§c只有玩家可以使用此命令"));
                            }
                            return Command.SINGLE_SUCCESS;
                        }))
        );

        // ========== /randomzombie command ==========
        dispatcher.register(Commands.literal("randomzombie")
                .executes(ctx -> {
                    CommandSourceStack source = ctx.getSource();
                    source.sendSuccess(() -> Component.literal("§6§lRandomZombieHealth §7v1.0.0"), false);
                    source.sendSuccess(() -> Component.literal("§7当前配置:"), false);
                    source.sendSuccess(() -> Component.literal("  §8• §7最小血量: §a" + Config.ZOMBIE_MIN_HEALTH.getAsInt()), false);
                    source.sendSuccess(() -> Component.literal("  §8• §7最大血量: §a" + Config.ZOMBIE_MAX_HEALTH.getAsInt()), false);
                    source.sendSuccess(() -> Component.literal("  §8• §7包含刷怪笼: §a" + Config.ZOMBIE_INCLUDE_SPAWNERS.getAsBoolean()), false);
                    source.sendSuccess(() -> Component.literal("  §8• §7包含刷怪蛋: §a" + Config.ZOMBIE_INCLUDE_EGGS.getAsBoolean()), false);
                    source.sendSuccess(() -> Component.literal("  §8• §7包含命令生成: §a" + Config.ZOMBIE_INCLUDE_COMMANDS.getAsBoolean()), false);
                    source.sendSuccess(() -> Component.literal("§7使用 §a/randomzombie reload §7重载配置"), false);
                    return Command.SINGLE_SUCCESS;
                })
                .then(Commands.literal("reload")
                        .requires(src -> src.hasPermission(2))
                        .executes(ctx -> {
                            CommandSourceStack source = ctx.getSource();
                            source.sendSuccess(() -> Component.literal("§a配置已重载！"), false);
                            source.sendSuccess(() -> Component.literal("§7新的血量范围: " +
                                    Config.ZOMBIE_MIN_HEALTH.getAsInt() + " ~ " + Config.ZOMBIE_MAX_HEALTH.getAsInt()), false);
                            return Command.SINGLE_SUCCESS;
                        }))
        );

        // ========== /nomobspawn command ==========
        dispatcher.register(Commands.literal("nomobspawn")
                .executes(ctx -> {
                    CommandSourceStack source = ctx.getSource();
                    source.sendSuccess(() -> Component.literal("§e用法: /nomobspawn <reload|list|help>"), false);
                    return Command.SINGLE_SUCCESS;
                })
                .then(Commands.literal("reload")
                        .requires(src -> src.hasPermission(2))
                        .executes(ctx -> {
                            CommandSourceStack source = ctx.getSource();
                            source.sendSuccess(() -> Component.literal("§a配置已重载成功！"), false);
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("list")
                        .requires(src -> src.hasPermission(2))
                        .executes(ctx -> {
                            CommandSourceStack source = ctx.getSource();
                            Set<String> banned = Config.getBannedMobs();
                            if (banned == null || banned.isEmpty()) {
                                source.sendSuccess(() -> Component.literal("§7当前没有禁止生成的生物"), false);
                            } else {
                                source.sendSuccess(() -> Component.literal("§6=== 禁止生成的生物列表 ==="), false);
                                for (String mob : banned) {
                                    source.sendSuccess(() -> Component.literal("§7- §e" + mob.toLowerCase()), false);
                                }
                                source.sendSuccess(() -> Component.literal("§6总计: §e" + banned.size() + " §6种生物"), false);
                            }
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("help")
                        .executes(ctx -> {
                            CommandSourceStack source = ctx.getSource();
                            source.sendSuccess(() -> Component.literal("§e/nomobspawn reload §7- 重载插件配置"), false);
                            source.sendSuccess(() -> Component.literal("§e/nomobspawn list §7- 查看禁止生成的生物列表"), false);
                            source.sendSuccess(() -> Component.literal("§e/nomobspawn help §7- 显示此帮助信息"), false);
                            return Command.SINGLE_SUCCESS;
                        }))
        );

        // ========== /setcoin command ==========
        // Only register if NumismaticOverhaul is available
        if (NumismaticHelper.isAvailable()) {
            dispatcher.register(Commands.literal("setcoin")
                    .requires(src -> src.hasPermission(2))
                    .then(Commands.argument("player", EntityArgument.player())
                            .then(Commands.literal("add")
                                    .then(Commands.argument("cointype", StringArgumentType.word())
                                            .suggests(COIN_SUGGESTIONS)
                                            .then(Commands.argument("value", IntegerArgumentType.integer(0, Integer.MAX_VALUE))
                                                    .executes(ctx -> {
                                                        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                                        String cointype = StringArgumentType.getString(ctx, "cointype");
                                                        int value = IntegerArgumentType.getInteger(ctx, "value");
                                                        Object currency = NumismaticHelper.parseCurrency(cointype);
                                                        if (currency == null) {
                                                            ctx.getSource().sendFailure(Component.literal("§c无效的货币类型。可用: bronze, silver, gold"));
                                                            return 0;
                                                        }
                                                        long rawValue = NumismaticHelper.getRawValue(currency, value);
                                                        NumismaticHelper.modify(target, rawValue);
                                                        ctx.getSource().sendSuccess(() ->
                                                                        Component.literal("§a已为 " + target.getName().getString() + " 添加 " + value + " " + cointype),
                                                                true);
                                                        return Command.SINGLE_SUCCESS;
                                                    })
                                            )
                                    )
                            )
                            .then(Commands.literal("rm")
                                    .then(Commands.argument("cointype", StringArgumentType.word())
                                            .suggests(COIN_SUGGESTIONS)
                                            .then(Commands.argument("value", IntegerArgumentType.integer(0, Integer.MAX_VALUE))
                                                    .executes(ctx -> {
                                                        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                                        String cointype = StringArgumentType.getString(ctx, "cointype");
                                                        int value = IntegerArgumentType.getInteger(ctx, "value");
                                                        Object currency = NumismaticHelper.parseCurrency(cointype);
                                                        if (currency == null) {
                                                            ctx.getSource().sendFailure(Component.literal("§c无效的货币类型。可用: bronze, silver, gold"));
                                                            return 0;
                                                        }
                                                        long rawValue = NumismaticHelper.getRawValue(currency, value);
                                                        long currentValue = NumismaticHelper.getValue(target);
                                                        long newValue = Math.max(0, currentValue - rawValue);
                                                        NumismaticHelper.setValue(target, newValue);
                                                        ctx.getSource().sendSuccess(() ->
                                                                        Component.literal("§a已从 " + target.getName().getString() + " 扣除 " + value + " " + cointype),
                                                                true);
                                                        return Command.SINGLE_SUCCESS;
                                                    })
                                            )
                                    )
                            )
                            .then(Commands.literal("set")
                                    .then(Commands.argument("cointype", StringArgumentType.word())
                                            .suggests(COIN_SUGGESTIONS)
                                            .then(Commands.argument("value", IntegerArgumentType.integer(0, Integer.MAX_VALUE))
                                                    .executes(ctx -> {
                                                        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                                        String cointype = StringArgumentType.getString(ctx, "cointype");
                                                        int value = IntegerArgumentType.getInteger(ctx, "value");
                                                        Object currency = NumismaticHelper.parseCurrency(cointype);
                                                        if (currency == null) {
                                                            ctx.getSource().sendFailure(Component.literal("§c无效的货币类型。可用: bronze, silver, gold"));
                                                            return 0;
                                                        }
                                                        long rawValue = NumismaticHelper.getRawValue(currency, value);
                                                        NumismaticHelper.setValue(target, rawValue);
                                                        ctx.getSource().sendSuccess(() ->
                                                                        Component.literal("§a已将 " + target.getName().getString() + " 的 " + cointype + " 设置为 " + value),
                                                                true);
                                                        return Command.SINGLE_SUCCESS;
                                                    })
                                            )
                                    )
                            )
                    )
            );
        }

        // ========== /combatmode command ==========
        dispatcher.register(Commands.literal("combatmode")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("add")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("value", IntegerArgumentType.integer(1, 3600))
                                        .executes(ctx -> {
                                            Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
                                            int value = IntegerArgumentType.getInteger(ctx, "value");
                                            for (ServerPlayer target : targets) {
                                                CombatStateManager.addCombatTime(target, value);
                                            }
                                            ctx.getSource().sendSuccess(() ->
                                                    Component.literal("§a已为 " + targets.size() + " 名玩家增加 §e" + value + " §a秒战斗状态"), true);
                                            return Command.SINGLE_SUCCESS;
                                        }))))
                .then(Commands.literal("set")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("value", IntegerArgumentType.integer(0, 3600))
                                        .executes(ctx -> {
                                            Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
                                            int value = IntegerArgumentType.getInteger(ctx, "value");
                                            for (ServerPlayer target : targets) {
                                                CombatStateManager.setCombatSeconds(target, value);
                                            }
                                            ctx.getSource().sendSuccess(() ->
                                                    Component.literal("§a已为 " + targets.size() + " 名玩家设置 §e" + value + " §a秒战斗状态"), true);
                                            return Command.SINGLE_SUCCESS;
                                        }))))
                .then(Commands.literal("rm")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("value", IntegerArgumentType.integer(1, 3600))
                                        .executes(ctx -> {
                                            Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
                                            int value = IntegerArgumentType.getInteger(ctx, "value");
                                            for (ServerPlayer target : targets) {
                                                CombatStateManager.removeCombatTime(target, value);
                                            }
                                            ctx.getSource().sendSuccess(() ->
                                                    Component.literal("§a已为 " + targets.size() + " 名玩家减少 §e" + value + " §a秒战斗状态"), true);
                                            return Command.SINGLE_SUCCESS;
                                        }))))
                .then(Commands.literal("reset")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(ctx -> {
                                    Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
                                    for (ServerPlayer target : targets) {
                                        CombatStateManager.resetCombat(target);
                                    }
                                    ctx.getSource().sendSuccess(() ->
                                            Component.literal("§a已重置 " + targets.size() + " 名玩家的战斗状态"), true);
                                    return Command.SINGLE_SUCCESS;
                                })))
        );
    }

    private static final SuggestionProvider<CommandSourceStack> COIN_SUGGESTIONS = (ctx, builder) -> {
        builder.suggest("bronze");
        builder.suggest("silver");
        builder.suggest("gold");
        return builder.buildFuture();
    };
}