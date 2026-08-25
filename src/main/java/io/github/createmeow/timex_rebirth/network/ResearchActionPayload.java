package io.github.createmeow.timex_rebirth.network;

import io.github.createmeow.timex_rebirth.TimeX;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * 研究操作包（客户端 → 服务端）。
 * action: start_solo / help / collab
 * partner 仅在 help / collab 时需要（目标基地成员 UUID）。
 * stationPos 为发起操作时绑定的研究站坐标（asLong），服务端据此精确路由到对应研究站；
 * 0 表示未知，服务端回退为按玩家附近研究站查找。
 */
public record ResearchActionPayload(String action, String nodeId, @Nullable UUID partner, long stationPos) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ResearchActionPayload> TYPE =
            new CustomPacketPayload.Type<>(TimeX.rl("research_action"));

    public static final StreamCodec<FriendlyByteBuf, ResearchActionPayload> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeUtf(packet.action());
                buf.writeUtf(packet.nodeId());
                boolean hasPartner = packet.partner() != null;
                buf.writeBoolean(hasPartner);
                if (hasPartner) {
                    buf.writeUUID(packet.partner());
                }
                buf.writeLong(packet.stationPos());
            },
            buf -> {
                String action = buf.readUtf();
                String nodeId = buf.readUtf();
                UUID partner = buf.readBoolean() ? buf.readUUID() : null;
                long stationPos = buf.readLong();
                return new ResearchActionPayload(action, nodeId, partner, stationPos);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
