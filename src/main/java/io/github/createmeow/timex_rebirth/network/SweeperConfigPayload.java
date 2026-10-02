package io.github.createmeow.timex_rebirth.network;

import io.github.createmeow.timex_rebirth.features.flint.SnowSweeperItem;
import io.github.createmeow.timex_rebirth.features.flint.SweeperConfig;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 清刷机配置同步（C2S）：配置菜单点击开关/拖动滑条后由客户端发来，
 * 服务端写回玩家手中（主手优先、副手兜底）清刷机的 {@link net.minecraft.core.component.DataComponents#CUSTOM_DATA}。
 */
public record SweeperConfigPayload(boolean snow, boolean leaves, boolean plants,
                                   boolean damage, boolean noWaste,
                                   double range, float damageAmt) implements CustomPacketPayload {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath("timex_rebirth", "sweeper_config");
    public static final Type<SweeperConfigPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<io.netty.buffer.ByteBuf, SweeperConfigPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, packet) -> {
                        buf.writeBoolean(packet.snow());
                        buf.writeBoolean(packet.leaves());
                        buf.writeBoolean(packet.plants());
                        buf.writeBoolean(packet.damage());
                        buf.writeBoolean(packet.noWaste());
                        buf.writeDouble(packet.range());
                        buf.writeFloat(packet.damageAmt());
                    },
                    buf -> new SweeperConfigPayload(buf.readBoolean(), buf.readBoolean(), buf.readBoolean(),
                            buf.readBoolean(), buf.readBoolean(), buf.readDouble(), buf.readFloat()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SweeperConfigPayload packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            for (InteractionHand hand : InteractionHand.values()) {
                var stack = player.getItemInHand(hand);
                if (stack.getItem() instanceof SnowSweeperItem) {
                    SweeperConfig.set(stack, packet.snow(), packet.leaves(), packet.plants(),
                            packet.damage(), packet.noWaste(), packet.range(), packet.damageAmt());
                    return;
                }
            }
        });
    }
}
