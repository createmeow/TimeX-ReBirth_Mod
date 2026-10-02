package com.createmeow.cm_plugins;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
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

    // 玩家附件：物品收集过滤（持久化，跨维度/死亡保留）
    public static final DeferredRegister<net.neoforged.neoforge.attachment.AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(net.neoforged.neoforge.registries.NeoForgeRegistries.ATTACHMENT_TYPES, MODID);
    public static final DeferredHolder<net.neoforged.neoforge.attachment.AttachmentType<?>,
            net.neoforged.neoforge.attachment.AttachmentType<ItemFilterHandler.FilterData>> ITEM_FILTER =
            ATTACHMENT_TYPES.register("item_filter", () ->
                    net.neoforged.neoforge.attachment.AttachmentType
                            .builder(() -> new ItemFilterHandler.FilterData(false, java.util.List.of()))
                            .serialize(ItemFilterHandler.FilterData.CODEC)
                            .copyOnDeath()
                            .build());

    // Shared combat state (set on client via network packet, 0 = not in combat)
    public static volatile int clientCombatTicks = 0;

    public createmeowsplugins(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::onRegisterPayload);
        MENUS.register(modEventBus);
        ITEMS.register(modEventBus);
        ATTACHMENT_TYPES.register(modEventBus);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(new PluginFeatures());
        NeoForge.EVENT_BUS.register(ModCommands.class);
        NeoForge.EVENT_BUS.register(new CombatStateManager());
        NeoForge.EVENT_BUS.register(new CoinConsolidationHandler());
        NeoForge.EVENT_BUS.register(PlayerCoinConsolidationHandler.class);
        NeoForge.EVENT_BUS.register(new ItemFilterHandler());
        NeoForge.EVENT_BUS.register(ActivityAttractionHandler.class);
        NeoForge.EVENT_BUS.register(LegacyPluginCommands.class);
        NeoForge.EVENT_BUS.register(LegacyPluginEvents.class);
        // 世界规则：TNT 上限 / 禁放盔甲架·展示框 / 超大 NBT 物品（禁人盒/书）移除
        NeoForge.EVENT_BUS.register(new WorldRulesHandler());
        // 掉落物合并：爆炸当 tick 对爆炸范围内的同类掉落物聚类合并（静态事件方法，按类注册）
        NeoForge.EVENT_BUS.register(ItemMergeHandler.class);
        // 自然生成数量限制：僵尸 90 / mutanter 每种 10 / 发光鱿鱼 10 / 鲑鳕鱼合计 10 + 蝙蝠·热带鱼禁生成
        NeoForge.EVENT_BUS.register(new SpawnLimitHandler());
        // 枪声吸引怪物：仅 cgm 已加载时注册（引用 cgm 事件类，须类隔离）
        if (ModList.get().isLoaded("cgm")) {
            NeoForge.EVENT_BUS.register(GunfireAttractionHandler.class);
        }
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
        // 客户端请求切换「物品收集过滤」开关
        registrar.playToServer(
                ToggleItemFilterPayload.TYPE,
                ToggleItemFilterPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                        boolean now = ItemFilterHandler.toggle(serverPlayer);
                        ItemFilterHandler.FilterData data = ItemFilterHandler.get(serverPlayer);
                        PacketDistributor.sendToPlayer(serverPlayer,
                                new ItemFilterSyncPayload(now, String.join("|", data.items())));
                    }
                })
        );
        // 客户端保存「物品收集过滤」的物品列表
        registrar.playToServer(
                SaveItemFilterPayload.TYPE,
                SaveItemFilterPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                        ItemFilterHandler.save(serverPlayer, payload.ids());
                        ItemFilterHandler.FilterData data = ItemFilterHandler.get(serverPlayer);
                        PacketDistributor.sendToPlayer(serverPlayer,
                                new ItemFilterSyncPayload(data.enabled(), String.join("|", data.items())));
                    }
                })
        );
        // 服务端同步「物品收集过滤」状态给客户端
        registrar.playToClient(
                ItemFilterSyncPayload.TYPE,
                ItemFilterSyncPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        ItemFilterScreen.setClientState(payload.enabled(), payload.joinedIds()))
        );
    }
}