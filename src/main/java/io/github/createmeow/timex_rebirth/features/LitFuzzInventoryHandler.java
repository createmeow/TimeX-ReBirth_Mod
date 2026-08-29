package io.github.createmeow.timex_rebirth.features;

import io.github.createmeow.timex_rebirth.TimeX;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * 点燃的绒毛物品栏耐久消耗（与涂蜡纸板相同的"每秒扣 1"机制）：
 * <ul>
 *   <li>每 20 tick 扫描背包/快捷栏/副手的 lit_fuzz；</li>
 *   <li>耐久 &gt; 0 → damage +1；耐久 ≤ 0 → 替换为 ASH。</li>
 * </ul>
 */
@EventBusSubscriber(modid = TimeX.MODID)
public class LitFuzzInventoryHandler {

    @SubscribeEvent
    public static void onPlayerTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().getGameTime() % 20 != 0) return;

        var inventory = player.getInventory().items;
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack s = inventory.get(i);
            if (!s.is(FireToolRegistry.LIT_FUZZ.get())) continue;
            if (LitFuzzItem.remainingSeconds(s) <= 1) {
                player.getInventory().setItem(i, new ItemStack(FireToolRegistry.ASH.get()));
            } else {
                s.setDamageValue(s.getDamageValue() + 1);
            }
        }
    }
}
