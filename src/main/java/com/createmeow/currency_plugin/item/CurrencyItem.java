package com.createmeow.currency_plugin.item;

import net.minecraft.world.item.ItemStack;

/**
 * 所有可作为货币的物品（钱币等）实现此接口。
 * 货币物品接口。
 */
public interface CurrencyItem {
    boolean wasAdjusted(ItemStack stack);

    long getValue(ItemStack stack);

    long[] getCombinedValue(ItemStack stack);
}
