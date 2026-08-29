package io.github.createmeow.timex_rebirth.features;

import com.momosoftworks.coldsweat.api.event.core.registry.TempModifierRegisterEvent;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.api.util.placement.Matcher;
import io.github.createmeow.timex_rebirth.TimeX;
import io.github.createmeow.timex_rebirth.compat.LitCardboardTempModifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * 点燃的涂蜡纸板的温度与耐久联动（参考 Cold Sweat 水袋的快捷栏增温机制）：
 * <ul>
 *   <li><b>快捷栏/手持增温</b>：快捷栏或主副手中的点燃纸板，每 20 tick 向玩家身体温度（CORE）
 *       加一次热量偏置（每张 +0.03，可叠加）；</li>
 *   <li><b>背包耐久消耗</b>：背包任意位置的点燃纸板每秒扣 1 点耐久，归零则烧尽为灰烬（替换该槽位）。</li>
 * </ul>
 */
@EventBusSubscriber(modid = TimeX.MODID)
public class CardboardTemperatureHandler {

    /** 每张点燃纸板提供的热量偏置（Cold Sweat 温度单位）。 */
    private static final double HEAT_PER_CARD = 0.03;

    /** 注册 LitCardboardTempModifier 以便网络同步（与 Cold Sweat 修饰器一致）。 */
    @SubscribeEvent
    public static void onRegisterTempModifiers(TempModifierRegisterEvent event) {
        event.register(TimeX.rl("lit_cardboard"), LitCardboardTempModifier::new);
    }

    /** 每 20 tick 处理增温与背包耐久。 */
    @SubscribeEvent
    public static void onPlayerTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().getGameTime() % 20 != 0) return;

        // ── 快捷栏/手持增温（快捷栏 0~8 已含主手，副手单独统计）──
        double heat = 0.0;
        for (int slot = 0; slot < 9; slot++) {
            if (isLitCardboard(player.getInventory().getItem(slot))) heat += HEAT_PER_CARD;
        }
        if (isLitCardboard(player.getOffhandItem())) heat += HEAT_PER_CARD;

        // 为玩家核心温度添加/替换加温修饰器（Cold Sweat 签名：entity, modifier, trait, matcher）
        Temperature.replaceOrAddModifier(player,
                new LitCardboardTempModifier(heat).expires(25).tickRate(20),
                Temperature.Trait.CORE, Matcher.SAME_CLASS);

        // ── 背包耐久消耗（含主背包与副手；逐张每秒扣 1，归零变灰烬）──
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack s = inventory.getItem(i);
            if (!isLitCardboard(s)) continue;
            int remaining = WaxedCardboardItem.remainingSeconds(s);
            if (remaining <= 1) {
                inventory.setItem(i, new ItemStack(FireToolRegistry.ASH.get()));
            } else {
                s.setDamageValue(s.getDamageValue() + 1);
            }
        }
    }

    private static boolean isLitCardboard(ItemStack stack) {
        return stack.is(FireToolRegistry.LIT_WAXED_CARDBOARD.get())
                && WaxedCardboardItem.remainingSeconds(stack) > 0;
    }
}
