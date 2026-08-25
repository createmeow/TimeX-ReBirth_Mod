package io.github.createmeow.timex_rebirth.network;

import io.github.createmeow.timex_rebirth.TimeX;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;

/**
 * 研究数据同步包（服务端 → 客户端）。
 * 携带玩家研究点数、已解锁节点、当前研究站会话（若有）与基地成员列表。
 * openScreen 为 true 时客户端同时打开研究站界面。
 * globalResearching 表示玩家全局是否已有进行中的研究会话（含其他研究站），用于界面禁用操作按钮。
 * stationPos 为当前绑定的研究站坐标（asLong，0 表示附近无研究站），客户端操作包原样带回以精确路由。
 * 成员以 "名字|uuid" 字符串列表编码。
 */
public record ResearchSyncPayload(
        int points,
        List<String> unlocked,
        boolean openScreen,
        boolean globalResearching,
        String sessionNode,
        String sessionMode,
        String researcherName,
        List<String> participantNames,
        int sessionProgress,
        int sessionDuration,
        List<String> members,
        long stationPos
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ResearchSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(TimeX.rl("research_sync"));

    public static final StreamCodec<FriendlyByteBuf, ResearchSyncPayload> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeVarInt(packet.points());
                writeStrings(buf, packet.unlocked());
                buf.writeBoolean(packet.openScreen());
                buf.writeBoolean(packet.globalResearching());
                boolean hasSession = packet.sessionNode() != null;
                buf.writeBoolean(hasSession);
                if (hasSession) {
                    buf.writeUtf(packet.sessionNode());
                    buf.writeUtf(packet.sessionMode());
                    buf.writeUtf(packet.researcherName());
                    writeStrings(buf, packet.participantNames());
                    buf.writeVarInt(packet.sessionProgress());
                    buf.writeVarInt(packet.sessionDuration());
                }
                writeStrings(buf, packet.members());
                buf.writeLong(packet.stationPos());
            },
            buf -> {
                int points = buf.readVarInt();
                List<String> unlocked = readStrings(buf);
                boolean openScreen = buf.readBoolean();
                boolean globalResearching = buf.readBoolean();
                boolean hasSession = buf.readBoolean();
                String sessionNode = null;
                String sessionMode = null;
                String researcherName = "";
                List<String> participants = new ArrayList<>();
                int progress = 0;
                int duration = 0;
                if (hasSession) {
                    sessionNode = buf.readUtf();
                    sessionMode = buf.readUtf();
                    researcherName = buf.readUtf();
                    participants = readStrings(buf);
                    progress = buf.readVarInt();
                    duration = buf.readVarInt();
                }
                List<String> members = readStrings(buf);
                long stationPos = buf.readLong();
                return new ResearchSyncPayload(points, unlocked, openScreen, globalResearching, sessionNode, sessionMode,
                        researcherName, participants, progress, duration, members, stationPos);
            }
    );

    private static void writeStrings(FriendlyByteBuf buf, List<String> list) {
        buf.writeVarInt(list.size());
        for (String s : list) {
            buf.writeUtf(s);
        }
    }

    private static List<String> readStrings(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<String> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(buf.readUtf());
        }
        return list;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
