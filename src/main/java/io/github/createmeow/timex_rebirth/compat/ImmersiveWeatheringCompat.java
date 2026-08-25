package io.github.createmeow.timex_rebirth.compat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.fml.ModList;

/**
 * Immersive Weathering 联动（反射/注册表查询，无编译期依赖）。
 * - 获取冻土方块（permafrost / grassy_permafrost）
 * - 战利品表替换：冻土只掉自身，不再掉金粒/铁粒/骨头
 */
public class ImmersiveWeatheringCompat {
    public static final String MOD_ID = "immersive_weathering";

    private static Block permafrost;
    private static Block grassyPermafrost;
    private static boolean resolved = false;

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    public static void resolveBlocks() {
        if (resolved) return;
        resolved = true;
        if (!isLoaded()) return;
        permafrost = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(MOD_ID, "permafrost"));
        grassyPermafrost = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(MOD_ID, "grassy_permafrost"));
    }

    public static Block getPermafrost() {
        resolveBlocks();
        return permafrost;
    }

    public static Block getGrassyPermafrost() {
        resolveBlocks();
        return grassyPermafrost;
    }

    public static boolean isPermafrost(Block block) {
        if (block == null) return false;
        resolveBlocks();
        return block == permafrost || block == grassyPermafrost;
    }

    public static boolean isPermafrostItem(Item item) {
        if (item == null) return false;
        resolveBlocks();
        if (permafrost != null && item == permafrost.asItem()) return true;
        if (grassyPermafrost != null && item == grassyPermafrost.asItem()) return true;
        return false;
    }
}
