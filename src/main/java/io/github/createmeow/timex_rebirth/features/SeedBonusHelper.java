package io.github.createmeow.timex_rebirth.features;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 种子奖励池：营火烧炼冻土时小概率掉落随机作物种子。
 * 包含原版种子与 FarmersDelight 的种子。
 */
public class SeedBonusHelper {
    private static final List<ResourceLocation> SEED_IDS = new ArrayList<>();
    private static boolean initialized = false;

    public static void init() {
        if (initialized) return;
        initialized = true;
        // 原版种子
        add("minecraft:wheat_seeds");
        add("minecraft:beetroot_seeds");
        add("minecraft:pumpkin_seeds");
        add("minecraft:melon_seeds");
        add("minecraft:carrot");
        add("minecraft:potato");
        // FarmersDelight 种子（若安装）
        add("farmersdelight:cabbage_seeds");
        add("farmersdelight:tomato_seeds");
        add("farmersdelight:onion");
        add("farmersdelight:rice");
        // TimeX 冬季作物种子
        add("timex_rebirth:rye_seeds");
        add("timex_rebirth:turnip_seeds");
    }

    private static void add(String id) {
        SEED_IDS.add(ResourceLocation.parse(id));
    }

    /**
     * 随机返回一个种子物品，若无可返回的种子返回空栈。
     */
    public static ItemStack getRandomSeed(RandomSource random) {
        init();
        List<Item> items = new ArrayList<>();
        for (ResourceLocation id : SEED_IDS) {
            Item item = BuiltInRegistries.ITEM.get(id);
            if (item != null && item != BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace("air"))) {
                items.add(item);
            }
        }
        if (items.isEmpty()) return ItemStack.EMPTY;
        return new ItemStack(items.get(random.nextInt(items.size())));
    }
}
