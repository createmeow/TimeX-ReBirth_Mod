package io.github.createmeow.timex_rebirth.research;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * 研究系统命令：
 * {@code /timexresearch agree} —— 同意当前待处理的帮助/协助邀请
 * （聊天邀请消息上的 [同意] 按钮通过 ClickEvent RUN_COMMAND 触发该命令）。
 * {@code /timexresearch abandon} —— 放弃当前进行中的研究（不返还点数），
 * 用于研究站被拆等导致会话停滞时解除"同时只能研究 1 项"的限制。
 * 管理命令（需 OP，调试用）：
 * {@code /timexresearch reset <玩家>} —— 一键重置该玩家研究数据（点数/解锁/活动会话）。
 * {@code /timexresearch unlock <玩家> <节点id>} —— 解锁指定研究节点。
 * {@code /timexresearch unlockall <玩家>} —— 瞬间解锁该玩家全部研究节点（调试用）。
 * {@code /timexresearch points <玩家> <点数>} —— 增加研究点数（负数可扣除）。
 */
@EventBusSubscriber(modid = "timex_rebirth")
public class ResearchCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("timexresearch")
                .then(Commands.literal("agree")
                        .executes(ctx -> {
                            CommandSourceStack source = ctx.getSource();
                            if (source.getEntity() instanceof ServerPlayer player) {
                                ResearchStationBlockEntity.agreeInvite(player);
                            }
                            return 1;
                        }))
                .then(Commands.literal("abandon")
                        .executes(ctx -> {
                            CommandSourceStack source = ctx.getSource();
                            if (source.getEntity() instanceof ServerPlayer player) {
                                ResearchStationBlockEntity.abandonSession(player);
                            }
                            return 1;
                        }))
                .then(Commands.literal("reset")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> {
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                    ResearchData.reset(target);
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "§a已重置 " + target.getGameProfile().getName() + " 的研究数据"), true);
                                    return 1;
                                })))
                .then(Commands.literal("unlock")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("node", StringArgumentType.word())
                                        .executes(ctx -> {
                                            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                            String node = StringArgumentType.getString(ctx, "node");
                                            if (TechTree.getNode(node) == null) {
                                                ctx.getSource().sendFailure(Component.literal("§c未知研究节点: " + node));
                                                return 0;
                                            }
                                            ResearchData.unlock(target, node);
                                            ctx.getSource().sendSuccess(() -> Component.literal(
                                                    "§a已为 " + target.getGameProfile().getName() + " 解锁「" + node + "」"), true);
                                            return 1;
                                        }))))
                .then(Commands.literal("unlockall")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> {
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                    int unlockedCount = 0;
                                    for (TechNode node : TechTree.getNodes()) {
                                        if (ResearchData.isUnlocked(target, node.id())) continue;
                                        ResearchData.unlock(target, node.id());
                                        unlockedCount++;
                                    }
                                    final int added = unlockedCount;
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "§a已为 " + target.getGameProfile().getName() + " 瞬间解锁 "
                                                    + added + " 个研究节点（累计 "
                                                    + ResearchData.getUnlocked(target).size() + "/"
                                                    + TechTree.getNodes().size() + "）"), true);
                                    return 1;
                                })))
                .then(Commands.literal("points")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("amount", IntegerArgumentType.integer())
                                        .executes(ctx -> {
                                            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                            int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                            ResearchData.addPoints(target, amount);
                                            ctx.getSource().sendSuccess(() -> Component.literal(
                                                    "§a已为 " + target.getGameProfile().getName() + " 添加 " + amount
                                                            + " 阅历（当前 " + ResearchData.getPoints(target) + "）"), true);
                                            return 1;
                                        })))));
    }
}
