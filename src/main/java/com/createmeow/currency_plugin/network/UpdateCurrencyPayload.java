package com.createmeow.currency_plugin.network;

import com.createmeow.currency_plugin.CurrencyPlugin;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * S2C：服务端同步玩家两种货币余额（腐空朽 / 归霜升，各自独立枚数）到客户端。
 */
public record UpdateCurrencyPayload(long common, long rare) implements CustomPacketPayload {
    public static final Type<UpdateCurrencyPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CurrencyPlugin.MODID, "update_currency"));

    public static final StreamCodec<ByteBuf, UpdateCurrencyPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_LONG,
                    UpdateCurrencyPayload::common,
                    ByteBufCodecs.VAR_LONG,
                    UpdateCurrencyPayload::rare,
                    UpdateCurrencyPayload::new
            );

    @Override
    public Type<? extends UpdateCurrencyPayload> type() {
        return TYPE;
    }
}
