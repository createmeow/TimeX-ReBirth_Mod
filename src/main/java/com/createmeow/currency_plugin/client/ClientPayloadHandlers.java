package com.createmeow.currency_plugin.client;

import com.createmeow.currency_plugin.init.CurrencyAttachments;
import com.createmeow.currency_plugin.network.UpdateCurrencyPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 仅客户端加载的余额同步处理器。
 * <p>相比在公共代码中使用 {@code context.player()}（玩家实体不存在时包会被静默丢弃），
 * 这里在主线程执行时直接回退到 {@link Minecraft#getInstance()} 的当前玩家，
 * 确保余额包不丢失。</p>
 */
public final class ClientPayloadHandlers {

    private ClientPayloadHandlers() {
    }

    public static void handleUpdateCurrency(UpdateCurrencyPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = Minecraft.getInstance().player;
            if (player != null) {
                player.setData(CurrencyAttachments.COMMON_VALUE.get(), payload.common());
                player.setData(CurrencyAttachments.RARE_VALUE.get(), payload.rare());
            }
        });
    }
}
