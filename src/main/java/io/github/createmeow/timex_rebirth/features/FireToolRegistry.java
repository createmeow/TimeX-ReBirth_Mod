package io.github.createmeow.timex_rebirth.features;

import io.github.createmeow.timex_rebirth.TimeX;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * 火种与引火物：
 * <ul>
 *   <li><b>粉尘燃烧线</b>：破袜子（可穿戴/可放置）→ 剑刮出绒毛 → 绒毛堆（海龟蛋式 1~4 叠）+
 *       燧石生火（20%×数量）；潮湿状态（雪天露天/沾水）不可点火。</li>
 *   <li><b>涂蜡纸板快速生火线</b>：融化油脂/蜜脾（Create 虚拟流体，250mb/物品）+ Create 纸板
 *       → 涂蜡纸板（80s 引火棒：放置点燃、手持灼伤、SHIFT+右键引火、可作熔炉燃料）。</li>
 * </ul>
 */
public class FireToolRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TimeX.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TimeX.MODID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, TimeX.MODID);
    public static final DeferredRegister<net.neoforged.neoforge.fluids.FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.FLUID_TYPES, TimeX.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, TimeX.MODID);
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, TimeX.MODID);

    // ── 物品：破袜子系列 ──
    /** 破袜子：不可堆叠（有耐久）；穿戴在脚部或放置为方块。 */
    public static final DeferredItem<Item> TORN_SOCKS =
            ITEMS.register("torn_socks", () -> new TornSocksItem(new Properties().durability(59).stacksTo(1)));
    /** 湿水的破袜子：篝火 10s / 熔炉 5s 烤干。 */
    public static final DeferredItem<Item> WET_TORN_SOCKS =
            ITEMS.register("wet_torn_socks", () -> new Item(new Properties().durability(59).stacksTo(1)));

    // ── 物品：绒毛系列 ──
    /** 潮湿的绒毛：不可作点火原、不可放置。 */
    public static final DeferredItem<Item> WET_FUZZ =
            ITEMS.register("wet_fuzz", () -> new Item(new Properties()));
    /** 点燃的绒毛：绒毛堆被明火点燃时掉落，耐久=绒毛数量×10（1~4 绒毛=10~40 耐久）。 */
    public static final DeferredItem<LitFuzzItem> LIT_FUZZ =
            ITEMS.register("lit_fuzz", () -> new LitFuzzItem(new Properties().durability(LitFuzzItem.MAX_DURABILITY).stacksTo(1).rarity(Rarity.UNCOMMON)));

    // ── 物品：涂蜡纸板 ──
    /** 涂蜡的纸板：80s 引火棒（耐久 80 = 燃烧秒数），点燃后不因熄灭重置；不可堆叠（有耐久）。 */
    public static final DeferredItem<WaxedCardboardItem> WAXED_CARDBOARD =
            ITEMS.register("waxed_cardboard", () -> new WaxedCardboardItem(new Properties().durability(80).stacksTo(1).rarity(Rarity.UNCOMMON)));
    /** 点燃的涂蜡纸板：燃烧状态（同耐久数据组件延续）。 */
    public static final DeferredItem<WaxedCardboardItem> LIT_WAXED_CARDBOARD =
            ITEMS.register("lit_waxed_cardboard", () -> new WaxedCardboardItem(new Properties().durability(80).stacksTo(1).rarity(Rarity.UNCOMMON)));

    // ── 物品：灰烬（点燃的纸板烧尽产物）──
    public static final DeferredItem<Item> ASH =
            ITEMS.register("ash", () -> new Item(new Properties()));

    // ── 物品：用完的打火机 ──
    /** 用完的打火机：64 耐久的凑合火源，右键绒毛堆直接点燃（每次 1 耐久）；考古可挖出残存 5~20 耐久的。 */
    public static final DeferredItem<UsedLighterItem> USED_LIGHTER =
            ITEMS.register("used_lighter", () -> new UsedLighterItem(new Properties().durability(64).stacksTo(1)));

    // ── 物品：枝条 / 干枝条 / 堆肥枝条（树叶采集 + 晾晒架）──
/** 枝条：用剑/农夫乐事刀采集树叶概率掉落；在晾晒架上晒 20 秒变干枝条。 */
public static final DeferredItem<Item> TWIG =
        ITEMS.register("twig", () -> new Item(new Properties()));
/** 干枝条：4 个合成干燥的木条（dry_kindling）。 */
public static final DeferredItem<Item> DRY_TWIG =
        ITEMS.register("dry_twig", () -> new Item(new Properties()));
/** 堆肥枝条：采集树叶时掉落，堆肥值更高，可以直接添加到堆肥桶。 */
public static final DeferredItem<Item> COMPOST_TWIG =
        ITEMS.register("compost_twig", () -> new Item(new Properties()));

    // ── 虚拟流体：融化的油脂 / 融化的蜜脾（无世界方块，仅管道/盆/喷嘴）──
    public static final DeferredHolder<FluidType, FluidType>
            MOLTEN_TALLOW_TYPE = FLUID_TYPES.register("molten_tallow", () -> new FluidType(FluidType.Properties.create()
            .descriptionId("fluid.timex_rebirth.molten_tallow")
            .viscosity(1500).density(1200).temperature(60).canSwim(false).canDrown(false)));
    public static final DeferredHolder<Fluid, MoltenFluid> MOLTEN_TALLOW =
            FLUIDS.register("molten_tallow", () -> new MoltenFluid.TallowSource(MoltenFluid.TallowProps.create()));
    public static final DeferredHolder<Fluid, MoltenFluid> MOLTEN_TALLOW_FLOWING =
            FLUIDS.register("molten_tallow_flowing", () -> new MoltenFluid.TallowFlowing(MoltenFluid.TallowProps.create()));

    public static final DeferredHolder<FluidType, FluidType>
            MOLTEN_HONEYCOMB_TYPE = FLUID_TYPES.register("molten_honeycomb", () -> new FluidType(FluidType.Properties.create()
            .descriptionId("fluid.timex_rebirth.molten_honeycomb")
            .viscosity(1500).density(1300).temperature(50).canSwim(false).canDrown(false)));
    public static final DeferredHolder<Fluid, MoltenFluid> MOLTEN_HONEYCOMB =
            FLUIDS.register("molten_honeycomb", () -> new MoltenFluid.HoneycombSource(MoltenFluid.HoneycombProps.create()));
    public static final DeferredHolder<Fluid, MoltenFluid> MOLTEN_HONEYCOMB_FLOWING =
            FLUIDS.register("molten_honeycomb_flowing", () -> new MoltenFluid.HoneycombFlowing(MoltenFluid.HoneycombProps.create()));

    // ── 桶装流体（1000mb，像原版牛奶桶一样仅作为容器物品）──
    public static final DeferredItem<BucketItem> MOLTEN_TALLOW_BUCKET =
            ITEMS.register("molten_tallow_bucket", () -> new BucketItem(MOLTEN_TALLOW.get(),
                    new Properties().stacksTo(1).craftRemainder(net.minecraft.world.item.Items.BUCKET).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<BucketItem> MOLTEN_HONEYCOMB_BUCKET =
            ITEMS.register("molten_honeycomb_bucket", () -> new BucketItem(MOLTEN_HONEYCOMB.get(),
                    new Properties().stacksTo(1).craftRemainder(net.minecraft.world.item.Items.BUCKET).rarity(Rarity.UNCOMMON)));

    // ── 方块 ──
    /** 晾晒架：四根木棍合成，晾晒枝条 20 秒 → 干枝条。 */
    public static final DeferredBlock<DryingRackBlock> DRYING_RACK =
            BLOCKS.register("drying_rack", () -> new DryingRackBlock(
                    BlockBehaviour.Properties.of().strength(0.3F).sound(SoundType.WOOD).noOcclusion()));
    /** 晾晒架 BlockItem。 */
    public static final DeferredItem<net.minecraft.world.item.BlockItem> DRYING_RACK_ITEM =
            ITEMS.register("drying_rack", () -> new net.minecraft.world.item.BlockItem(DRYING_RACK.get(), new Properties()));
    /** 破袜子（地上）：剑刮出绒毛。 */
    public static final DeferredBlock<TornSocksBlock> TORN_SOCKS_BLOCK =
            BLOCKS.register("torn_socks_block", () -> new TornSocksBlock(
                    BlockBehaviour.Properties.of().strength(0.2F).sound(SoundType.WOOL).noOcclusion()));
    /** 绒毛堆：海龟蛋式 1~4 叠，燧石生火。 */
    public static final DeferredBlock<FuzzPileBlock> FUZZ_PILE =
            BLOCKS.register("fuzz_pile", () -> new FuzzPileBlock(
                    BlockBehaviour.Properties.of().strength(0.05F).sound(SoundType.WOOL)
                            .ignitedByLava().noOcclusion()));
    /** 绒毛 = 绒毛方块的 BlockItem（放置后海龟蛋式堆叠 1~4）。 */
    public static final DeferredItem<net.minecraft.world.item.BlockItem> FUZZ =
            ITEMS.register("fuzz", () -> new net.minecraft.world.item.BlockItem(FUZZ_PILE.get(), new Properties()));
    /** 未点燃的涂蜡纸板（放置等待外部热源）：存剩余秒数于 BE 附件。 */
    public static final DeferredBlock<UnlitWaxedCardboardBlock> WAXED_CARDBOARD_BLOCK =
            BLOCKS.register("waxed_cardboard_block", () -> new UnlitWaxedCardboardBlock(
                    BlockBehaviour.Properties.of().strength(0.1F).sound(SoundType.WOOD).noOcclusion()));
    /** 点燃的涂蜡纸板（地上燃烧）：80s，存剩余秒数于 BE 附件。 */
    public static final DeferredBlock<LitWaxedCardboardBlock> LIT_WAXED_CARDBOARD_BLOCK =
            BLOCKS.register("lit_waxed_cardboard", () -> new LitWaxedCardboardBlock(
                    BlockBehaviour.Properties.of().strength(0.1F).sound(SoundType.WOOD)
                            .noOcclusion().lightLevel(s -> 13)));

    // ── 方块实体 ──
    public static final DeferredRegister<net.minecraft.world.level.block.entity.BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE, TimeX.MODID);
    public static final DeferredHolder<net.minecraft.world.level.block.entity.BlockEntityType<?>,
            net.minecraft.world.level.block.entity.BlockEntityType<UnlitWaxedCardboardBlockEntity>> WAXED_CARDBOARD_BE =
            BLOCK_ENTITIES.register("waxed_cardboard_block", () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder
                    .of(UnlitWaxedCardboardBlockEntity::new, WAXED_CARDBOARD_BLOCK.get()).build(null));
    public static final DeferredHolder<net.minecraft.world.level.block.entity.BlockEntityType<?>,
            net.minecraft.world.level.block.entity.BlockEntityType<LitWaxedCardboardBlockEntity>> LIT_WAXED_CARDBOARD_BE =
            BLOCK_ENTITIES.register("lit_waxed_cardboard", () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder
                    .of(LitWaxedCardboardBlockEntity::new, LIT_WAXED_CARDBOARD_BLOCK.get()).build(null));
    public static final DeferredHolder<net.minecraft.world.level.block.entity.BlockEntityType<?>,
            net.minecraft.world.level.block.entity.BlockEntityType<TornSocksBlockEntity>> TORN_SOCKS_BE =
            BLOCK_ENTITIES.register("torn_socks_block", () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder
                    .of(TornSocksBlockEntity::new, TORN_SOCKS_BLOCK.get()).build(null));
    public static final DeferredHolder<net.minecraft.world.level.block.entity.BlockEntityType<?>,
            net.minecraft.world.level.block.entity.BlockEntityType<DryingRackBlockEntity>> DRYING_RACK_BE =
            BLOCK_ENTITIES.register("drying_rack", () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder
                    .of(DryingRackBlockEntity::new, DRYING_RACK.get()).build(null));

    // ── 干燥配方：数据驱动（参考 StoneAge DryingRackRecipe）──
    public static final DeferredHolder<RecipeType<?>, RecipeType<DryingRackRecipe>> DRYING_RACK_TYPE =
            RECIPE_TYPES.register("drying_rack", () -> RecipeType.simple(TimeX.rl("drying_rack")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<DryingRackRecipe>> DRYING_RACK_SERIALIZER =
            RECIPE_SERIALIZERS.register("drying_rack", DryingRackRecipe.Serializer::new);

    /** 注册入口。 */
    public static void register(IEventBus modEventBus) {
        FLUID_TYPES.register(modEventBus);
        FLUIDS.register(modEventBus);
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        RECIPE_TYPES.register(modEventBus);
        RECIPE_SERIALIZERS.register(modEventBus);
    }

    // ── 虚拟流体基类（仿 HerbalTeaFluid：无方块、仅管道/盆/喷嘴使用）──
    public abstract static class MoltenFluid extends BaseFlowingFluid {
        protected MoltenFluid(Properties properties) {
            super(properties);
        }

        /** 油脂流体：可桶装（getBucket 返回桶物品供 Create 盆/排液口识别）。子类覆写。 */
        public abstract Item getBucket();

        /** 油脂流体的 Properties（source/flowing 引用在静态初始化后由各流体类传入）。 */
        public static class TallowProps {
            static Properties create() {
                return new Properties(MOLTEN_TALLOW_TYPE, MOLTEN_TALLOW, MOLTEN_TALLOW_FLOWING)
                        .slopeFindDistance(4)
                        .levelDecreasePerBlock(1)
                        .explosionResistance(100.0F)
                        .tickRate(5);
            }
        }

        /** 蜜脾流体的 Properties。 */
        public static class HoneycombProps {
            static Properties create() {
                return new Properties(MOLTEN_HONEYCOMB_TYPE, MOLTEN_HONEYCOMB, MOLTEN_HONEYCOMB_FLOWING)
                        .slopeFindDistance(4)
                        .levelDecreasePerBlock(1)
                        .explosionResistance(100.0F)
                        .tickRate(5);
            }
        }

        /** 油脂源。 */
        public static class TallowSource extends MoltenFluid {
            public TallowSource(Properties properties) {
                super(properties);
            }

            @Override
            public Item getBucket() {
                return MOLTEN_TALLOW_BUCKET.get();
            }

            @Override
            public int getAmount(net.minecraft.world.level.material.FluidState state) {
                return 8;
            }

            @Override
            public boolean isSource(net.minecraft.world.level.material.FluidState state) {
                return true;
            }
        }

        /** 油脂流动态。 */
        public static class TallowFlowing extends MoltenFluid {
            public TallowFlowing(Properties properties) {
                super(properties);
                registerDefaultState(getStateDefinition().any().setValue(LEVEL, 7));
            }

            @Override
            public Item getBucket() {
                return MOLTEN_TALLOW_BUCKET.get();
            }

            @Override
            protected void createFluidStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Fluid, net.minecraft.world.level.material.FluidState> builder) {
                super.createFluidStateDefinition(builder);
                builder.add(LEVEL);
            }

            @Override
            public int getAmount(net.minecraft.world.level.material.FluidState state) {
                return state.getValue(LEVEL);
            }

            @Override
            public boolean isSource(net.minecraft.world.level.material.FluidState state) {
                return false;
            }
        }

        /** 蜜脾源。 */
        public static class HoneycombSource extends MoltenFluid {
            public HoneycombSource(Properties properties) {
                super(properties);
            }

            @Override
            public Item getBucket() {
                return MOLTEN_HONEYCOMB_BUCKET.get();
            }

            @Override
            public int getAmount(net.minecraft.world.level.material.FluidState state) {
                return 8;
            }

            @Override
            public boolean isSource(net.minecraft.world.level.material.FluidState state) {
                return true;
            }
        }

        /** 蜜脾流动态。 */
        public static class HoneycombFlowing extends MoltenFluid {
            public HoneycombFlowing(Properties properties) {
                super(properties);
                registerDefaultState(getStateDefinition().any().setValue(LEVEL, 7));
            }

            @Override
            public Item getBucket() {
                return MOLTEN_HONEYCOMB_BUCKET.get();
            }

            @Override
            protected void createFluidStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Fluid, net.minecraft.world.level.material.FluidState> builder) {
                super.createFluidStateDefinition(builder);
                builder.add(LEVEL);
            }

            @Override
            public int getAmount(net.minecraft.world.level.material.FluidState state) {
                return state.getValue(LEVEL);
            }

            @Override
            public boolean isSource(net.minecraft.world.level.material.FluidState state) {
                return false;
            }
        }
    }
}
