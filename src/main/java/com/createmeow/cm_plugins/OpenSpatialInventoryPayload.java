package com.createmeow.cm_plugins;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 客户端→服务端：请求服务端打开当前玩家的"量子空间"容器菜单。
 *
 * <p>ALT 轮盘在客户端选中"量子空间"项时发送；服务端收到后调用
 * {@link SpatialInventoryManager#openSpatialInventory}（打开的是服务端容器，客户端会自动弹出
 * 已注册的 {@link SpatialScreen}）。</p>
 */
public record OpenSpatialInventoryPayload() implements CustomPacketPayload {
    public static final Type<OpenSpatialInventoryPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(createmeowsplugins.MODID, "open_spatial_inventory"));

    public static final StreamCodec<ByteBuf, OpenSpatialInventoryPayload> STREAM_CODEC =
            StreamCodec.unit(new OpenSpatialInventoryPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
