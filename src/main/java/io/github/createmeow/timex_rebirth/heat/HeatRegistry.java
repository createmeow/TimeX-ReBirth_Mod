package io.github.createmeow.timex_rebirth.heat;

import com.createmeow.nightvisiondevice.NVItems;
import io.github.createmeow.timex_rebirth.TimeX;
import io.github.createmeow.timex_rebirth.antifreeze.AntiFreezeRegistry;
import io.github.createmeow.timex_rebirth.crops.WinterCropRegistry;
import io.github.createmeow.timex_rebirth.food.WastelandFoodRegistry;
import io.github.createmeow.timex_rebirth.heat.station.HeatStationRegistry;
import io.github.createmeow.timex_rebirth.research.ResearchRegistry;
import io.github.createmeow.timex_rebirth.wasteland.WastelandRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * 基地供热系统的全部注册项：
 * - heat_flux 热流流体（管道/储罐专用，不自流）
 * - heat_receiver 热源接收器（紧邻基地核心，接收热流）
 * - heat_bridge_module 热源桥接模块（放入基地核心槽位生效）
 */
public class HeatRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TimeX.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TimeX.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TimeX.MODID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, TimeX.MODID);
    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.FLUID_TYPES, TimeX.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TimeX.MODID);

    // 热流流体类型与流体本体（流体持有类型引用，延迟解析）
    public static final DeferredHolder<FluidType, FluidType> HEAT_FLUX_TYPE =
            FLUID_TYPES.register("heat_flux", () -> new FluidType(
                    FluidType.Properties.create().density(1500).viscosity(2000).temperature(1000)
                            .canSwim(false).canDrown(false).canExtinguish(true)));
    public static final DeferredHolder<Fluid, HeatFluxFluid> HEAT_FLUX =
            FLUIDS.register("heat_flux", () -> new HeatFluxFluid.Source(HeatFluxFluid.createProperties()));
    public static final DeferredHolder<Fluid, HeatFluxFluid> HEAT_FLUX_FLOWING =
            FLUIDS.register("heat_flux_flowing", () -> new HeatFluxFluid.Flowing(HeatFluxFluid.createProperties()));

    // 热流流体方块（真流体，可放置到世界；构造引用 HEAT_FLUX，故 FLUIDS 必须先于 BLOCKS 注册）
    public static final DeferredBlock<LiquidBlock> HEAT_FLUX_BLOCK =
            BLOCKS.register("heat_flux", () -> new LiquidBlock(
                    HEAT_FLUX.get(),
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_ORANGE)
                            .replaceable()
                            .noCollission()
                            .strength(100.0F)
                            .pushReaction(PushReaction.DESTROY)
                            .noLootTable()
                            .liquid()));

    // 热源接收器方块（必须紧邻基地核心）
    public static final DeferredBlock<HeatReceiverBlock> HEAT_RECEIVER =
            BLOCKS.register("heat_receiver", HeatReceiverBlock::new);
    public static final DeferredItem<HeatReceiverItem> HEAT_RECEIVER_ITEM =
            ITEMS.register("heat_receiver", HeatReceiverItem::new);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HeatReceiverBlockEntity>> HEAT_RECEIVER_BE =
            BLOCK_ENTITIES.register("heat_receiver",
                    () -> BlockEntityType.Builder.of(HeatReceiverBlockEntity::new, HEAT_RECEIVER.get()).build(null));

    // 热源桥接模块（BaseCore 模块，放入基地核心槽位）
    public static final DeferredItem<HeatBridgeModuleItem> HEAT_BRIDGE_MODULE =
            ITEMS.register("heat_bridge_module", HeatBridgeModuleItem::new);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB =
            TABS.register("timex", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.timex_rebirth"))
                    .icon(() -> new ItemStack(HEAT_BRIDGE_MODULE.get()))
                    .displayItems((params, output) -> {
                        output.accept(HEAT_RECEIVER_ITEM.get());
                        output.accept(HEAT_BRIDGE_MODULE.get());
                        // 热源供应站组件
                        output.accept(HeatStationRegistry.HEAT_STATION_ITEM.get());
                        output.accept(HeatStationRegistry.HEAT_BASE_ITEM.get());
                        output.accept(HeatStationRegistry.HEAT_FUEL_RECEIVER_ITEM.get());
                        output.accept(HeatStationRegistry.HEAT_ADAPTER_ITEM.get());
                        output.accept(HeatStationRegistry.HEAT_MODULE_SLOT_ITEM.get());
                        output.accept(HeatStationRegistry.HEAT_FLUX_BUCKET.get());
                        output.accept(HeatStationRegistry.ENERGY_SAVE_MODULE.get());
                        output.accept(HeatStationRegistry.PRODUCTION_MODULE.get());
                        // 冬季作物：种子与产物
                        output.accept(WinterCropRegistry.RYE_SEEDS.get());
                        output.accept(WinterCropRegistry.TURNIP_SEEDS.get());
                        output.accept(WinterCropRegistry.RYE.get());
                        output.accept(WinterCropRegistry.RYE_FLOUR.get());
                        output.accept(WinterCropRegistry.RYE_DOUGH.get());
                        output.accept(WinterCropRegistry.RYE_BREAD.get());
                        output.accept(WinterCropRegistry.RYE_STRAW.get());
                        output.accept(WinterCropRegistry.TURNIP.get());
                        output.accept(WinterCropRegistry.BAKED_TURNIP.get());
                        // 防冻系统：防冻剂 + 抗冻凝胶 + 抗冻土壤/抗冻耕地
                        output.accept(AntiFreezeRegistry.ANTI_FREEZE.get());
                        output.accept(AntiFreezeRegistry.ANTI_FREEZE_GEL.get());
                        output.accept(AntiFreezeRegistry.ANTI_FREEZE_DIRT_ITEM.get());
                        output.accept(AntiFreezeRegistry.ANTI_FREEZE_FARMLAND_ITEM.get());
                        // 供热工业材料：耐热合金锭 + 隔热玻璃
                        output.accept(HeatMaterialsRegistry.HEAT_ALLOY_INGOT.get());
                        output.accept(HeatMaterialsRegistry.INSULATED_GLASS.get());
                        // 防火材料：耐火砖 + 防火机壳 + 耐寒机壳
                        output.accept(FireproofRegistry.FIREPROOF_BRICK.get());
                        output.accept(FireproofRegistry.FIREPROOF_CASING_ITEM.get());
                        output.accept(FireproofRegistry.COLD_RESISTANT_CASING_ITEM.get());
                        // 团队研究 / 科技树
                        output.accept(ResearchRegistry.RESEARCH_STATION_ITEM.get());
                        // 废土食物：炖菜/浓汤/罐头/肉干/蒸馏水/草药茶
                        output.accept(WastelandFoodRegistry.WASTELAND_STEW.get());
                        output.accept(WastelandFoodRegistry.WASTELAND_BROTH.get());
                        output.accept(WastelandFoodRegistry.CANNED_FOOD.get());
                        output.accept(WastelandFoodRegistry.EMPTY_CAN.get());
                        output.accept(WastelandFoodRegistry.DRIED_MEAT.get());
                        output.accept(WastelandFoodRegistry.INSTANT_NOODLES.get());
                        output.accept(WastelandFoodRegistry.PAPER_BOWL.get());
                        output.accept(WastelandFoodRegistry.HERBAL_TEA.get());
                        // 废土物资：绘制台 / 爆炸箭 / 西瓜皮 / 废旧物品
                        output.accept(WastelandRegistry.DRAWING_TABLE_ITEM.get());
                        output.accept(WastelandRegistry.EXPLOSIVE_ARROW.get());
                        output.accept(WastelandRegistry.WATERMELON_RIND.get());
                        output.accept(WastelandRegistry.ROTTEN_MEAL_BOWL.get());
                        output.accept(WastelandRegistry.ROTTEN_MEAT_BOWL.get());
                        for (var scrap : WastelandRegistry.scrapItems()) {
                            output.accept(scrap.get());
                        }
                        // 夜视装备（night_vision_device 模组，合并于本 jar）
                        output.accept(NVItems.NIGHT_VISION_DEVICE.get());
                        // 火种与引火物：袜子/绒毛/涂蜡纸板
                        output.accept(io.github.createmeow.timex_rebirth.features.FireToolRegistry.TORN_SOCKS.get());
                        output.accept(io.github.createmeow.timex_rebirth.features.FireToolRegistry.WET_TORN_SOCKS.get());
                        output.accept(io.github.createmeow.timex_rebirth.features.FireToolRegistry.FUZZ.get());
                        output.accept(io.github.createmeow.timex_rebirth.features.FireToolRegistry.WET_FUZZ.get());
                        output.accept(io.github.createmeow.timex_rebirth.features.FireToolRegistry.LIT_FUZZ.get());
                        output.accept(io.github.createmeow.timex_rebirth.features.FireToolRegistry.WAXED_CARDBOARD.get());
                        output.accept(io.github.createmeow.timex_rebirth.features.FireToolRegistry.LIT_WAXED_CARDBOARD.get());
                        output.accept(io.github.createmeow.timex_rebirth.features.FireToolRegistry.ASH.get());
                        output.accept(io.github.createmeow.timex_rebirth.features.FireToolRegistry.USED_LIGHTER.get());
                        output.accept(io.github.createmeow.timex_rebirth.features.FireToolRegistry.MOLTEN_TALLOW_BUCKET.get());
                        output.accept(io.github.createmeow.timex_rebirth.features.FireToolRegistry.MOLTEN_HONEYCOMB_BUCKET.get());
                        // 生火/晾晒：枝条 + 干枝条 + 晾晒架
                        output.accept(io.github.createmeow.timex_rebirth.features.FireToolRegistry.TWIG.get());
                        output.accept(io.github.createmeow.timex_rebirth.features.FireToolRegistry.DRY_TWIG.get());
                        output.accept(io.github.createmeow.timex_rebirth.features.FireToolRegistry.DRYING_RACK_ITEM.get());
                        output.accept(io.github.createmeow.timex_rebirth.features.KindlingRegistry.DRY_KINDLING_ITEM.get());
                        // 燧石工具体系：草绳 + 燧石镐/斧/锹/锄
                        output.accept(io.github.createmeow.timex_rebirth.features.flint.FlintGearRegistry.GRASS_ROPE.get());
                        output.accept(io.github.createmeow.timex_rebirth.features.flint.FlintGearRegistry.FLINT_PICKAXE.get());
                        output.accept(io.github.createmeow.timex_rebirth.features.flint.FlintGearRegistry.FLINT_AXE.get());
                        output.accept(io.github.createmeow.timex_rebirth.features.flint.FlintGearRegistry.FLINT_SHOVEL.get());
                        output.accept(io.github.createmeow.timex_rebirth.features.flint.FlintGearRegistry.FLINT_HOE.get());
                        // 铝矿物链：粗铝/粗铝块/铝矿石/深层铝矿石/铝锭/铝粒/铝板/铝块
                        output.accept(io.github.createmeow.timex_rebirth.features.flint.FlintGearRegistry.RAW_ALUMINUM.get());
                        output.accept(io.github.createmeow.timex_rebirth.features.flint.FlintGearRegistry.RAW_ALUMINUM_BLOCK_ITEM.get());
                        output.accept(io.github.createmeow.timex_rebirth.features.flint.FlintGearRegistry.ALUMINUM_ORE_ITEM.get());
                        output.accept(io.github.createmeow.timex_rebirth.features.flint.FlintGearRegistry.DEEPSLATE_ALUMINUM_ORE_ITEM.get());
                        output.accept(io.github.createmeow.timex_rebirth.features.flint.FlintGearRegistry.ALUMINUM_INGOT.get());
                        output.accept(io.github.createmeow.timex_rebirth.features.flint.FlintGearRegistry.ALUMINUM_NUGGET.get());
                        output.accept(io.github.createmeow.timex_rebirth.features.flint.FlintGearRegistry.ALUMINUM_SHEET.get());
                        output.accept(io.github.createmeow.timex_rebirth.features.flint.FlintGearRegistry.ALUMINUM_BLOCK_ITEM.get());
                        // 水下探索插件：防寒装备 + 铝背罐
                        com.createmeow.underwaterplugin.UnderwaterRegisters.addToTab(output);
                        if (com.createmeow.underwaterplugin.UnderwaterCreateRegisters.createLoaded()) {
                            com.createmeow.underwaterplugin.UnderwaterCreateRegisters.addToTab(output);
                        }
                    })
                    .build());

    public static void register(IEventBus modEventBus) {
        // 流体类型/流体必须先于方块注册：HeatFluxBlock 构造引用 HEAT_FLUX
        FLUID_TYPES.register(modEventBus);
        FLUIDS.register(modEventBus);
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        TABS.register(modEventBus);
    }

    /** 接收器方块实体暴露流体储罐能力（Create 管道可直接对接）。 */
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                HEAT_RECEIVER_BE.get(),
                (be, side) -> be.getTank());
    }
}
