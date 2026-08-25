package com.createmeow.cm_plugins;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Helper class for optional NumismaticOverhaul integration.
 * Uses reflection to avoid runtime dependency when the mod is not installed.
 */
public class NumismaticHelper {
    private static boolean checked = false;
    private static boolean available = false;
    private static Class<?> currencyClass = null;
    private static Class<?> currencyHolderClass = null;

    /**
     * Check if NumismaticOverhaul is loaded and available.
     */
    public static boolean isAvailable() {
        if (!checked) {
            checked = true;
            try {
                currencyClass = Class.forName("tallestred.numismaticoverhaul.currency.Currency");
                currencyHolderClass = Class.forName("tallestred.numismaticoverhaul.cap.CurrencyHolder");
                available = true;
                createmeowsplugins.LOGGER.info("[NumismaticHelper] NumismaticOverhaul detected, currency features enabled");
            } catch (ClassNotFoundException e) {
                createmeowsplugins.LOGGER.info("[NumismaticHelper] NumismaticOverhaul not installed, currency features disabled");
            }
        }
        return available;
    }

    /**
     * Parse currency type name to Currency enum value.
     * @param name "bronze", "silver", or "gold"
     * @return Currency enum value, or null if invalid
     */
    public static Object parseCurrency(String name) {
        if (!isAvailable()) return null;
        try {
            Object[] constants = (Object[]) currencyClass.getMethod("values").invoke(null);
            for (Object constant : constants) {
                String enumName = ((Enum<?>) constant).name();
                if (enumName.equalsIgnoreCase(name)) {
                    return constant;
                }
            }
        } catch (Exception e) {
            createmeowsplugins.LOGGER.error("[NumismaticHelper] Failed to parse currency: {}", name, e);
        }
        return null;
    }

    /**
     * Get raw value for a currency type and count.
     * @param currency Currency enum value
     * @param count Number of coins
     * @return Raw value
     */
    public static long getRawValue(Object currency, long count) {
        if (!isAvailable() || currency == null) return 0;
        try {
            return (long) currencyClass.getMethod("getRawValue", long.class).invoke(currency, count);
        } catch (Exception e) {
            createmeowsplugins.LOGGER.error("[NumismaticHelper] Failed to get raw value", e);
            return 0;
        }
    }

    /**
     * Get player's currency balance.
     * @param player Server player
     * @return Current balance in raw value
     */
    public static long getValue(ServerPlayer player) {
        if (!isAvailable()) return 0;
        try {
            // CurrencyHolder 的方法参数为 Player，而非 ServerPlayer；
            // 反射 getMethod 按参数类型精确匹配，必须用 Player.class 才能命中。
            return (long) currencyHolderClass.getMethod("getValue", Player.class).invoke(null, player);
        } catch (Exception e) {
            createmeowsplugins.LOGGER.error("[NumismaticHelper] Failed to get value", e);
            return 0;
        }
    }

    /**
     * Modify player's currency balance.
     * @param player Server player
     * @param amount Amount to add (negative to deduct)
     */
    public static void modify(ServerPlayer player, long amount) {
        if (!isAvailable()) return;
        try {
            currencyHolderClass.getMethod("modify", Player.class, long.class).invoke(null, player, amount);
        } catch (Exception e) {
            createmeowsplugins.LOGGER.error("[NumismaticHelper] Failed to modify currency", e);
        }
    }

    /**
     * Set player's currency balance.
     * @param player Server player
     * @param value New balance in raw value
     */
    public static void setValue(ServerPlayer player, long value) {
        if (!isAvailable()) return;
        try {
            currencyHolderClass.getMethod("setValue", Player.class, long.class).invoke(null, player, value);
        } catch (Exception e) {
            createmeowsplugins.LOGGER.error("[NumismaticHelper] Failed to set value", e);
        }
    }
}