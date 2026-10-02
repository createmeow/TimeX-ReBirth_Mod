package com.createmeow.cm_plugins;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 服务端→客户端：同步「物品收集过滤」开关与过滤物品列表。
 */
public record ItemFilterSyncPayload(boolean enabled, String joinedIds) implements CustomPacketPayload {
    public static final Type<ItemFilterSyncPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(createmeowsplugins.MODID, "item_filter_state"));

    public static final StreamCodec<ByteBuf, ItemFilterSyncPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    ItemFilterSyncPayload::enabled,
                    ByteBufCodecs.STRING_UTF8,
                    ItemFilterSyncPayload::joinedIds,
                    ItemFilterSyncPayload::new);

    @Override
    public Type<? extends ItemFilterSyncPayload> type() {
        return TYPE;
    }
}
