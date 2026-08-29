package com.createmeow.cm_plugins;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

@Mod(createmeowsplugins.MODID)
public class createmeowsplugins {
    public static final String MODID = "cm_plugins";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<SpatialInventoryManager.SpatialContainer>> SPATIAL_MENU =
            MENUS.register("spatial", () ->
                    new MenuType<>(
                            SpatialInventoryManager.SpatialContainer::new,
                            FeatureFlags.DEFAULT_FLAGS
                    )
            );

    // 「空气」占位物品：聊天展示物品时，手上为空用它作为合法 hover（避免 minecraft:air 及数量 0 的编码导致断线）。
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(MODID);
    public static final DeferredItem<Item> AIR_ITEM =
            ITEMS.registerItem("air", Item::new);

    // Shared combat state (set on client via network packet, 0 = not in combat)
    public static volatile int clientCombatTicks = 0;

    public createmeowsplugins(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::onRegisterPayload);
        MENUS.register(modEventBus);
        ITEMS.register(modEventBus);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(new PluginFeatures());
        NeoForge.EVENT_BUS.register(ModCommands.class);
        NeoForge.EVENT_BUS.register(new CombatStateManager());
        NeoForge.EVENT_BUS.register(new CoinConsolidationHandler());
        NeoForge.EVENT_BUS.register(PlayerCoinConsolidationHandler.class);
        NeoForge.EVENT_BUS.register(LegacyPluginCommands.class);
        NeoForge.EVENT_BUS.register(LegacyPluginEvents.class);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("[cm_plugins] 已加载 - 功能: 随机僵尸血量 + 禁止刷怪 + 量子空间 + 战斗逃跑惩罚");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("[cm_plugins] 服务端启动完成");
        LegacyPluginData.load(event.getServer());
    }

    private void onRegisterPayload(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(MODID);
        registrar.playToClient(
                CombatStatePayload.TYPE,
                CombatStatePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> clientCombatTicks = payload.remainingTicks())
        );
        // 客户端请求打开量子空间（来自 ALT 轮盘）
        registrar.playToServer(
                OpenSpatialInventoryPayload.TYPE,
                OpenSpatialInventoryPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                        SpatialInventoryManager.openSpatialInventory(serverPlayer);
                    }
                })
        );
        // 客户端请求切换「自动合并钱币」开关
        registrar.playToServer(
                ToggleAutoCoinPayload.TYPE,
                ToggleAutoCoinPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                        boolean now = PlayerCoinConsolidationHandler.toggleAutoConsolidate(serverPlayer.getUUID());
                        PacketDistributor.sendToPlayer(serverPlayer, new AutoCoinStatePayload(now));
                    }
                })
        );
        // 服务端同步「自动合并钱币」开关状态给客户端
        registrar.playToClient(
                AutoCoinStatePayload.TYPE,
                AutoCoinStatePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        UtilityScreen.setClientAutoCoin(payload.enabled()))
        );
    }
}