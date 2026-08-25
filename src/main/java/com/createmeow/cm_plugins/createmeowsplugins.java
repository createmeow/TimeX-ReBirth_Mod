package com.createmeow.cm_plugins;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
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

    // Shared combat state (set on client via network packet, 0 = not in combat)
    public static volatile int clientCombatTicks = 0;

    public createmeowsplugins(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::onRegisterPayload);
        MENUS.register(modEventBus);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(new PluginFeatures());
        NeoForge.EVENT_BUS.register(ModCommands.class);
        NeoForge.EVENT_BUS.register(new CombatStateManager());
        NeoForge.EVENT_BUS.register(new CoinConsolidationHandler());
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("[cm_plugins] 已加载 - 功能: 随机僵尸血量 + 禁止刷怪 + 量子空间 + 战斗逃跑惩罚");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("[cm_plugins] 服务端启动完成");
    }

    private void onRegisterPayload(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(MODID);
        registrar.playToClient(
                CombatStatePayload.TYPE,
                CombatStatePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> clientCombatTicks = payload.remainingTicks())
        );
    }
}