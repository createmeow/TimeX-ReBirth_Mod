package com.createmeow.cm_plugins;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 服务端→客户端：同步当前玩家的「自动合并钱币」开关状态。
 */
public record AutoCoinStatePayload(boolean enabled) implements CustomPacketPayload {
    public static final Type<AutoCoinStatePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(createmeowsplugins.MODID, "auto_coin_state"));

    public static final StreamCodec<ByteBuf, AutoCoinStatePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    AutoCoinStatePayload::enabled,
                    AutoCoinStatePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}