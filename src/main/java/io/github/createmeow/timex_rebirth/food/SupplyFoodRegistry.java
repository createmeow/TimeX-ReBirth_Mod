package io.github.createmeow.timex_rebirth.food;

import io.github.createmeow.timex_rebirth.TimeX;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * 生存补给食品注册：高密度口粮 + 电解质补水（供长途探索/废土旅行使用）。
 *
 * <ul>
 * <li><b>堆叠曲奇 stacked_cookies</b>：9 曲奇压制，工作台可逆向分解；食用消耗口渴（干粮特性，
 *     经 ThirstCompat 注册 thirst=-2）</li>
 * <li><b>压缩曲奇 cookies_zip</b>：堆叠曲奇经 Create 冲压压实；食用消耗口渴（thirst=-4）</li>
 * <li><b>糖块 sugar_block</b>：4 糖合成，可作能量棒原料（食物属性同曲奇：营养 2 / 饱和 0.1）</li>
 * <li><b>能量棒 energy_bar</b>：2 糖块 + 曲奇，营养 10 / 饱和 10（saturationModifier 0.5，
 *     实际饱和 = min(10×0.5×2, 10) = 10 满额）</li>
 * <li><b>电解质水 electrolyte_water（流体）</b>：Create 搅拌盆 250mb 水 + 1 糖块产出；
 *     无方块无桶的"虚拟"流体（同草药茶），经 Create 装瓶（filling）获得瓶装电解质水</li>
 * <li><b>瓶装电解质水 electrolyte_water_bottle</b>：营养 2 / 饱和 2（原版饱和度上限 = 营养值，
 *     无法达到 3）；口渴联动恢复 10 口渴 + 15 解渴（ThirstCompat 注册）；
 *     饱食度满时也可饮用（alwaysEdible），饮用后返还玻璃瓶</li>
 * </ul>
 *
 * <p>饱和度换算说明：原版公式 实际饱和 = min(营养 × saturationModifier × 2, 营养)，
 * 故"营养 X 饱和 Y"（Y≥X）一律用 saturationModifier = Y/2X 且最终饱和封顶于营养值。</p>
 */
public class SupplyFoodRegistry {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TimeX.MODID);
    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.FLUID_TYPES, TimeX.MODID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, TimeX.MODID);

    // ── 高密度口粮 ──
    /** 堆叠曲奇：9 曲奇压制，恢复 9 个曲奇的饱食度（营养 9），消耗 2 口渴（ThirstCompat：thirst=-2）。 */
    public static final DeferredItem<Item> STACKED_COOKIES =
            ITEMS.register("stacked_cookies", () -> new Item(new Item.Properties()
                    .food(new FoodProperties.Builder().nutrition(9).saturationModifier(0.1F).build())));

    /** 压缩曲奇：堆叠曲奇冲压制成，恢复 10 个曲奇的饱食度（营养 10），消耗 4 口渴（thirst=-4）。 */
    public static final DeferredItem<Item> COOKIES_ZIP =
            ITEMS.register("cookies_zip", () -> new Item(new Item.Properties()
                    .food(new FoodProperties.Builder().nutrition(10).saturationModifier(0.1F).build())));

    /** 糖块：4 糖合成的食物，亦是能量棒原料。 */
    public static final DeferredItem<Item> SUGAR_BLOCK =
            ITEMS.register("sugar_block", () -> new Item(new Item.Properties()
                    .food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.1F).build())));

    /** 能量棒：2 糖块 + 曲奇，营养 10 / 饱和 10（saturationModifier 0.5 → 实际饱和满额 10）。 */
    public static final DeferredItem<Item> ENERGY_BAR =
            ITEMS.register("energy_bar", () -> new Item(new Item.Properties()
                    .food(new FoodProperties.Builder().nutrition(10).saturationModifier(0.5F).build())));

    // ── 电解质补水 ──
    /** 瓶装电解质水：营养 2 / 饱和 2（上限封顶）；口渴 +10 / 解渴 +15（ThirstCompat）；满食可饮，返还玻璃瓶。 */
    public static final DeferredItem<WastelandFoodRegistry.BottleDrinkItem> ELECTROLYTE_WATER_BOTTLE =
            ITEMS.register("electrolyte_water_bottle", () -> new WastelandFoodRegistry.BottleDrinkItem(new Item.Properties()
                    .stacksTo(1)
                    .food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.75F)
                            .alwaysEdible().build())));

    // ── 电解质水流体（Create 虚拟流体式：无方块、无桶，仅供搅拌产出/装瓶/管道运输）──
    public static final DeferredHolder<FluidType, FluidType> ELECTROLYTE_WATER_FLUID_TYPE =
            FLUID_TYPES.register("electrolyte_water", () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("fluid.timex_rebirth.electrolyte_water")
                    .viscosity(1000)
                    .density(1000)
                    .canSwim(false)
                    .canDrown(false)));
    public static final DeferredHolder<Fluid, ElectrolyteWaterFluid> ELECTROLYTE_WATER_FLUID =
            FLUIDS.register("electrolyte_water", () -> new ElectrolyteWaterFluid.Source(ElectrolyteWaterFluid.createProperties()));
    public static final DeferredHolder<Fluid, ElectrolyteWaterFluid> ELECTROLYTE_WATER_FLUID_FLOWING =
            FLUIDS.register("electrolyte_water_flowing", () -> new ElectrolyteWaterFluid.Flowing(ElectrolyteWaterFluid.createProperties()));

    public static void register(IEventBus modEventBus) {
        FLUID_TYPES.register(modEventBus);
        FLUIDS.register(modEventBus);
        ITEMS.register(modEventBus);
    }

    /**
     * 电解质水流体（无方块、无桶的"虚拟"流体，同 {@link WastelandFoodRegistry.HerbalTeaFluid}）：
     * 仅存在于 Create 盆地/管道/储罐系统，由搅拌盆 250mb 水 + 1 糖块产出，
     * 玩家用玻璃瓶经 Create 装瓶（filling）获得瓶装电解质水。
     */
    public abstract static class ElectrolyteWaterFluid extends BaseFlowingFluid {
        protected ElectrolyteWaterFluid(Properties properties) {
            super(properties);
        }

        public static Properties createProperties() {
            return new Properties(
                    ELECTROLYTE_WATER_FLUID_TYPE,
                    ELECTROLYTE_WATER_FLUID,
                    ELECTROLYTE_WATER_FLUID_FLOWING)
                    .slopeFindDistance(4)
                    .levelDecreasePerBlock(1)
                    .explosionResistance(100.0F)
                    .tickRate(5);
        }

        /** 电解质水源。 */
        public static class Source extends ElectrolyteWaterFluid {
            public Source(Properties properties) {
                super(properties);
            }

            @Override
            public int getAmount(FluidState state) {
                return 8;
            }

            @Override
            public boolean isSource(FluidState state) {
                return true;
            }
        }

        /** 电解质水流动态（本流体无方块不会放置到世界，此状态仅为满足注册表结构）。 */
        public static class Flowing extends ElectrolyteWaterFluid {
            public Flowing(Properties properties) {
                super(properties);
                registerDefaultState(getStateDefinition().any().setValue(LEVEL, 7));
            }

            @Override
            protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
                super.createFluidStateDefinition(builder);
                builder.add(LEVEL);
            }

            @Override
            public int getAmount(FluidState state) {
                return state.getValue(LEVEL);
            }

            @Override
            public boolean isSource(FluidState state) {
                return false;
            }
        }
    }
}
