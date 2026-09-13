package com.createmeow.underwaterplugin;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDrownEvent;

/**
 * 深度加权溺水伤害：
 * 玩家溺水时按其眼睛上方连续水柱深度增加伤害（越深越危险）。
 * 通过 NeoForge 官方 LivingDrownEvent 修改伤害量，不改动原版溺水流程。
 */
@EventBusSubscriber(modid = UnderwaterPlugin.MODID)
public class DrowningDamageHandler {

    @SubscribeEvent
    public static void onDrown(LivingDrownEvent event) {
        if (!Config.ENABLED.get()) return;
        // 伤害实际由服务端施加，客户端忽略
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.isSpectator()) return;

        float depth = WaterDepthUtil.getWaterColumnDepth(player, player.level());
        int threshold = Config.DEPTH_THRESHOLD.get();
        if (depth <= threshold) return;

        float extra = (float) ((depth - threshold) * Config.EXTRA_DAMAGE_PER_BLOCK.get());
        double maxExtra = Config.MAX_EXTRA_DAMAGE.get();
        if (maxExtra > 0) extra = Math.min(extra, (float) maxExtra);
        if (extra <= 0) return;

        // 在原版基础伤害（默认 2.0）上叠加深度加成，保留其他模组的修改
        event.setDamageAmount(event.getDamageAmount() + extra);
    }
}
