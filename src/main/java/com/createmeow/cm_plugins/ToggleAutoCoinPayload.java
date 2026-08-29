package com.createmeow.cm_plugins;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 客户端→服务端：请求切换当前玩家的「自动合并钱币」开关。
 */
public record ToggleAutoCoinPayload() implements CustomPacketPayload {
    public static final Type<ToggleAutoCoinPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(createmeowsplugins.MODID, "toggle_auto_coin"));

    public static final StreamCodec<ByteBuf, ToggleAutoCoinPayload> STREAM_CODEC =
            StreamCodec.unit(new ToggleAutoCoinPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}