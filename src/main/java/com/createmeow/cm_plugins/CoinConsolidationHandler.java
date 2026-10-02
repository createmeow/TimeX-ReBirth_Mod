package com.createmeow.cm_plugins;

import com.createmeow.currency_plugin.currency.Currency;
import com.createmeow.currency_plugin.item.CoinItem;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * 处理 currency_plugin 钱币兼容：
 * 在遗体加入世界时扫描其所有物品，合并钱币为合法堆叠放入额外空间。
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

    /**
     * 遗体钱币合并：<b>按货币种类分别合并</b>（腐空朽只与腐空朽合并，
     * 归霜升只与归霜升合并，不进行跨种类换算），放入额外空间。
     */
    private void consolidateCorpse(Entity corpseEntity) throws Exception {
        var getDeathMethod = corpseEntity.getClass().getMethod("getDeath");
        Object death = getDeathMethod.invoke(corpseEntity);
        if (death == null) {
            createmeowsplugins.LOGGER.warn("[CoinConsolidation] Death 对象为空");
            return;
        }

        var totals = new java.util.EnumMap<Currency, Long>(Currency.class);
        int coinCount = 0;

        // 扫描 4 个空间
        String[] inventoryGetters = {"getMainInventory", "getArmorInventory", "getOffHandInventory", "getAdditionalItems"};
        for (String getter : inventoryGetters) {
            try {
                var method = death.getClass().getMethod(getter);
                Object raw = method.invoke(death);
                if (raw == null) continue;
                NonNullList<ItemStack> items = (NonNullList<ItemStack>) raw;
                if (items.isEmpty()) continue;

                var toRemove = new ArrayList<ItemStack>();
                for (ItemStack stack : items) {
                    if (stack.isEmpty()) continue;
                    if (stack.getItem() instanceof CoinItem coinItem) {
                        totals.merge(coinItem.currency, (long) stack.getCount(), Long::sum);
                        coinCount += stack.getCount();
                        toRemove.add(stack);
                    }
                }
                if (!toRemove.isEmpty()) {
                    items.removeAll(toRemove);
                }
            } catch (NoSuchMethodException e) {
                // getter 不存在，跳过
            }
        }

        if (totals.isEmpty()) return;

        // 按种类重新生成堆叠（不换算面值），每堆最多 99
        List<ItemStack> stacks = new ArrayList<>();
        for (var entry : totals.entrySet()) {
            long count = entry.getValue();
            while (count > 0) {
                int size = (int) Math.min(count, 99);
                stacks.add(new ItemStack(entry.getKey().asItem(), size));
                count -= size;
            }
        }

        var getAdditionalItemsMethod = death.getClass().getMethod("getAdditionalItems");
        @SuppressWarnings("unchecked")
        NonNullList<ItemStack> additionalItems = (NonNullList<ItemStack>) getAdditionalItemsMethod.invoke(death);
        for (ItemStack stack : stacks) {
            additionalItems.add(stack);
        }

        createmeowsplugins.LOGGER.info("[CoinConsolidation] 合并完成: {} 枚钱币 → {} 堆（按种类合并，额外空间）",
                coinCount, stacks.size());
    }
}
