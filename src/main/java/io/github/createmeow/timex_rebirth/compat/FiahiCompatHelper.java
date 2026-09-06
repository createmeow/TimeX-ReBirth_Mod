package io.github.createmeow.timex_rebirth.compat;

import com.hexagram2021.fiahi.register.FIAHIAttachmentTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.Set;
import java.util.function.Consumer;

/**
 * Freeze-It-And-Heat-It（fiahi）联动辅助：
 * - {@link #isPerishableGrain}：麦类/面粉/面团/种子等"粮食"判定。
 *   原版 fiahi 只处理带食物组件（DataComponents.FOOD）且不在 fiahi:leftovers 的物品，
 *   这些粮食没有食物组件所以默认不参与温度/腐烂；本模组把它们也纳入腐烂体系。
 * - {@link #copyTemperature} / {@link #copyTemperatureFromContainer}：
 *   食品加工（合成/熔炼/营火）时把原料的 fiahi 温度组件（腐烂/冻结程度）保留到产物，
 *   避免加工后腐烂/冻结程度直接消失。
 */
public final class FiahiCompatHelper {

    /** 会腐烂的"粮食"（麦类/面粉/面团/种子）。含跨模组物品：农夫乐事、机械动力与本模组。 */
    private static final Set<ResourceLocation> PERISHABLE_GRAINS = Set.of(
            ResourceLocation.withDefaultNamespace("wheat"),
            ResourceLocation.withDefaultNamespace("wheat_seeds"),
            ResourceLocation.fromNamespaceAndPath("create", "wheat_flour"),
            ResourceLocation.fromNamespaceAndPath("create", "dough"),
            ResourceLocation.fromNamespaceAndPath("farmersdelight", "wheat_dough"),
            ResourceLocation.fromNamespaceAndPath("farmersdelight", "raw_pasta"),
            ResourceLocation.fromNamespaceAndPath("farmersdelight", "cabbage_seeds"),
            ResourceLocation.fromNamespaceAndPath("farmersdelight", "tomato_seeds"),
            ResourceLocation.fromNamespaceAndPath("farmersdelight", "onion"),
            ResourceLocation.fromNamespaceAndPath("farmersdelight", "rice"),
            ResourceLocation.fromNamespaceAndPath("farmersdelight", "rice_panicle"),
            ResourceLocation.fromNamespaceAndPath("timex_rebirth", "rye"),
            ResourceLocation.fromNamespaceAndPath("timex_rebirth", "rye_flour"),
            ResourceLocation.fromNamespaceAndPath("timex_rebirth", "rye_dough"),
            ResourceLocation.fromNamespaceAndPath("timex_rebirth", "rye_seeds"),
            ResourceLocation.fromNamespaceAndPath("timex_rebirth", "turnip_seeds")
    );

    /**
     * 不参与"温度继承"的密封/干燥保存食品：罐头食品、方便面。
     * 它们本身不会被冻结起效，但一旦被写入 FOOD_TEMPERATURE 组件，
     * 就会与无该组件的同类物品分裂成不同堆叠，占用背包格。
     */
    private static final Set<ResourceLocation> TEMPERATURE_EXEMPT_FOODS = Set.of(
            ResourceLocation.fromNamespaceAndPath("timex_rebirth", "canned_food"),
            ResourceLocation.fromNamespaceAndPath("timex_rebirth", "instant_noodles")
    );

    /** 会枯化为"枯萎的灌木"（原版枯死的灌木 minecraft:dead_bush）的种子。与 data 中的配方/tag 共用。 */
    public static final TagKey<Item> WITHERED_SEED_TAG =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("timex_rebirth", "withered_seed"));

    /** 原版"树苗"物品标签（minecraft:saplings）：涵盖各树苗。 */
    public static final TagKey<Item> SAPLINGS_ITEM =
            TagKey.create(Registries.ITEM, ResourceLocation.withDefaultNamespace("saplings"));

    /** 原版"鹦鹉食物"物品标签（minecraft:parrot_food）：涵盖各作物种子（胡萝卜等无独立种子的除外）。 */
    public static final TagKey<Item> PARROT_FOOD_ITEM =
            TagKey.create(Registries.ITEM, ResourceLocation.withDefaultNamespace("parrot_food"));

    /** 通用"种子"物品标签（c:seeds）：跨模组收集的更多种子。 */
    public static final TagKey<Item> C_SEEDS_ITEM =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "seeds"));

    private FiahiCompatHelper() {
    }

    /** fiahi 是否安装（guard：fiahi 未安装时不得引用其枚举常量/组件，否则会 NoClassDefFound）。 */
    public static boolean isLoaded() {
        return ModList.get().isLoaded("fiahi");
    }

    /** 是否为会枯化为"枯萎的灌木"的种子（腐烂或冻结到程度后变成 minecraft:dead_bush）。 */
    public static boolean isWitheredSeed(ItemStack stack) {
        return !stack.isEmpty() && isWitheredSeed(stack.getItem());
    }

    public static boolean isWitheredSeed(Item item) {
        return item != null && item.builtInRegistryHolder().is(WITHERED_SEED_TAG);
    }

    /**
     * 是否为"会种下去的植物材料"（种子/树苗）：参与温度、腐烂/冻结→枯灌木、种植→拔起保留温度。
     * 集合 = 本模组种子 tag + 通用 c:seeds + 原版作物种子（minecraft:parrot_food）+ 原版树苗（minecraft:saplings）+ 竹子。
     */
    public static boolean isSeedLike(ItemStack stack) {
        return !stack.isEmpty() && isSeedLike(stack.getItem());
    }

    public static boolean isSeedLike(Item item) {
        if (item == null) return false;
        if (isWitheredSeed(item)) return true; // 本模组/联动种子
        if (item.builtInRegistryHolder().is(C_SEEDS_ITEM)
                || item.builtInRegistryHolder().is(SAPLINGS_ITEM)
                || item.builtInRegistryHolder().is(PARROT_FOOD_ITEM)) return true; // 通用种子/原版树苗/作物种子
        if (item instanceof BlockItem blockItem) { // 树苗方块物品（含竹子）兜底
            Block block = blockItem.getBlock();
            if (block instanceof SaplingBlock || block == Blocks.BAMBOO_SAPLING) return true;
        }
        return false;
    }

    /**
     * 若物品是种子/树苗且温度达到 ±100（腐烂或冻结到临界），通过 leftOverSetter 把它替换为
     * "枯萎的灌木"（原版枯死的灌木 minecraft:dead_bush）。leftOverSetter 负责容器层替换。
     * <p>
     * 阈值取 ±100 是为了抢在 FIAHI 自身转换（腐烂&gt;120 / 冻结 level3 ≈ ±125）之前触发：
     * 部分"既是食物又是种子"的物品带食物组件，FIAHI 会在温度&gt;120 时把它转成腐烂剩菜；
     * 提前到 ±100 替换成枯死的灌木，避免与 FIAHI 的剩菜转换冲突。
     */
    public static void witherFrozenSeed(ItemStack food, Consumer<ItemStack> leftOverSetter) {
        if (!isSeedLike(food)) return;
        int temp = getFoodTemperature(food);
        if (temp >= 100 || temp <= -100) {
            leftOverSetter.accept(new ItemStack(Items.DEAD_BUSH));
        }
    }

    /** 是否为参与腐烂的"粮食"（按注册名匹配，跨模组可用）。 */
    public static boolean isPerishableGrain(Item item) {
        return item != null && PERISHABLE_GRAINS.contains(BuiltInRegistries.ITEM.getKey(item));
    }

    public static boolean isPerishableGrain(ItemStack stack) {
        return !stack.isEmpty() && isPerishableGrain(stack.getItem());
    }

    /** 读取物品的 fiahi 温度（腐烂/冻结程度），无组件视为 0。 */
    public static int getFoodTemperature(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        Integer temp = stack.get(FIAHIAttachmentTypes.FOOD_TEMPERATURE.get());
        return temp == null ? 0 : temp;
    }

    /** 是否为温度豁免物品（罐头/方便面等密封干燥食品）：不继承、不写入 fiahi 温度。 */
    public static boolean isTemperatureExempt(ItemStack stack) {
        return !stack.isEmpty()
                && TEMPERATURE_EXEMPT_FOODS.contains(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    /** 把 source 的温度（非 0）写入 target，实现"加工保留温度"。 */
    public static void copyTemperature(ItemStack source, ItemStack target) {
        if (source.isEmpty() || target.isEmpty() || isTemperatureExempt(target)) return;
        int temp = getFoodTemperature(source);
        if (temp != 0) {
            target.set(FIAHIAttachmentTypes.FOOD_TEMPERATURE.get(), temp);
        }
    }

    /** 从容器中第一个带温度的物品复制温度到 target（合成台等场景）。 */
    public static void copyTemperatureFromContainer(Container container, ItemStack target) {
        if (target.isEmpty() || isTemperatureExempt(target)) return;
        for (int i = 0; i < container.getContainerSize(); i++) {
            int temp = getFoodTemperature(container.getItem(i));
            if (temp != 0) {
                target.set(FIAHIAttachmentTypes.FOOD_TEMPERATURE.get(), temp);
                return;
            }
        }
    }

    /** 取 IItemHandler（如 Create 盆地输入、FD 厨锅物品栏）中第一个带温度的物品的温度。 */
    public static int getFirstTemperature(IItemHandler handler) {
        if (handler == null) return 0;
        for (int i = 0; i < handler.getSlots(); i++) {
            int temp = getFoodTemperature(handler.getStackInSlot(i));
            if (temp != 0) return temp;
        }
        return 0;
    }

    /** 给产物写入温度（非 0 才写，0 表示无腐烂/冻结程度）。 */
    public static void setTemperature(ItemStack target, int temp) {
        if (target.isEmpty() || temp == 0 || isTemperatureExempt(target)) return;
        target.set(FIAHIAttachmentTypes.FOOD_TEMPERATURE.get(), temp);
    }
}
