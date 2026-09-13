package com.createmeow.underwaterplugin;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 深度温度削减入口：玩家下潜越深，Cold Sweat 的环境温度削减越快
 * （每格额外降低 depthTempPerBlock °C，可配置；换算到 Cold Sweat 内部 MC 标度
 * 由 ColdSweatIntegration 完成）。
 * 本类维护"玩家 → 眼睛上方水柱深度"的缓存（每 5 tick 刷新），供 ColdSweatIntegration
 * 的 Calculate.Post 事件读取；本类不直接引用 Cold Sweat API，
 * Cold Sweat 缺席时缓存照常维护但不会被消费，安全无副作用。
 */
@EventBusSubscriber(modid = UnderwaterPlugin.MODID)
public class DepthTemperatureHandler {

    private static final Map<UUID, Float> DEPTH_CACHE = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        if (player.isSpectator()) return;
        if (!ColdSweatIntegration.isAvailable()) return;

        if (player.tickCount % 5 != 0) return;

        float depth = 0.0f;
        if (Config.DEPTH_TEMP_ENABLED.get() && player.isInWater()) {
            depth = WaterDepthUtil.getWaterColumnDepth(player, player.level());
        }
        if (depth > 0) {
            DEPTH_CACHE.put(player.getUUID(), depth);
        } else {
            DEPTH_CACHE.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        DEPTH_CACHE.remove(event.getEntity().getUUID());
    }

    /** @return 该玩家当前的水柱深度；不在水中/无数据时为 0 */
    static float getCachedDepth(Player player) {
        return DEPTH_CACHE.getOrDefault(player.getUUID(), 0.0f);
    }
}
