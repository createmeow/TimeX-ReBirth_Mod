package com.createmeow.cm_plugins;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 简化版 TPA 传送系统（复刻 SimpleTpa 的核心流程）：
 * tpa / tpahere 发送请求，tpaccept / tpdeny 同意或拒绝。
 * 不实现冷却、忽略、自动接受等扩展功能。
 */
public class SimpleTpaManager {

    private static final long REQUEST_TIMEOUT_MS = 30_000L;

    private enum Type { TPA, TPAHERE }

    private record Request(UUID from, Type type, long expireAt) {}

    /** 接受者 UUID -> 待处理请求。 */
    private static final Map<UUID, Request> PENDING = new HashMap<>();

    public static void tpa(ServerPlayer from, ServerPlayer target) {
        send(from, target, Type.TPA);
    }

    public static void tpahere(ServerPlayer from, ServerPlayer target) {
        send(from, target, Type.TPAHERE);
    }

    private static void send(ServerPlayer from, ServerPlayer target, Type type) {
        if (from.getUUID().equals(target.getUUID())) {
            from.sendSystemMessage(Component.literal("§c不能向自己发送传送请求"));
            return;
        }
        PENDING.put(target.getUUID(), new Request(from.getUUID(), type, System.currentTimeMillis() + REQUEST_TIMEOUT_MS));

        String action = type == Type.TPA ? "想要传送到你这里" : "想让你传送到 TA 那里";
        target.sendSystemMessage(Component.literal("§e" + from.getName().getString() + " §r" + action
                + "§r：输入 §a/tpaccept §r同意 或 §c/tpdeny §r拒绝（30 秒内有效）"));
        from.sendSystemMessage(Component.literal("§a已向 " + target.getName().getString() + " 发送传送请求"));
    }

    public static void accept(ServerPlayer target) {
        Request req = PENDING.remove(target.getUUID());
        if (req == null) {
            target.sendSystemMessage(Component.literal("§c当前没有待处理的传送请求"));
            return;
        }
        if (System.currentTimeMillis() > req.expireAt()) {
            target.sendSystemMessage(Component.literal("§c该传送请求已过期"));
            return;
        }
        ServerPlayer from = target.getServer().getPlayerList().getPlayer(req.from());
        if (from == null) {
            target.sendSystemMessage(Component.literal("§c请求发起者已离线"));
            return;
        }
        if (req.type() == Type.TPA) {
            teleport(from, target);
        } else {
            teleport(target, from);
        }
    }

    public static void deny(ServerPlayer target) {
        Request req = PENDING.remove(target.getUUID());
        if (req == null) {
            target.sendSystemMessage(Component.literal("§c当前没有待处理的传送请求"));
            return;
        }
        target.sendSystemMessage(Component.literal("§c已拒绝传送请求"));
        ServerPlayer from = target.getServer().getPlayerList().getPlayer(req.from());
        if (from != null) {
            from.sendSystemMessage(Component.literal("§c" + target.getName().getString() + " 拒绝了你的传送请求"));
        }
    }

    private static void teleport(ServerPlayer player, ServerPlayer destination) {
        player.teleportTo(destination.serverLevel(),
                destination.getX(), destination.getY(), destination.getZ(),
                destination.getYRot(), destination.getXRot());
        player.sendSystemMessage(Component.literal("§a传送完成"));
    }
}