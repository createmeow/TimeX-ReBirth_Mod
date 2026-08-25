package com.createmeow.cm_plugins;

import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * 处理 NumismaticOverhaul 钱币兼容：
 * 在遗体加入世界时扫描其所有物品，合并钱币为钱袋放入额外空间。
 */
public class CoinConsolidationHandler {

    private static final ResourceLocation CORPSE_ENTITY_ID = ResourceLocation.parse("corpse:corpse");
    private static boolean loggedInit = false;

    @SubscribeEvent
    public void onEntityJoin(EntityJoinLevelEvent event) {
        if (!loggedInit) {
            createmeowsplugins.LOGGER.info("[CoinConsolidation] 处理器已加载，监听 EntityJoinLevelEvent");
            loggedInit = true;
        }

        if (event.getLevel().isClientSide()) return;

        Entity entity = event.getEntity();
        ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (!CORPSE_ENTITY_ID.equals(typeId)) return;

        createmeowsplugins.LOGGER.info("[CoinConsolidation] 检测到遗体加入世界: {} @ {}",
                entity.getUUID(), entity.blockPosition());

        try {
            consolidateCorpse(entity);
        } catch (Exception e) {
            createmeowsplugins.LOGGER.error("[CoinConsolidation] 处理遗体时出错", e);
        }
    }

    private void consolidateCorpse(Entity corpseEntity) throws Exception {
        // 检查 NumismaticOverhaul 是否加载
        Class<?> coinItemClass;
        Class<?> currencyEnumClass;
        Class<?> moneyBagItemClass;
        try {
            coinItemClass = Class.forName("tallestred.numismaticoverhaul.item.CoinItem");
            currencyEnumClass = Class.forName("tallestred.numismaticoverhaul.currency.Currency");
            moneyBagItemClass = Class.forName("tallestred.numismaticoverhaul.item.MoneyBagItem");
        } catch (ClassNotFoundException e) {
            createmeowsplugins.LOGGER.info("[CoinConsolidation] NumismaticOverhaul 未安装，跳过");
            return;
        }

        // 获取 Death 对象
        var getDeathMethod = corpseEntity.getClass().getMethod("getDeath");
        Object death = getDeathMethod.invoke(corpseEntity);
        if (death == null) {
            createmeowsplugins.LOGGER.warn("[CoinConsolidation] Death 对象为空");
            return;
        }

        createmeowsplugins.LOGGER.info("[CoinConsolidation] Death 对象类型: {}", death.getClass().getName());

        long totalRawValue = 0;
        int coinCount = 0;

        // 扫描 4 个空间
        String[] inventoryGetters = {"getMainInventory", "getArmorInventory", "getOffHandInventory", "getAdditionalItems"};
        for (String getter : inventoryGetters) {
            try {
                var method = death.getClass().getMethod(getter);
                Object raw = method.invoke(death);
                if (raw == null) continue;
                @SuppressWarnings("unchecked")
                NonNullList<ItemStack> items = (NonNullList<ItemStack>) raw;
                if (items.isEmpty()) continue;

                createmeowsplugins.LOGGER.info("[CoinConsolidation]  {}: {} 个物品", getter, items.size());

                var toRemove = new java.util.ArrayList<ItemStack>();
                for (ItemStack stack : items) {
                    if (stack.isEmpty()) continue;
                    if (coinItemClass.isInstance(stack.getItem())) {
                        Object currency = coinItemClass.getField("currency").get(stack.getItem());
                        long count = stack.getCount();
                        long rawValue = (long) currencyEnumClass.getMethod("getRawValue", long.class).invoke(currency, count);
                        totalRawValue += rawValue;
                        coinCount += count;
                        toRemove.add(stack);
                    }
                }
                if (!toRemove.isEmpty()) {
                    items.removeAll(toRemove);
                    createmeowsplugins.LOGGER.info("[CoinConsolidation]   → 移除了 {} 个钱币", toRemove.size());
                }
            } catch (NoSuchMethodException e) {
                createmeowsplugins.LOGGER.warn("[CoinConsolidation]  方法 {} 不存在", getter);
            }
        }

        if (totalRawValue <= 0) {
            createmeowsplugins.LOGGER.info("[CoinConsolidation] 未找到钱币，跳过");
            return;
        }

        // 创建钱袋放入额外空间
        ItemStack moneyBag = (ItemStack) moneyBagItemClass.getMethod("fromRawValue", long.class)
                .invoke(null, totalRawValue);

        var getAdditionalItemsMethod = death.getClass().getMethod("getAdditionalItems");
        @SuppressWarnings("unchecked")
        NonNullList<ItemStack> additionalItems = (NonNullList<ItemStack>) getAdditionalItemsMethod.invoke(death);
        additionalItems.add(moneyBag);

        createmeowsplugins.LOGGER.info("[CoinConsolidation] ✅ 合并完成: {} 枚钱币 ({}) → 1 个钱袋（额外空间）",
                coinCount, totalRawValue);
    }
}