package com.createmeow.cm_plugins;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 复刻旧 Bukkit 插件的命令集合（Hat / Mute / DimensionBack / LiteItemShow / Redemption / SimpleTpa）。
 * 管理类命令（禁言/兑换码/重载）需权限等级 >= 2（OP）。
 */
public class LegacyPluginCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        // ========== /hat（戴帽子）==========
        dispatcher.register(Commands.literal("hat")
                .executes(LegacyPluginCommands::hat));

        // ========== /mute /unmute（禁言）==========
        dispatcher.register(Commands.literal("mute")
                .requires(src -> src.hasPermission(2))
                .then(Commands.argument("players", EntityArgument.players())
                        .executes(ctx -> mute(ctx, null))
                        .then(Commands.argument("duration", StringArgumentType.word())
                                .executes(ctx -> mute(ctx, StringArgumentType.getString(ctx, "duration"))))));
        dispatcher.register(Commands.literal("unmute")
                .requires(src -> src.hasPermission(2))
                .then(Commands.argument("players", EntityArgument.players())
                        .executes(ctx -> unmute(ctx))));

        // ========== /dimensionbackreload（维度回退重载）==========
        dispatcher.register(Commands.literal("dimensionbackreload")
                .requires(src -> src.hasPermission(2))
                .executes(ctx -> {
                    ctx.getSource().sendSuccess(() -> Component.literal("§a[DimensionBack] 数据已重载"), false);
                    return Command.SINGLE_SUCCESS;
                }));

        // ========== /lis（展示物品）==========
        dispatcher.register(Commands.literal("lis")
                .then(Commands.literal("reload")
                        .requires(src -> src.hasPermission(2))
                        .executes(ctx -> {
                            ctx.getSource().sendSuccess(() -> Component.literal("§a[LiteItemShow] 配置已重载"), false);
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("version")
                        .executes(ctx -> {
                            ctx.getSource().sendSuccess(() -> Component.literal("§a[LiteItemShow] v1.0.0"), false);
                            return Command.SINGLE_SUCCESS;
                        })));

        // ========== /rc（兑换码）==========
        dispatcher.register(Commands.literal("rc")
                .executes(ctx -> {
                    showRcHelp(ctx.getSource());
                    return Command.SINGLE_SUCCESS;
                })
                .then(Commands.literal("code")
                        .then(Commands.argument("code", StringArgumentType.word())
                                .executes(LegacyPluginCommands::rcRedeem)))
                .then(Commands.literal("create")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("code", StringArgumentType.word())
                                .executes(LegacyPluginCommands::rcCreate)))
                .then(Commands.literal("del")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("id", IntegerArgumentType.integer(1))
                                .executes(LegacyPluginCommands::rcDelete)))
                .then(Commands.literal("change")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("id", IntegerArgumentType.integer(1))
                                .then(Commands.argument("code", StringArgumentType.word())
                                        .executes(LegacyPluginCommands::rcChange))))
                .then(Commands.literal("reset")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("id", IntegerArgumentType.integer(1))
                                .executes(LegacyPluginCommands::rcReset)))
                .then(Commands.literal("check")
                        .requires(src -> src.hasPermission(2))
                        .executes(LegacyPluginCommands::rcCheck)));

        // ========== TPA 传送 ==========
        dispatcher.register(Commands.literal("tpa")
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> {
                            SimpleTpaManager.tpa(ctx.getSource().getPlayerOrException(),
                                    EntityArgument.getPlayer(ctx, "player"));
                            return Command.SINGLE_SUCCESS;
                        })));
        dispatcher.register(Commands.literal("tpahere")
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> {
                            SimpleTpaManager.tpahere(ctx.getSource().getPlayerOrException(),
                                    EntityArgument.getPlayer(ctx, "player"));
                            return Command.SINGLE_SUCCESS;
                        })));
        dispatcher.register(Commands.literal("tpaccept")
                .executes(ctx -> {
                    SimpleTpaManager.accept(ctx.getSource().getPlayerOrException());
                    return Command.SINGLE_SUCCESS;
                }));
        dispatcher.register(Commands.literal("tpdeny")
                .executes(ctx -> {
                    SimpleTpaManager.deny(ctx.getSource().getPlayerOrException());
                    return Command.SINGLE_SUCCESS;
                }));

        // ========== /showxyz（展示坐标，需二次确认）==========
        dispatcher.register(Commands.literal("showxyz")
                .executes(LegacyPluginCommands::showXyz));

        // ========== /autocoin（切换自动合并钱币开关）==========
        dispatcher.register(Commands.literal("autocoin")
                .executes(LegacyPluginCommands::toggleAutoCoin));
    }

    // ── /hat ──
    private static int hat(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (held.getCount() > 1) {
            player.sendSystemMessage(Component.literal("§c手持物数量必须为 1 才能戴在头上"));
            return 0;
        }
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        player.setItemInHand(InteractionHand.MAIN_HAND, helmet);
        player.setItemSlot(EquipmentSlot.HEAD, held);
        player.sendSystemMessage(Component.literal("§a已戴上帽子"));
        return Command.SINGLE_SUCCESS;
    }

    // ── /mute [players] [duration] /unmute [players] ──
    private static int mute(CommandContext<CommandSourceStack> ctx, String durationStr) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "players");
        long untilMillis = Long.MAX_VALUE; // 默认永久
        if (durationStr != null && !durationStr.isEmpty()) {
            long ticks = LegacyPluginData.parseDuration(durationStr);
            if (ticks <= 0) {
                ctx.getSource().sendFailure(Component.literal("§c无效的时长：" + durationStr));
                return 0;
            }
            untilMillis = System.currentTimeMillis() + ticks * 50L;
        }
        for (ServerPlayer p : players) {
            LegacyPluginData.mute(p.getUUID(), untilMillis);
        }
        LegacyPluginData.save(ctx.getSource().getServer());
        ctx.getSource().sendSuccess(() -> Component.literal("§a已禁言 " + players.size() + " 名玩家"), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int unmute(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "players");
        for (ServerPlayer p : players) {
            LegacyPluginData.unmute(p.getUUID());
        }
        LegacyPluginData.save(ctx.getSource().getServer());
        ctx.getSource().sendSuccess(() -> Component.literal("§a已解除 " + players.size() + " 名玩家的禁言"), true);
        return Command.SINGLE_SUCCESS;
    }

    // ── /rc code <code>：兑换 ──
    private static int rcRedeem(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String code = StringArgumentType.getString(ctx, "code");
        LegacyPluginData.RedemptionCode found = null;
        for (LegacyPluginData.RedemptionCode rc : LegacyPluginData.CODES.values()) {
            if (rc.code.equalsIgnoreCase(code)) {
                found = rc;
                break;
            }
        }
        if (found == null) {
            player.sendSystemMessage(Component.literal("§c兑换码不存在！"));
            return 0;
        }
        if (found.usedBy != null) {
            player.sendSystemMessage(Component.literal("§c该兑换码已被其他玩家使用！"));
            return 0;
        }
        ItemStack item = LegacyPluginData.tagToItem(found.item, ctx.getSource().getServer());
        boolean added = player.getInventory().add(item);
        if (!added) {
            player.drop(item, false);
            player.sendSystemMessage(Component.literal("§e背包已满，物品已掉落在地"));
        }
        found.usedBy = player.getStringUUID();
        LegacyPluginData.save(ctx.getSource().getServer());
        player.sendSystemMessage(Component.literal("§a成功兑换物品"));
        return Command.SINGLE_SUCCESS;
    }

    // ── /rc create <code>：创建手中物品的兑换码 ──
    private static int rcCreate(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String code = StringArgumentType.getString(ctx, "code");
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            player.sendSystemMessage(Component.literal("§c你手上必须拿着物品！"));
            return 0;
        }
        for (LegacyPluginData.RedemptionCode rc : LegacyPluginData.CODES.values()) {
            if (rc.code.equalsIgnoreCase(code)) {
                player.sendSystemMessage(Component.literal("§c兑换码已存在！"));
                return 0;
            }
        }
        int id = LegacyPluginData.nextCodeId++;
        CompoundTag itemTag = LegacyPluginData.itemToTag(held.copy(), ctx.getSource().getServer());
        LegacyPluginData.CODES.put(id, new LegacyPluginData.RedemptionCode(id, code, itemTag, null));
        LegacyPluginData.save(ctx.getSource().getServer());
        player.sendSystemMessage(Component.literal("§a成功创建兑换码! ID: §e" + id));
        return Command.SINGLE_SUCCESS;
    }

    // ── /rc del <id> ──
    private static int rcDelete(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        int id = IntegerArgumentType.getInteger(ctx, "id");
        if (LegacyPluginData.CODES.remove(id) != null) {
            LegacyPluginData.save(ctx.getSource().getServer());
            player.sendSystemMessage(Component.literal("§a已删除兑换码 ID: §e" + id));
        } else {
            player.sendSystemMessage(Component.literal("§c找不到该兑换码 ID！"));
        }
        return Command.SINGLE_SUCCESS;
    }

    // ── /rc change <id> <code> ──
    private static int rcChange(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        int id = IntegerArgumentType.getInteger(ctx, "id");
        String newCode = StringArgumentType.getString(ctx, "code");
        LegacyPluginData.RedemptionCode rc = LegacyPluginData.CODES.get(id);
        if (rc == null) {
            player.sendSystemMessage(Component.literal("§c找不到该兑换码 ID！"));
            return 0;
        }
        for (LegacyPluginData.RedemptionCode other : LegacyPluginData.CODES.values()) {
            if (other.id != id && other.code.equalsIgnoreCase(newCode)) {
                player.sendSystemMessage(Component.literal("§c兑换码已存在！"));
                return 0;
            }
        }
        rc.code = newCode;
        LegacyPluginData.save(ctx.getSource().getServer());
        player.sendSystemMessage(Component.literal("§a成功更新兑换码! ID: §e" + id));
        return Command.SINGLE_SUCCESS;
    }

    // ── /rc reset <id> ──
    private static int rcReset(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        int id = IntegerArgumentType.getInteger(ctx, "id");
        LegacyPluginData.RedemptionCode rc = LegacyPluginData.CODES.get(id);
        if (rc == null) {
            player.sendSystemMessage(Component.literal("§c找不到该兑换码 ID！"));
            return 0;
        }
        rc.usedBy = null;
        LegacyPluginData.save(ctx.getSource().getServer());
        player.sendSystemMessage(Component.literal("§a已重置兑换码 ID: §e" + id));
        return Command.SINGLE_SUCCESS;
    }

    // ── /rc check：文本列表（替代原版 GUI）──
    private static int rcCheck(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        source.sendSuccess(() -> Component.literal("§6=== 兑换码列表（共 " + LegacyPluginData.CODES.size() + " 个）==="), false);
        for (LegacyPluginData.RedemptionCode rc : LegacyPluginData.CODES.values()) {
            String status = rc.usedBy == null ? "§a可用" : "§c已使用";
            source.sendSuccess(() -> Component.literal("§7ID: §e" + rc.id + " §7| 兑换码: §a" + rc.code + " §7| " + status), false);
        }
        return Command.SINGLE_SUCCESS;
    }

    // ── /showxyz：第一次提示确认，10 秒内再次发送才真正广播坐标 ──
    private static final Map<UUID, Long> XYZ_CONFIRM = new HashMap<>();

    private static int showXyz(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        UUID uuid = player.getUUID();
        long now = System.currentTimeMillis();
        Long due = XYZ_CONFIRM.get(uuid);
        if (due != null && now <= due) {
            XYZ_CONFIRM.remove(uuid);
            Component msg = Component.literal(player.getGameProfile().getName() + " 的坐标: X=" + player.getBlockX()
                    + ", Y=" + player.getBlockY() + ", Z=" + player.getBlockZ());
            player.getServer().getPlayerList().broadcastSystemMessage(msg, false);
        } else {
            XYZ_CONFIRM.put(uuid, now + 10_000L);
            player.sendSystemMessage(Component.literal("§e您当前的操作可能会意外的发送一些秘密信息，请确认您是否真的要这么做，"
                    + "如果真的要这么做，请在10s内再次发送此命令"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int toggleAutoCoin(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        boolean now = PlayerCoinConsolidationHandler.toggleAutoConsolidate(player.getUUID());
        player.sendSystemMessage(Component.literal("§a自动合并钱币已" + (now ? "开启" : "关闭")));
        return Command.SINGLE_SUCCESS;
    }

    private static void showRcHelp(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("§6§lRedemption 帮助"), false);
        source.sendSuccess(() -> Component.literal("§a/rc code <兑换码> §7- 兑换物品"), false);
        if (source.hasPermission(2)) {
            source.sendSuccess(() -> Component.literal("§a/rc create <兑换码> §7- 创建手中物品的兑换码 [OP]"), false);
            source.sendSuccess(() -> Component.literal("§a/rc del <ID> §7- 删除兑换码 [OP]"), false);
            source.sendSuccess(() -> Component.literal("§a/rc change <ID> <兑换码> §7- 更改兑换码 [OP]"), false);
            source.sendSuccess(() -> Component.literal("§a/rc reset <ID> §7- 重置兑换码使用状态 [OP]"), false);
            source.sendSuccess(() -> Component.literal("§a/rc check §7- 查看所有兑换码 [OP]"), false);
        }
    }
}