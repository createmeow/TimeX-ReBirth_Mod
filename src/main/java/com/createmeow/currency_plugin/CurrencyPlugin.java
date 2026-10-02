package com.createmeow.currency_plugin;

import com.createmeow.currency_plugin.client.ClientPayloadHandlers;
import com.createmeow.currency_plugin.event.ZombieDropHandler;
import com.createmeow.currency_plugin.init.CurrencyAttachments;
import com.createmeow.currency_plugin.init.CurrencyBlockEntities;
import com.createmeow.currency_plugin.init.CurrencyBlocks;
import com.createmeow.currency_plugin.init.CurrencyItems;
import com.createmeow.currency_plugin.init.CurrencyMenuTypes;
import com.createmeow.currency_plugin.network.PurseActionPayload;
import com.createmeow.currency_plugin.network.SyncCurrencyRequestPayload;
import com.createmeow.currency_plugin.network.UpdateCurrencyPayload;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@Mod(CurrencyPlugin.MODID)
public class CurrencyPlugin {
    public static final String MODID = "currency_plugin";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CurrencyPlugin(IEventBus modEventBus, ModContainer modContainer) {
        CurrencyItems.ITEMS.register(modEventBus);
        CurrencyBlocks.BLOCKS.register(modEventBus);
        CurrencyBlocks.BLOCK_ITEMS.register(modEventBus);
        CurrencyBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        CurrencyMenuTypes.MENU_TYPES.register(modEventBus);
        CurrencyAttachments.ATTACHMENT_TYPES.register(modEventBus);
        modEventBus.addListener(this::onRegisterPayload);
        modEventBus.addListener(this::onCreativeTab);
        NeoForge.EVENT_BUS.register(ZombieDropHandler.class);
        LOGGER.info("[currency_plugin] 货币插件已加载 - 两种钱币：腐空朽 / 归霜升");
    }

    private void onRegisterPayload(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(MODID);
        // C2S：客户端请求存入/取出钱币
        registrar.playToServer(
                PurseActionPayload.TYPE,
                PurseActionPayload.STREAM_CODEC,
                PurseActionPayload::handle
        );
        // C2S：客户端玩家实体就绪/重建后请求同步余额（登录/重生/维度切换）
        registrar.playToServer(
                SyncCurrencyRequestPayload.TYPE,
                SyncCurrencyRequestPayload.STREAM_CODEC,
                SyncCurrencyRequestPayload::handle
        );
        // S2C：服务端同步玩家余额到客户端。客户端处理器隔离在 ClientPayloadHandlers
        // （回退到 Minecraft.getInstance().player，避免 context.player() 为 null 时丢包）
        if (FMLEnvironment.dist.isClient()) {
            registrar.playToClient(
                    UpdateCurrencyPayload.TYPE,
                    UpdateCurrencyPayload.STREAM_CODEC,
                    ClientPayloadHandlers::handleUpdateCurrency
            );
        } else {
            registrar.playToClient(
                    UpdateCurrencyPayload.TYPE,
                    UpdateCurrencyPayload.STREAM_CODEC,
                    (payload, context) -> {
                    }
            );
        }
    }

    @SubscribeEvent
    public void onCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(CurrencyItems.COMMON_COIN.get());
            event.accept(CurrencyItems.RARE_COIN.get());
        }
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(CurrencyBlocks.REFINING_MACHINE_ITEM.get());
        }
    }
}
