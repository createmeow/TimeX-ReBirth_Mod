package io.github.createmeow.timex_rebirth.weather;

import io.github.createmeow.timex_rebirth.TimeXConfig;

/**
 * 确定性温度曲线天气预测：
 * 以固定周期（默认 30 天）按"温度段"演进，段长由世界种子确定性扰动：
 *   晴段（温暖）→ 雪段（寒冷）→ 暴风雪段（极寒）→ 雪段（回暖）→ 晴段
 * - 暴风雪段被两侧雪段夹住，结束后自然转换为普通降雪；
 * - 暴风雪每周期至多一次，与前次间隔接近整个周期（远超 5 天），杜绝
 *   "一天暴风雪一天晴又一天雪"的随机跳跃；
 * - 不再生成雨/雷雨（在积雪生物群系中它们本就表现为普通降雪，预报无意义）。
 */
public class WeatherForecast {

    /**
     * 获取指定天数（dayIndex = dayTime / 24000）的天气。
     */
    public static TimeXWeather getWeather(long worldSeed, long dayIndex) {
        int cycleLen = Math.max(10, TimeXConfig.WEATHER_CYCLE_LENGTH.get());
        long cycle = Math.floorDiv(dayIndex, cycleLen);
        int dayInCycle = (int) Math.floorMod(dayIndex, cycleLen);

        // 段长由种子 + 周期序号确定性派生（各段范围固定，保证暴风雪间隔与衔接）
        long h = mix64(worldSeed ^ (cycle * 0x9E3779B97F4A7C15L));
        int clear1 = 3 + (int) (h & 3);           // 晴 3~6 天
        int snow1 = 3 + (int) ((h >> 2) & 3);     // 雪 3~6 天
        int blizzard = 2 + (int) ((h >> 4) & 3);  // 暴风雪 2~5 天
        int snow2 = 3 + (int) ((h >> 6) & 3);     // 回暖雪 3~6 天

        if (dayInCycle < clear1) return TimeXWeather.CLEAR;
        dayInCycle -= clear1;
        if (dayInCycle < snow1) return TimeXWeather.SNOW;
        dayInCycle -= snow1;
        if (dayInCycle < blizzard) return TimeXWeather.BLIZZARD;
        dayInCycle -= blizzard;
        if (dayInCycle < snow2) return TimeXWeather.SNOW;
        return TimeXWeather.CLEAR;
    }

    /**
     * 生成 8 字节预测序列：[0]=今天，[1..7]=未来 7 天。
     */
    public static byte[] getForecast(long worldSeed, long todayIndex) {
        byte[] forecast = new byte[8];
        for (int i = 0; i < 8; i++) {
            forecast[i] = (byte) getWeather(worldSeed, todayIndex + i).ordinal();
        }
        return forecast;
    }

    private static long mix64(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }
}
