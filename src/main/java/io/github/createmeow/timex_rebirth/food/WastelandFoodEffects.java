package io.github.createmeow.timex_rebirth.food;

import io.github.createmeow.timex_rebirth.TimeX;
import io.github.createmeow.timex_rebirth.compat.RealityValueCompat;
import io.github.createmeow.timex_rebirth.wasteland.WastelandRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 废土食物特殊效果（食用完成后触发）：
 * - 罐头食品：恢复理智（耐储存的安心补给）
 * - 蒸馏水：饮用干净水源，小幅恢复理智
 * - 草药茶：舒缓精神，恢复理智
 * - 碗装腐烂食物：随机损失 1~3 点健康（与 RealityValue 吃腐肉机制一致）
 */
@EventBusSubscriber(modid = TimeX.MODID)
public class WastelandFoodEffects {

    @SubscribeEvent
    public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        Item item = event.getItem().getItem();
        if (item == WastelandFoodRegistry.CANNED_FOOD.get()) {
            RealityValueCompat.addSanity(player, 5);
        } else if (item == WastelandFoodRegistry.INSTANT_NOODLES.get()) {
            RealityValueCompat.addSanity(player, 4);
        } else if (item == WastelandFoodRegistry.HERBAL_TEA.get()) {
            RealityValueCompat.addSanity(player, 5);
        } else if (item == WastelandRegistry.ROTTEN_MEAL_BOWL.get()
                || item == WastelandRegistry.ROTTEN_MEAT_BOWL.get()) {
            // 与 RealityValue 吃腐肉一致：随机损失 1~3 点健康值（nextInt(1,4) 返回 1/2/3）
            int loss = ThreadLocalRandom.current().nextInt(1, 4);
            RealityValueCompat.addHealth(player, -loss);
        }
    }
}
