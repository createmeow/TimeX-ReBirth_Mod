package io.github.createmeow.timex_rebirth.research;

import io.github.createmeow.timex_rebirth.client.ClientResearchState;
import io.github.createmeow.timex_rebirth.network.ResearchActionPayload;
import io.github.createmeow.timex_rebirth.network.ResearchSyncPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 研究系统网络：注册 S2C 同步包与 C2S 操作包，并负责服务端会话操作分发与同步构建。
 */
public class ResearchNetwork {

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("timex_rebirth");
        registrar.playToClient(ResearchSyncPayload.TYPE, ResearchSyncPayload.STREAM_CODEC,
                ResearchNetwork::handleSync);
        registrar.playToServer(ResearchActionPayload.TYPE, ResearchActionPayload.STREAM_CODEC,
                ResearchNetwork::handleAction);
    }

    // ── 服务端 → 客户端 ──

    private static void handleSync(ResearchSyncPayload packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientResearchState.handlePacket(packet);
            if (packet.openScreen()) {
                ClientResearchState.openStationScreen();
            }
        });
    }

    /** 打开研究站界面（携带完整同步数据）。 */
    public static void openStation(ServerPlayer player, BlockPos pos) {
        ResearchStationBlockEntity station = getStationAt(player, pos);
        if (station == null) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("research.timex_rebirth.no_station"));
            return;
        }
        // 权限校验：基地内研究站需基地权限，独立研究站全员放行（此前无校验，任意玩家可打开基地内研究站界面）
        if (!station.canUse(player)) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("research.timex_rebirth.no_permission"));
            return;
        }
        // 若本站会话因区块数据丢失而缺失，从研究者玩家存档快照兜底恢复
        station.restoreFromSnapshot(player);
        sendSync(player, station, true);
    }

    /** 同步玩家研究数据（登录 / 变化时）。 */
    public static void syncPlayer(ServerPlayer player) {
        ResearchStationBlockEntity station = ResearchStationBlockEntity.findStationFor(player);
        sendSync(player, station, false);
    }

    private static void sendSync(ServerPlayer player, ResearchStationBlockEntity station, boolean openScreen) {
        ResearchSession s = station != null ? station.getSession() : null;
        boolean involved = s != null && s.involves(player.getUUID());
        List<String> members = new ArrayList<>();
        if (station != null) {
            for (ServerPlayer m : station.getBaseMembers(player)) {
                members.add(m.getGameProfile().getName() + "|" + m.getUUID());
            }
        }
        ResearchSyncPayload payload = new ResearchSyncPayload(
                ResearchData.getPoints(player),
                new ArrayList<>(ResearchData.getUnlocked(player)),
                openScreen,
                ResearchStationBlockEntity.hasActiveResearch(player),
                involved ? s.nodeId : null,
                involved ? s.mode.name() : null,
                involved ? nameOf(player, s.researcher) : "",
                involved ? namesOf(player, s.participants) : new ArrayList<>(),
                involved ? s.progress : 0,
                involved ? s.duration : 0,
                members,
                station != null ? station.getBlockPos().asLong() : 0L);
        PacketDistributor.sendToPlayer(player, payload);
    }

    private static ResearchStationBlockEntity getStationAt(ServerPlayer player, BlockPos pos) {
        if (player.level().getBlockEntity(pos) instanceof ResearchStationBlockEntity station) {
            return station;
        }
        return null;
    }

    private static String nameOf(ServerPlayer player, UUID uuid) {
        if (uuid == null) return "?";
        ServerPlayer p = player.server.getPlayerList().getPlayer(uuid);
        return p == null ? "?" : p.getGameProfile().getName();
    }

    private static List<String> namesOf(ServerPlayer player, List<UUID> uuids) {
        List<String> names = new ArrayList<>();
        for (UUID uuid : uuids) {
            names.add(nameOf(player, uuid));
        }
        return names;
    }

    // ── 客户端 → 服务端 ──

    private static void handleAction(ResearchActionPayload packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            // 优先按界面绑定的研究站路由（多站相邻时避免操作发到错误的研究站），找不到再按距离回退
            ResearchStationBlockEntity station = ResearchStationBlockEntity.findStationByPos(player, packet.stationPos());
            if (station == null) {
                station = ResearchStationBlockEntity.findStationFor(player);
            }
            if (station == null) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("research.timex_rebirth.no_station"));
                return;
            }

            // 安全检查：验证玩家距离目标研究站不超过 16 格（防止客户端伪造坐标绕过权限）
            BlockPos stationPos = station.getBlockPos();
            double distance = player.distanceToSqr(stationPos.getX() + 0.5, stationPos.getY() + 0.5, stationPos.getZ() + 0.5);
            if (distance > 256) { // 16 格的平方 = 256
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("research.timex_rebirth.no_station"));
                return;
            }

            switch (packet.action()) {
                case "start_solo" -> station.startSolo(player, packet.nodeId());
                case "help" -> station.startHelp(player, packet.nodeId(), packet.partner());
                case "collab" -> station.startCollab(player, packet.nodeId(), packet.partner());
                default -> {
                }
            }
        });
    }

    // ── 登录同步 ──

    @EventBusSubscriber(modid = "timex_rebirth")
    public static class LoginSync {
        @SubscribeEvent
        public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                // 登录时清理失效会话快照（协助伙伴放弃研究后，离线方的残留快照会锁死其研究能力）
                ResearchStationBlockEntity.cleanupStaleSnapshot(player);
                syncPlayer(player);
            }
        }

        @SubscribeEvent
        public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                syncPlayer(player);
            }
        }
    }

    private static String nameOf(List<String> names) {
        return String.join(", ", names);
    }
}
