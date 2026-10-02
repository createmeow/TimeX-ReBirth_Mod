package com.createmeow.currency_plugin.client;

import com.createmeow.currency_plugin.CurrencyPlugin;
import com.createmeow.currency_plugin.init.CurrencyMenuTypes;
import com.createmeow.currency_plugin.network.SyncCurrencyRequestPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 客户端入口：注册钱包覆盖层 + 提炼机界面 + 货币同步请求。
 * 不依赖 owolib，使用 NeoForge 原生 ScreenEvent 渲染。
 */
@Mod(value = CurrencyPlugin.MODID, dist = Dist.CLIENT)
public class CurrencyPluginClient {
    public CurrencyPluginClient(ModContainer container, IEventBus modEventBus) {
        NeoForge.EVENT_BUS.register(PurseOverlay.class);
        NeoForge.EVENT_BUS.register(CurrencyPluginClient.class);
        modEventBus.addListener(this::onRegisterScreens);
        CurrencyPlugin.LOGGER.info("[currency_plugin] 客户端钱包覆盖层已注册");
    }

    private void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(CurrencyMenuTypes.REFINER_MENU.get(), RefinerScreen::new);
    }

    /**
     * 客户端玩家实体就绪（登录）或被重建（重生/维度切换）时，
     * 主动向服务端请求同步货币余额。
     * <p>相比服务端在登录/重生事件里直接推送，该机制保证包到达时
     * 客户端玩家实体已经是最新的，避免余额写入到即将丢弃的旧实体上。</p>
     */
    @SubscribeEvent
    public static void onClientPlayerLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        PacketDistributor.sendToServer(new SyncCurrencyRequestPayload());
    }

    @SubscribeEvent
    public static void onClientPlayerClone(ClientPlayerNetworkEvent.Clone event) {
        PacketDistributor.sendToServer(new SyncCurrencyRequestPayload());
    }
}
