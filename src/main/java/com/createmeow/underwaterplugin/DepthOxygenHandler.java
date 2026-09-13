package com.createmeow.underwaterplugin;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 过深耗氧（参考 BetterDiving 的 oxygenEfficiency 机制）：
 * 眼睛上方水柱深度超过阈值（默认 20 格）后，氧气消耗速率飙升：
 *   额外消耗 = 1 + (深度 - 阈值) / blocksPerUnit（默认 8 格 +1，即每 8 格耗氧倍率 +1 倍）
 * 供氧来源二选一：
 * - 无背罐供氧（无装备/装备无气）：作用于原版氧气（每 tick 额外扣 air，更快进入原版溺水伤害周期）
 * - 背罐供氧中：作用于背罐压缩空气（每秒额外消耗）。三种背罐统一处理——
 *   CreateIntegration.hasOxygenSupply 判定铝背罐（胸甲槽，BacktankBreatheHandler 供氧中）
 *   或 Create 潜水头盔 + 任意有气背罐；消耗走 BacktankUtil.getAllWithAir
 *   （按存量升序，优先耗尽最空的，与 Create 官方调度一致，保留低气量警告）
 */
@EventBusSubscriber(modid = UnderwaterPlugin.MODID)
public class DepthOxygenHandler {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        if (player.isSpectator() || player.isCreative()) return;
        if (!Config.DEPTH_OXYGEN_ENABLED.get()) return;

        float depth = WaterDepthUtil.getWaterColumnDepth(player, player.level());
        int threshold = Config.DEPTH_OXYGEN_THRESHOLD.get();
        if (depth <= threshold) return;

        // BetterDiving 公式的惩罚部分（基础消耗由原版/Create 自行处理）
        int extra = 1 + (int) ((depth - threshold) / Config.DEPTH_OXYGEN_BLOCKS_PER_UNIT.get());

        if (CreateIntegration.isAvailable() && CreateIntegration.hasOxygenSupply(player)) {
            // 背罐供氧中：等比作用于背罐空气（原版 20 单位/秒 ↔ 背罐 1 单位/秒）
            if (player.level().getGameTime() % 20 == 0) {
                CreateIntegration.consumeExtraAir(player, extra);
            }
        } else {
            // 原版氧气：每 tick 额外扣除；air 降到负数会更快触发原版溺水伤害
            player.setAirSupply(player.getAirSupply() - extra);
        }
    }
}
