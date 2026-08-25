package io.github.createmeow.timex_rebirth.heat.station;

import io.github.createmeow.timex_rebirth.TimeX;
import io.github.createmeow.timex_rebirth.heat.HeatRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 热源供应站注册：5 个方块（发生器/底座/燃料接收器/适配器/模块插槽）、
 * 4 个方块实体、2 个菜单、热流桶、节能/增产模块。
 */
public class HeatStationRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TimeX.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TimeX.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TimeX.MODID);
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, TimeX.MODID);

    // ── 方块与物品（右键统一走放置进度，位置检查在进度开始时进行）──
    public static final DeferredBlock<HeatStationBlock> HEAT_STATION =
            BLOCKS.register("heat_station", HeatStationBlock::new);
    public static final DeferredItem<StationBlockItem> HEAT_STATION_ITEM =
            ITEMS.register("heat_station", () -> new StationBlockItem(HEAT_STATION.get(),
                    "timex_rebirth:heat_station", "block.timex_rebirth.heat_station",
                    StationPlacement.GENERATOR));

    public static final DeferredBlock<HeatBaseBlock> HEAT_BASE =
            BLOCKS.register("heat_base", HeatBaseBlock::new);
    public static final DeferredItem<StationBlockItem> HEAT_BASE_ITEM =
            ITEMS.register("heat_base", () -> new StationBlockItem(HEAT_BASE.get(),
                    "timex_rebirth:heat_base", "block.timex_rebirth.heat_base",
                    StationPlacement.BASE));

    // ── 站位方块（大水车式结构件：由主方块放置时自动填充，无物品无掉落，不参与创造栏）──
    public static final DeferredBlock<HeatBaseCasingBlock> HEAT_BASE_CASING =
            BLOCKS.register("heat_base_casing", HeatBaseCasingBlock::new);
    public static final DeferredBlock<HeatStationCasingBlock> HEAT_STATION_CASING =
            BLOCKS.register("heat_station_casing", HeatStationCasingBlock::new);

    public static final DeferredBlock<HeatFuelReceiverBlock> HEAT_FUEL_RECEIVER =
            BLOCKS.register("heat_fuel_receiver", HeatFuelReceiverBlock::new);
    public static final DeferredItem<StationBlockItem> HEAT_FUEL_RECEIVER_ITEM =
            ITEMS.register("heat_fuel_receiver", () -> new StationBlockItem(HEAT_FUEL_RECEIVER.get(),
                    "timex_rebirth:heat_fuel_receiver", "block.timex_rebirth.heat_fuel_receiver",
                    StationPlacement.PART));

    public static final DeferredBlock<HeatAdapterBlock> HEAT_ADAPTER =
            BLOCKS.register("heat_adapter", HeatAdapterBlock::new);
    public static final DeferredItem<StationBlockItem> HEAT_ADAPTER_ITEM =
            ITEMS.register("heat_adapter", () -> new StationBlockItem(HEAT_ADAPTER.get(),
                    "timex_rebirth:heat_adapter", "block.timex_rebirth.heat_adapter",
                    StationPlacement.PART));

    public static final DeferredBlock<HeatModuleSlotBlock> HEAT_MODULE_SLOT =
            BLOCKS.register("heat_module_slot", HeatModuleSlotBlock::new);
    public static final DeferredItem<StationBlockItem> HEAT_MODULE_SLOT_ITEM =
            ITEMS.register("heat_module_slot", () -> new StationBlockItem(HEAT_MODULE_SLOT.get(),
                    "timex_rebirth:heat_module_slot", "block.timex_rebirth.heat_module_slot",
                    StationPlacement.PART));

    // ── 供应站物品：热流桶 + 节能/增产模块 ──
    // 热流桶必须是 BucketItem 本类（非子类）：Create 只给 BucketItem 本类 patch
    // 流体物品能力（GenericItemFilling.isFluidHandlerValid），子类无法倒入工作盆；
    // stacksTo(1) 保证不可堆叠，容量固定 1000mb。
    public static final DeferredItem<BucketItem> HEAT_FLUX_BUCKET =
            ITEMS.register("heat_flux_bucket", () -> new BucketItem(HeatRegistry.HEAT_FLUX.get(),
                    new Item.Properties().stacksTo(1)));
    public static final DeferredItem<EnergySaveModuleItem> ENERGY_SAVE_MODULE =
            ITEMS.register("energy_save_module", EnergySaveModuleItem::new);
    public static final DeferredItem<ProductionModuleItem> PRODUCTION_MODULE =
            ITEMS.register("production_module", ProductionModuleItem::new);

    // ── 方块实体 ──
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HeatStationBlockEntity>> HEAT_STATION_BE =
            BLOCK_ENTITIES.register("heat_station",
                    () -> BlockEntityType.Builder.of(HeatStationBlockEntity::new, HEAT_STATION.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HeatFuelReceiverBlockEntity>> HEAT_FUEL_RECEIVER_BE =
            BLOCK_ENTITIES.register("heat_fuel_receiver",
                    () -> BlockEntityType.Builder.of(HeatFuelReceiverBlockEntity::new, HEAT_FUEL_RECEIVER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HeatAdapterBlockEntity>> HEAT_ADAPTER_BE =
            BLOCK_ENTITIES.register("heat_adapter",
                    () -> BlockEntityType.Builder.of(HeatAdapterBlockEntity::new, HEAT_ADAPTER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HeatModuleSlotBlockEntity>> HEAT_MODULE_SLOT_BE =
            BLOCK_ENTITIES.register("heat_module_slot",
                    () -> BlockEntityType.Builder.of(HeatModuleSlotBlockEntity::new, HEAT_MODULE_SLOT.get()).build(null));

    // ── 菜单 ──
    public static final DeferredHolder<MenuType<?>, MenuType<HeatStationMenu>> HEAT_STATION_MENU =
            MENUS.register("heat_station",
                    () -> new MenuType<>(HeatStationMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<MenuType<?>, MenuType<HeatModuleSlotMenu>> HEAT_MODULE_SLOT_MENU =
            MENUS.register("heat_module_slot",
                    () -> new MenuType<>(HeatModuleSlotMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        MENUS.register(modEventBus);
    }

    /** 方块实体能力：
     * 热源发生器不暴露物品/流体能力（拒绝漏斗与管道直接输入，
     * 燃料必须经燃料接收器中转或由玩家手动右键投入）；
     * 燃料接收器/适配器暴露流体能力（Create 管道），物品槽暴露给漏斗。 */
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        // 燃料接收器：熔岩注入 + 燃料放入（漏斗 / Create 管道）
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, HEAT_FUEL_RECEIVER_BE.get(),
                (be, side) -> be.getLavaTank());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, HEAT_FUEL_RECEIVER_BE.get(),
                (be, side) -> be.getFuelSlot());

        // 热源适配器：热流抽取（Create 管道从适配器拉取输送到接收器）
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, HEAT_ADAPTER_BE.get(),
                (be, side) -> be.getHeatTank());

        // 模块插槽：物品放入（漏斗 / Create 漏斗自动装填模块）
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, HEAT_MODULE_SLOT_BE.get(),
                (be, side) -> be.getModules());
    }
}
