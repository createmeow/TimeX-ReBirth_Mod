package com.createmeow.cm_plugins;

import com.createmeow.currency_plugin.cap.CurrencyHolder;
import com.createmeow.currency_plugin.currency.Currency;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * 货币系统兼容层：直接对接 currency_plugin（与 cm_plugins 同 JAR，无需反射）。
 * <p>方法签名与原反射版本保持一致，供 ModCommands / PluginFeatures /
 * SpatialInventoryManager 等使用。</p>
 */
public final class CurrencyCompat {

    private CurrencyCompat() {
    }

    /** currency_plugin 与 cm_plugins 打包在同一个 JAR 中，始终可用。 */
    public static boolean isAvailable() {
        return true;
    }

    /**
     * Parse currency type name to Currency enum value.
     * @param name "common" / "rare"
     * @return Currency enum value, or null if invalid
     */
    public static Currency parseCurrency(String name) {
        for (Currency currency : Currency.values()) {
            if (currency.name().equalsIgnoreCase(name)) {
                return currency;
            }
        }
        createmeowsplugins.LOGGER.error("[CurrencyCompat] Failed to parse currency: {}", name);
        return null;
    }

    /** Get raw value for a currency type and count. */
    public static long getRawValue(Currency currency, long count) {
        return currency == null ? 0 : currency.getRawValue(count);
    }

    /** Get player's balance of the given currency (枚数，互不转换). */
    public static long getBalance(ServerPlayer player, Currency currency) {
        return CurrencyHolder.getBalance((Player) player, currency);
    }

    /** Add to player's balance of the given currency (枚数，互不转换). */
    public static void addBalance(ServerPlayer player, Currency currency, long count) {
        CurrencyHolder.addBalance((Player) player, currency, count);
    }

    /** Set player's balance of the given currency (枚数，互不转换). */
    public static void setBalance(ServerPlayer player, Currency currency, long count) {
        CurrencyHolder.setBalance((Player) player, currency, count);
    }

    /** Get player's total balance in raw value (腐空朽 + 归霜升×100)，仅供总值判断。 */
    public static long getValue(ServerPlayer player) {
        Player p = (Player) player;
        return CurrencyHolder.getBalance(p, Currency.COMMON)
                + CurrencyHolder.getBalance(p, Currency.RARE) * Currency.RARE_VALUE;
    }

    /**
     * Modify player's total balance in raw value.
     * 扣款（负数）按总值消耗：先腐空朽后归霜升，不找零；加款（正数）全部计入腐空朽。
     * @return 扣款时余额是否足够（加款恒为 true）
     */
    public static boolean modify(ServerPlayer player, long amount) {
        if (amount >= 0) {
            CurrencyHolder.addBalance((Player) player, Currency.COMMON, amount);
            return true;
        }
        return CurrencyHolder.spend((Player) player, -amount);
    }
}
