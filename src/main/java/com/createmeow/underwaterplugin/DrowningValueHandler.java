package com.createmeow.underwaterplugin;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.common.NeoForgeMod;

/**
 * 溺水值系统：
 * - 氧气值 < 1 时每秒 +1；氧气值 >= 上限（默认 300）时每秒 -1；范围 0 ~ 18。
 * - 玩家在水中时，溺水值 0~5 保持原版游速；5 之后线性减速，18 点时完全无法移动。
 * 实现：修改 NeoForge 的 SWIM_SPEED 属性（1.21.1 中水中移动速度 = 0.02 * SWIM_SPEED，
 * 归零即完全无法移动），比 Mixin travel 更安全。
 */
@EventBusSubscriber(modid = UnderwaterPlugin.MODID)
public class DrowningValueHandler {

    private static final ResourceLocation SLOWDOWN_ID =
            ResourceLocation.fromNamespaceAndPath(UnderwaterPlugin.MODID, "drowning_value_slowdown");

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        if (player.isSpectator()) return;

        int max = Config.DROWNING_VALUE_MAX.get();
        int start = Config.DROWNING_PENALTY_START.get();

        // ---- 每 1 秒更新溺水值 ----
        if (player.tickCount % 20 == 0) {
            int drowning = player.getData(UnderwaterAttachments.DROWNING_VALUE.get());
            int air = player.getAirSupply();
            if (air < 1) {
                // 缺氧：每秒 +1（原版溺水伤害期间 air 为负数同样计入）
                drowning = Math.min(drowning + 1, max);
            } else if (air >= player.getMaxAirSupply()) {
                // 氧气充足：每秒 -1
                drowning = Math.max(drowning - 1, 0);
            }
            player.setData(UnderwaterAttachments.DROWNING_VALUE.get(), drowning);
        }

        // ---- 水中减速（每 tick 维护瞬时属性修正） ----
        AttributeInstance swimSpeed = player.getAttribute(NeoForgeMod.SWIM_SPEED);
        if (swimSpeed == null) return;

        int drowning = player.getData(UnderwaterAttachments.DROWNING_VALUE.get());
        boolean applyPenalty = player.isInWater() && drowning > start;
        AttributeModifier existing = swimSpeed.getModifier(SLOWDOWN_ID);

        if (!applyPenalty) {
            if (existing != null) swimSpeed.removeModifier(SLOWDOWN_ID);
            return;
        }

        // 5 点 = 不减速，max 点 = -100%（完全无法移动）
        double factor = (double) (drowning - start) / Math.max(1, max - start);
        factor = Math.min(1.0, Math.max(0.0, factor));

        if (existing != null && existing.amount() == -factor) return;
        swimSpeed.removeModifier(SLOWDOWN_ID);
        swimSpeed.addTransientModifier(new AttributeModifier(SLOWDOWN_ID, -factor,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }
}
