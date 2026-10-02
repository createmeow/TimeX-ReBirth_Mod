package com.createmeow.currency_plugin.init;

import com.createmeow.currency_plugin.CurrencyPlugin;
import com.createmeow.currency_plugin.currency.Currency;
import com.createmeow.currency_plugin.item.CoinItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 钱币物品注册：普通钱币 / 稀有钱币。
 */
public class CurrencyItems {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(CurrencyPlugin.MODID);

    public static final DeferredItem<CoinItem> COMMON_COIN =
            ITEMS.register("common_coin", () -> new CoinItem(Currency.COMMON));

    public static final DeferredItem<CoinItem> RARE_COIN =
            ITEMS.register("rare_coin", () -> new CoinItem(Currency.RARE));

    // 提炼机序列组装中间产物（配方 refining_machine_assembled.json，带 create+thirst 条件）
    public static final DeferredItem<Item> INCOMPLETE_REFINING_MACHINE =
            ITEMS.register("incomplete_refining_machine", () -> new Item(new Item.Properties()));
}
