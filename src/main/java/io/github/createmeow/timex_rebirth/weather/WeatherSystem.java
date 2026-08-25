package io.github.createmeow.timex_rebirth.weather;

import io.github.createmeow.timex_rebirth.TimeX;
import io.github.createmeow.timex_rebirth.network.WeatherSyncPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;

/**
 * 服务端天气调度系统：
 * - 每日（世界日界）按确定性随机决定当天天气，覆盖原版天气更替
 * - 通过 setWeatherParameters 强制原版天气与预测一致（防止原版随机切换）
 * - 向玩家广播当天 + 未来 7 天预测
 */
@EventBusSubscriber(modid = TimeX.MODID)
public class WeatherSystem {
    /** 记录每个维度已应用天气的天数索引，日界变化时重新应用 */
    private static final Map<Level, Long> appliedDay = new HashMap<>();
    private static final Map<Level, TimeXWeather> currentWeather = new HashMap<>();

    /** 巨大但不会溢出的剩余时长，防止原版 tickWeather 随机切换天气 */
    private static final int LONG_DURATION = Integer.MAX_VALUE - 1_000_000;

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        Level level = event.getLevel();
        if (level.isClientSide) return;
        if (!(level instanceof ServerLevel serverLevel)) return;

        long dayIndex = serverLevel.getDayTime() / 24000L;
        Long applied = appliedDay.get(level);
        if (applied == null || applied != dayIndex) {
            appliedDay.put(level, dayIndex);
            applyWeather(serverLevel, dayIndex);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Level level = player.level();
            if (level instanceof ServerLevel serverLevel) {
                long dayIndex = serverLevel.getDayTime() / 24000L;
                sendForecast(serverLevel, dayIndex);
            }
        }
    }

    /**
     * 应用当天天气到原版天气系统并广播预测。
     */
    private static void applyWeather(ServerLevel level, long dayIndex) {
        TimeXWeather weather = WeatherForecast.getWeather(level.getSeed(), dayIndex);
        currentWeather.put(level, weather);

        // 强制原版天气与我们一致，并设置巨大时长防止原版切换
        // setWeatherParameters(clearWeatherTime, rainTime, raining, thundering)
        // 注意：clearWeatherTime > 0 时原版强制晴天；rainTime/thunderTime 巨大时天气长期持续
        switch (weather) {
            case CLEAR -> level.setWeatherParameters(LONG_DURATION, 0, false, false);
            case RAIN, SNOW -> level.setWeatherParameters(0, LONG_DURATION, true, false);
            case THUNDER, BLIZZARD -> level.setWeatherParameters(0, LONG_DURATION, true, true);
        }
        TimeX.LOGGER.info("[Weather] Day {} weather set to {} (seed {})", dayIndex, weather, level.getSeed());

        broadcastBlizzardWarning(level, weather, dayIndex);
        sendForecast(level, dayIndex);
    }

    /**
     * 暴风雪预警提示（参照 FrostedHeart 天气预报预警）：
     * - 明天将迎来暴风雪（且今天不是暴风雪）→ 提前一天预警
     * - 今天进入暴风雪（昨天不是）→ 暴风雪来临预警
     */
    private static void broadcastBlizzardWarning(ServerLevel level, TimeXWeather weather, long dayIndex) {
        TimeXWeather tomorrow = WeatherForecast.getWeather(level.getSeed(), dayIndex + 1);
        if (tomorrow == TimeXWeather.BLIZZARD && weather != TimeXWeather.BLIZZARD) {
            broadcast(level, Component.translatable("msg.timex_rebirth.weather.blizzard_coming"));
        }
        if (weather == TimeXWeather.BLIZZARD) {
            TimeXWeather prev = WeatherForecast.getWeather(level.getSeed(), dayIndex - 1);
            if (prev != TimeXWeather.BLIZZARD) {
                broadcast(level, Component.translatable("msg.timex_rebirth.weather.blizzard_warning"));
            }
        }
    }

    private static void broadcast(ServerLevel level, Component message) {
        for (ServerPlayer player : level.players()) {
            player.sendSystemMessage(message);
        }
    }

    /**
     * 广播当天 + 未来 7 天预测给该维度所有玩家。
     */
    private static void sendForecast(ServerLevel level, long dayIndex) {
        byte[] forecast = WeatherForecast.getForecast(level.getSeed(), dayIndex);
        WeatherSyncPacket packet = new WeatherSyncPacket(forecast);
        for (ServerPlayer player : level.players()) {
            PacketDistributor.sendToPlayer(player, packet);
        }
    }

    /**
     * 查询某个维度当前的模组天气（服务端）。
     */
    public static TimeXWeather getWeather(ServerLevel level) {
        TimeXWeather weather = currentWeather.get(level);
        if (weather == null) {
            long dayIndex = level.getDayTime() / 24000L;
            weather = WeatherForecast.getWeather(level.getSeed(), dayIndex);
        }
        return weather;
    }

    public static boolean isColdWeather(ServerLevel level) {
        TimeXWeather w = getWeather(level);
        return w == TimeXWeather.SNOW || w == TimeXWeather.THUNDER || w == TimeXWeather.BLIZZARD;
    }
}
