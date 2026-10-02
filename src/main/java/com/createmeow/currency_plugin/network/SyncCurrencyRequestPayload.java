package com.createmeow.currency_plugin.network;

import com.createmeow.currency_plugin.CurrencyPlugin;
import com.createmeow.currency_plugin.cap.CurrencyHolder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * C2S：客户端请求服务端同步当前货币余额。
 * <p>客户端玩家实体就绪（登录）或被重建（重生/维度切换）后发送本包，
 * 服务端回发 {@link UpdateCurrencyPayload}。相比服务端主动推送，
 * 该机制保证包到达时客户端玩家实体已经是最新的。</p>
 */
public record SyncCurrencyRequestPayload() implements CustomPacketPayload {

    public static final Type<SyncCurrencyRequestPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CurrencyPlugin.MODID, "sync_currency_request"));

    public static final StreamCodec<ByteBuf, SyncCurrencyRequestPayload> STREAM_CODEC =
            StreamCodec.unit(new SyncCurrencyRequestPayload());

    public static void handle(SyncCurrencyRequestPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer serverPlayer) {
            context.enqueueWork(() -> CurrencyHolder.sync(serverPlayer));
        }
    }

    @Override
    public Type<? extends SyncCurrencyRequestPayload> type() {
        return TYPE;
    }
}
