package com.createmeow.cm_plugins;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 自动合并玩家背包中的货币（钱币 + 钱袋）为钱袋：
 * 定期（默认每 100 tick = 5 秒）扫描玩家背包，当货币（CoinItem 钱币 / MoneyBagItem 钱袋）
 * 分散在多个槽位时，将其合并为一个等价价值的钱袋，减少背包占用。
 * <p>通过反射访问 NumismaticOverhaul，未安装该模组时自动停用，避免编译期依赖。</p>
 * <p>自动合并按玩家独立开关（默认开启），可由 {@code /autocoin} 或「实用功能」面板切换。</p>
 */
public class PlayerCoinConsolidationHandler {

    private static final int CHECK_INTERVAL_TICKS = 100;

    /** 关闭自动合并的玩家 UUID（默认所有玩家开启自动合并）。 */
    private static final Set<UUID> AUTO_CONSOLIDATE_DISABLED = new HashSet<>();

    private static boolean checked = false;
    private static boolean available = false;
    private static Class<?> coinItemClass;
    private static Class<?> moneyBagItemClass;
    private static Field coinCurrencyField;
    private static Method currencyGetRawValue;
    private static Method moneyBagFromRawValue;
    private static Method moneyBagGetValue;

    public static boolean isAutoConsolidate(UUID uuid) {
        return !AUTO_CONSOLIDATE_DISABLED.contains(uuid);
    }

    /** 切换指定玩家的自动合并开关，返回切换后的状态。 */
    public static boolean toggleAutoConsolidate(UUID uuid) {
        if (!AUTO_CONSOLIDATE_DISABLED.remove(uuid)) {
            AUTO_CONSOLIDATE_DISABLED.add(uuid);
        }
        return isAutoConsolidate(uuid);
    }

    /** 懒加载反射句柄（NumismaticOverhaul 为可选依赖）。 */
    private static void ensureLoaded() {
        if (checked) return;
        checked = true;
        try {
            coinItemClass = Class.forName("tallestred.numismaticoverhaul.item.CoinItem");
            Class<?> currencyEnumClass = Class.forName("tallestred.numismaticoverhaul.currency.Currency");
            moneyBagItemClass = Class.forName("tallestred.numismaticoverhaul.item.MoneyBagItem");

            coinCurrencyField = coinItemClass.getField("currency");
            currencyGetRawValue = currencyEnumClass.getMethod("getRawValue", long.class);
            moneyBagFromRawValue = moneyBagItemClass.getMethod("fromRawValue", long.class);
            moneyBagGetValue = moneyBagItemClass.getMethod("getValue", ItemStack.class);
            available = true;
        } catch (Exception e) {
            available = false;
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!isAutoConsolidate(player.getUUID())) return; // 该玩家关闭了自动合并
        ensureLoaded();
        if (!available) return;
        if (player.tickCount % CHECK_INTERVAL_TICKS != 0) return;

        try {
            consolidatePlayerCoins(player);
        } catch (Exception e) {
            createmeowsplugins.LOGGER.error("[PlayerCoinConsolidation] 合并货币时出错", e);
        }
    }

    /** 扫描玩家背包中的货币（钱币 + 钱袋），若分散在多个槽位则合并为一个钱袋。 */
    private static void consolidatePlayerCoins(ServerPlayer player) throws Exception {
        var inventory = player.getInventory();

        long totalValue = 0;
        List<Integer> currencySlots = new ArrayList<>();

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) continue;

            long value;
            if (coinItemClass.isInstance(stack.getItem())) {
                // 钱币：面值 × 数量
                Object currency = coinCurrencyField.get(stack.getItem());
                value = (long) currencyGetRawValue.invoke(currency, (long) stack.getCount());
            } else if (moneyBagItemClass.isInstance(stack.getItem())) {
                // 钱袋：读取其内部总价值
                value = (long) moneyBagGetValue.invoke(stack.getItem(), stack);
            } else {
                continue;
            }
            totalValue += value;
            currencySlots.add(i);
        }

        // 只有分散在多个槽位时才需要合并；单堆保持原样，避免无意义地改变物品形态
        if (currencySlots.size() < 2 || totalValue <= 0) return;

        // 移除所有货币（钱币 + 钱袋）
        for (int slot : currencySlots) {
            inventory.setItem(slot, ItemStack.EMPTY);
        }

        // 生成总价值的钱袋并放回背包（放不下则掉落）
        ItemStack moneyBag = (ItemStack) moneyBagFromRawValue.invoke(null, totalValue);
        if (!inventory.add(moneyBag)) {
            player.drop(moneyBag, false);
        }

        createmeowsplugins.LOGGER.info("[PlayerCoinConsolidation] 已将 {} 组货币合并为 1 个钱袋（价值 {}）: {}",
                currencySlots.size(), totalValue, player.getName().getString());
    }
}