package com.createmeow.cm_plugins;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 客户端→服务端：请求切换当前玩家的「物品收集过滤」开关。
 */
public record ToggleItemFilterPayload() implements CustomPacketPayload {
    public static final Type<ToggleItemFilterPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(createmeowsplugins.MODID, "toggle_item_filter"));

    public static final StreamCodec<ByteBuf, ToggleItemFilterPayload> STREAM_CODEC =
            StreamCodec.unit(new ToggleItemFilterPayload());

    @Override
    public Type<? extends ToggleItemFilterPayload> type() {
        return TYPE;
    }
}
