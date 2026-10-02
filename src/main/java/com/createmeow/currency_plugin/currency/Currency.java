package com.createmeow.currency_plugin.currency;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import com.createmeow.currency_plugin.init.CurrencyItems;

/**
 * 货币枚举：两级货币体系。
 * <ul>
 *   <li>{@link #COMMON} — 腐空朽，原始值 = 数量 × 1</li>
 *   <li>{@link #RARE} — 归霜升，原始值 = 数量 × 100</li>
 * </ul>
 */
public enum Currency implements ItemLike {
    COMMON {
        @Override
        public int getNameColor() {
            return 0x999999; // 灰色
        }

        @Override
        public long getRawValue(long amount) {
            return amount;
        }

        @Override
        public Item asItem() {
            return CurrencyItems.COMMON_COIN.get();
        }
    },
    RARE {
        @Override
        public int getNameColor() {
            return 0xFFAA00; // 金色
        }

        @Override
        public long getRawValue(long amount) {
            return amount * 100;
        }

        @Override
        public Item asItem() {
            return CurrencyItems.RARE_COIN.get();
        }
    };

    public static final int COMMON_VALUE = 1;
    public static final int RARE_VALUE = 100;

    /** 本地化显示名（腐空朽 / 归霜升），键：currency.currency_plugin.<name>。 */
    public net.minecraft.network.chat.Component displayName() {
        return net.minecraft.network.chat.Component.translatable("currency.currency_plugin." + name().toLowerCase());
    }

    public abstract int getNameColor();

    public abstract long getRawValue(long amount);
}
