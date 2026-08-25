package io.github.createmeow.timex_rebirth.food;

import io.github.createmeow.timex_rebirth.TimeX;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * 废土食物注册：提升生存沉浸感的食物体系。
 * - 废土炖菜/浓汤：碗装热食，食用后返还碗，加入 heating_food 标签（热食驱寒，联动 Cold Sweat 体温）
 * - 罐头食品：耐储存补给，食用后恢复理智（RealityValue 联动），返还空铁罐（可烧炼回收铁锭）
 * - 肉干：烟熏耐储存食品
 * - 草药茶：加热搅拌草药+水制出草药茶液，玩家用玻璃瓶装取饮用（联动 Thirst 纯净度、RealityValue 理智）
 */
public class WastelandFoodRegistry {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TimeX.MODID);
    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.FLUID_TYPES, TimeX.MODID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, TimeX.MODID);

    // ── 草药茶流体（Create 虚拟流体式：无方块、无桶，仅供加热搅拌/装瓶/管道运输）──
    // 命名含 "tea"：ThirstWasTaken 的 MixinBasinRecipe 按 "tea" 匹配输出流体并继承输入水的纯净度
    public static final DeferredHolder<FluidType, FluidType> HERBAL_TEA_FLUID_TYPE =
            FLUID_TYPES.register("herbal_tea", () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("fluid.timex_rebirth.herbal_tea")
                    .viscosity(1500)
                    .density(1000)
                    .canSwim(false)
                    .canDrown(false)));
    public static final DeferredHolder<Fluid, HerbalTeaFluid> HERBAL_TEA_FLUID =
            FLUIDS.register("herbal_tea", () -> new HerbalTeaFluid.Source(HerbalTeaFluid.createProperties()));
    public static final DeferredHolder<Fluid, HerbalTeaFluid> HERBAL_TEA_FLUID_FLOWING =
            FLUIDS.register("herbal_tea_flowing", () -> new HerbalTeaFluid.Flowing(HerbalTeaFluid.createProperties()));

    // ── 碗装热食（食用后返还碗）──
    public static final DeferredItem<BowlFoodItem> WASTELAND_STEW =
            ITEMS.register("wasteland_stew", () -> new BowlFoodItem(new Item.Properties()
                    .stacksTo(1)
                    .food(new FoodProperties.Builder().nutrition(10).saturationModifier(12.0F).build())));
    public static final DeferredItem<BowlFoodItem> WASTELAND_BROTH =
            ITEMS.register("wasteland_broth", () -> new BowlFoodItem(new Item.Properties()
                    .stacksTo(1)
                    .food(new FoodProperties.Builder().nutrition(6).saturationModifier(7.2F).build())));

    // ── 耐储存食品 ──
    // 空铁罐：罐头吃完返还的容器，可烧炼回收 1 个铁锭
    public static final DeferredItem<Item> EMPTY_CAN =
            ITEMS.register("empty_can", () -> new Item(new Item.Properties()));
    public static final DeferredItem<CanFoodItem> CANNED_FOOD =
            ITEMS.register("canned_food", () -> new CanFoodItem(new Item.Properties()
                    .food(new FoodProperties.Builder().nutrition(6).saturationModifier(8.0F).build())));
    public static final DeferredItem<Item> DRIED_MEAT =
            ITEMS.register("dried_meat", () -> new Item(new Item.Properties()
                    .food(new FoodProperties.Builder().nutrition(5).saturationModifier(6.0F).build())));

    // ── 长保质期食品：方便面（纸碗封装）──
    // 纸碗：方便面的封装容器，食用后返还；可加热搅拌回收成纸浆（联动 Create 纸浆/纸板体系）
    public static final DeferredItem<Item> PAPER_BOWL =
            ITEMS.register("paper_bowl", () -> new Item(new Item.Properties()));
    public static final DeferredItem<InstantNoodlesItem> INSTANT_NOODLES =
            ITEMS.register("instant_noodles", () -> new InstantNoodlesItem(new Item.Properties()
                    .food(new FoodProperties.Builder().nutrition(6).saturationModifier(7.0F).build())));

    // ── 饮品（瓶装，饮用后返还玻璃瓶）──
    // alwaysEat：饱食度满时也可饮用（参考机械动力建筑工茶饮）
    public static final DeferredItem<BottleDrinkItem> HERBAL_TEA =
            ITEMS.register("herbal_tea", () -> new BottleDrinkItem(new Item.Properties()
                    .stacksTo(1)
                    .food(new FoodProperties.Builder().nutrition(2).saturationModifier(2.0F)
                            .alwaysEdible().build())));

    public static void register(IEventBus modEventBus) {
        FLUID_TYPES.register(modEventBus);
        FLUIDS.register(modEventBus);
        ITEMS.register(modEventBus);
    }

    /** 碗装食物：食用后返还一个碗（参考原版蘑菇煲）。 */
    public static class BowlFoodItem extends Item {
        public BowlFoodItem(Properties properties) {
            super(properties);
        }

        @Override
        public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
            ItemStack result = super.finishUsingItem(stack, level, entity);
            return entity instanceof Player player && player.getAbilities().instabuild
                    ? result : new ItemStack(Items.BOWL);
        }
    }

    /**
     * 罐头食品：食用后返还一个空铁罐（可烧炼回收铁锭）。
     * 参考农夫乐德 ConsumableItem：吃完若手中还有剩余（堆叠>1），
     * 空铁罐单独放入背包（背包满则掉落），而不是替换手中的罐头堆。
     */
    public static class CanFoodItem extends Item {
        public CanFoodItem(Properties properties) {
            super(properties);
        }

        @Override
        public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity consumer) {
            // 食物效果（饥饿值/状态效果）由父类处理
            if (stack.getFoodProperties(consumer) != null) {
                super.finishUsingItem(stack, level, consumer);
            }
            ItemStack containerStack = new ItemStack(EMPTY_CAN.get());
            if (stack.isEmpty()) {
                return containerStack;
            }
            if (consumer instanceof Player player && !player.getAbilities().instabuild) {
                if (!player.getInventory().add(containerStack)) {
                    player.drop(containerStack, false);
                }
            }
            return stack;
        }
    }

    /**
     * 方便面：纸碗封装的长保质期面食。
     * 食用后返还一个纸碗：可复用于炖锅制作方便面，或加热搅拌回收成纸浆（再压片回纸板），
     * 避免"无损返还纸板"——回收需消耗燃料与时间。
     */
    public static class InstantNoodlesItem extends Item {
        public InstantNoodlesItem(Properties properties) {
            super(properties);
        }

        @Override
        public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity consumer) {
            // 食物效果（饥饿值/状态效果）由父类处理
            if (stack.getFoodProperties(consumer) != null) {
                super.finishUsingItem(stack, level, consumer);
            }
            ItemStack cup = new ItemStack(PAPER_BOWL.get());
            if (stack.isEmpty()) {
                return cup;
            }
            if (consumer instanceof Player player && !player.getAbilities().instabuild) {
                if (!player.getInventory().add(cup)) {
                    player.drop(cup, false);
                }
            }
            return stack;
        }
    }

    /** 瓶装饮品：饮用动画 + 饮用后返还一个玻璃瓶（参考原版蜂蜜瓶）。 */
    public static class BottleDrinkItem extends Item {
        public BottleDrinkItem(Properties properties) {
            super(properties);
        }

        @Override
        public UseAnim getUseAnimation(ItemStack stack) {
            return UseAnim.DRINK;
        }

        @Override
        public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
            ItemStack result = super.finishUsingItem(stack, level, entity);
            return entity instanceof Player player && player.getAbilities().instabuild
                    ? result : new ItemStack(Items.GLASS_BOTTLE);
        }
    }

    /**
     * 草药茶流体（无方块、无桶的"虚拟"流体，仿 Create 建筑工茶饮）：
     * 仅存在于 Create 盆地/管道/储罐系统，由加热搅拌草药+水产出，
     * 玩家用玻璃瓶经 Create 装瓶（filling）获得瓶装草药茶。
     */
    public abstract static class HerbalTeaFluid extends BaseFlowingFluid {
        protected HerbalTeaFluid(Properties properties) {
            super(properties);
        }

        public static Properties createProperties() {
            return new Properties(
                    HERBAL_TEA_FLUID_TYPE,
                    HERBAL_TEA_FLUID,
                    HERBAL_TEA_FLUID_FLOWING)
                    .slopeFindDistance(4)
                    .levelDecreasePerBlock(1)
                    .explosionResistance(100.0F)
                    .tickRate(5);
        }

        /** 草药茶源。 */
        public static class Source extends HerbalTeaFluid {
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

        /** 草药茶流动态（本流体无方块不会放置到世界，此状态仅为满足注册表结构）。 */
        public static class Flowing extends HerbalTeaFluid {
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
