package io.github.createmeow.timex_rebirth.features.flint;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;

/**
 * IW（Immersive Weathering）树皮刮削掉落替换桥：
 * <ul>
 * <li>树木刮削 → {@code farmersdelight:tree_bark}</li>
 * <li>竹块刮削 → {@code farmersdelight:straw}</li>
 * <li>FD 未加载或物品不存在 → 保持 IW 原掉落</li>
 * </ul>
 * 不 import 任何 FD/IW 类，通过注册表按 id 取物品（类隔离）。
 * 供 {@code WeatheringHelperMixin}（getBarkToStrip / getBarkForStrippedLog）调用，
 * 后者同时让 FD 树皮/秸秆可以反向修复剥皮原木。
 */
public final class IwTreeBarkBridge {

    private static volatile Boolean fdLoaded = null;

    private IwTreeBarkBridge() {}

    /** 返回替换后的掉落物品；不可替换时返回原物品。 */
    public static Item replaceBark(BlockState state, Item original) {
        if (original == null || original == Items.AIR) return original;
        if (!fdLoaded()) return original;
        // 竹块（IW 唯一的竹类可刮削原木）→ FD 秸秆；其余树木 → FD 树皮
        Item replacement = state.is(Blocks.BAMBOO_BLOCK) ? fdItem("straw") : fdItem("tree_bark");
        return replacement == Items.AIR ? original : replacement;
    }

    private static boolean fdLoaded() {
        Boolean b = fdLoaded;
        if (b == null) {
            b = ModList.get().isLoaded("farmersdelight");
            fdLoaded = b;
        }
        return b;
    }

    private static Item fdItem(String name) {
        var item = BuiltInRegistries.ITEM.get(
                ResourceLocation.fromNamespaceAndPath("farmersdelight", name));
        return item == null ? Items.AIR : item;
    }
}
