package io.github.createmeow.timex_rebirth.compat;

import com.hexagram2021.fiahi.common.item.capability.IFrozenRottenFood;
import com.hexagram2021.fiahi.register.FIAHICapabilities;
import io.github.createmeow.timex_rebirth.TimeX;
import io.github.createmeow.timex_rebirth.TimeXConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Freeze-It-And-Heat-It（fiahi）联动：吃冷冻/腐败食物时追加理智惩罚。
 * fiahi 负责冻结/腐败等级、战栗/反胃等原效果（读取其物品能力判断等级），
 * 本类在其基础上按等级扣除 RealityValue 理智，并给出提示。
 */
@EventBusSubscriber(modid = TimeX.MODID)
public class FiahiFoodHandler {

    @SubscribeEvent
    public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        IFrozenRottenFood data = event.getItem().getCapability(FIAHICapabilities.FOOD_CAPABILITY);
        if (data == null) return;

        int frozenLevel = data.getFrozenLevel();
        if (frozenLevel > 0) {
            int loss = TimeXConfig.FIAHI_FROZEN_SANITY_LOSS.get() * frozenLevel;
            RealityValueCompat.addSanity(player, -loss);
            player.displayClientMessage(
                    Component.translatable("msg.timex_rebirth.food.frozen_sanity", loss), true);
        }

        int rottenLevel = data.getRottenLevel();
        if (rottenLevel > 0) {
            int loss = TimeXConfig.FIAHI_ROTTEN_SANITY_LOSS.get() * rottenLevel;
            RealityValueCompat.addSanity(player, -loss);
            player.displayClientMessage(
                    Component.translatable("msg.timex_rebirth.food.rotten_sanity", loss), true);
            // 与 RealityValue 吃腐肉一致：随机损失 1~3 点健康值（nextInt(1,4) 返回 1/2/3）
            int healthLoss = ThreadLocalRandom.current().nextInt(1, 4);
            RealityValueCompat.addHealth(player, -healthLoss);
        }
    }
}
