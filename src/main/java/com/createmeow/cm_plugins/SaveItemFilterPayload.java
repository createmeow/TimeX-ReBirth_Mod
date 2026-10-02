package com.createmeow.cm_plugins;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 客户端→服务端：保存玩家的过滤物品列表。
 * 物品注册名以 '|' 拼接传输（注册名不含 '|'）。
 */
public record SaveItemFilterPayload(String joinedIds) implements CustomPacketPayload {
    public static final Type<SaveItemFilterPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(createmeowsplugins.MODID, "save_item_filter"));

    public static final StreamCodec<ByteBuf, SaveItemFilterPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    SaveItemFilterPayload::joinedIds,
                    SaveItemFilterPayload::new);

    public java.util.List<String> ids() {
        return joinedIds().isEmpty() ? java.util.List.of() : java.util.List.of(joinedIds().split("\\|"));
    }

    @Override
    public Type<? extends SaveItemFilterPayload> type() {
        return TYPE;
    }
}
