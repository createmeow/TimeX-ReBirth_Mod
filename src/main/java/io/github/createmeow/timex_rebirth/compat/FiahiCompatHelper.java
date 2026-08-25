package io.github.createmeow.timex_rebirth.compat;

import com.hexagram2021.fiahi.register.FIAHIAttachmentTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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

    /** 会枯化为"枯萎的灌木"（原版枯死的灌木 minecraft:dead_bush）的种子。与 data 中的配方/tag 共用。 */
    public static final TagKey<Item> WITHERED_SEED_TAG =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("timex_rebirth", "withered_seed"));

    private FiahiCompatHelper() {
    }

    /** 是否为会枯化为"枯萎的灌木"的种子（腐烂或冻结到程度后变成 minecraft:dead_bush）。 */
    public static boolean isWitheredSeed(ItemStack stack) {
        return !stack.isEmpty() && stack.is(WITHERED_SEED_TAG);
    }

    /**
     * 若物品是种子且温度达到 ±100（腐烂或冻结到临界），通过 leftOverSetter 把它替换为
     * "枯萎的灌木"（原版枯死的灌木 minecraft:dead_bush）。leftOverSetter 负责容器层替换。
     * <p>
     * 阈值取 ±100 是为了抢在 FIAHI 自身转换（腐烂&gt;120 / 冻结 level3 ≈ ±125）之前触发：
     * 部分"既是食物又是种子"的物品带食物组件，FIAHI 会在温度&gt;120 时把它转成腐烂剩菜；
     * 提前到 ±100 替换成枯死的灌木，避免与 FIAHI 的剩菜转换冲突。
     */
    public static void witherFrozenSeed(ItemStack food, Consumer<ItemStack> leftOverSetter) {
        if (!isWitheredSeed(food)) return;
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

    /** 把 source 的温度（非 0）写入 target，实现"加工保留温度"。 */
    public static void copyTemperature(ItemStack source, ItemStack target) {
        if (source.isEmpty() || target.isEmpty()) return;
        int temp = getFoodTemperature(source);
        if (temp != 0) {
            target.set(FIAHIAttachmentTypes.FOOD_TEMPERATURE.get(), temp);
        }
    }

    /** 从容器中第一个带温度的物品复制温度到 target（合成台等场景）。 */
    public static void copyTemperatureFromContainer(Container container, ItemStack target) {
        if (target.isEmpty()) return;
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
        if (target.isEmpty() || temp == 0) return;
        target.set(FIAHIAttachmentTypes.FOOD_TEMPERATURE.get(), temp);
    }
}
