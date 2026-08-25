package io.github.createmeow.timex_rebirth.heat.station;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * 热源供应站燃料判定（双燃料）：
 * - 固体燃料：任何可作为熔炉燃料的物品（NeoForge getBurnTime 判据），
 *   另加冷汗"锅炉"专用燃料中的岩浆块（原版不可燃烧但锅炉接受）。
 *   并集覆盖：煤、木炭、煤炭块、熔岩桶（原版熔炉燃料）+ 岩浆块（锅炉燃料）。
 * - 液体燃料：熔岩（Cold Sweat 锅炉同款燃料，经 Create 流体管道/泵注入）。
 */
public final class StationFuelUtil {

    private StationFuelUtil() {
    }

    /** 是否为热源供应站接受的固体燃料。 */
    public static boolean isSolidFuel(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.getBurnTime(RecipeType.SMELTING) > 0 || stack.is(Items.MAGMA_BLOCK);
    }

    /** 固体燃料的燃烧时长（tick）。非燃料返回 0。 */
    public static int getBurnTime(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        int burnTime = stack.getBurnTime(RecipeType.SMELTING);
        if (burnTime > 0) return burnTime;
        return stack.is(Items.MAGMA_BLOCK) ? 2000 : 0;
    }
}
