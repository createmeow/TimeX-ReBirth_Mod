package io.github.createmeow.timex_rebirth.client;

import io.github.createmeow.timex_rebirth.network.WeatherSyncPacket;
import io.github.createmeow.timex_rebirth.weather.TimeXWeather;

/**
 * 客户端天气状态缓存（由服务端 WeatherSyncPacket 更新）。
 */
public class ClientWeatherState {
    private static TimeXWeather current = TimeXWeather.CLEAR;
    private static TimeXWeather[] forecast = new TimeXWeather[8];

    static {
        for (int i = 0; i < forecast.length; i++) forecast[i] = TimeXWeather.CLEAR;
    }

    public static void handlePacket(WeatherSyncPacket packet) {
        byte[] data = packet.forecast();
        if (data.length > 0) {
            TimeXWeather today = safe(data[0]);
            current = today;
            for (int i = 0; i < forecast.length; i++) {
                forecast[i] = i < data.length ? safe(data[i]) : TimeXWeather.CLEAR;
            }
        }
    }

    private static TimeXWeather safe(byte ordinal) {
        TimeXWeather[] values = TimeXWeather.values();
        int idx = ordinal & 0xFF;
        if (idx < 0 || idx >= values.length) return TimeXWeather.CLEAR;
        return values[idx];
    }

    public static TimeXWeather getCurrent() { return current; }

    /** forecast[i]：i=0 为今天，1..7 为未来。 */
    public static TimeXWeather getForecast(int i) {
        if (i < 0 || i >= forecast.length) return TimeXWeather.CLEAR;
        return forecast[i];
    }

    public static boolean isBlizzard() { return current == TimeXWeather.BLIZZARD; }
    public static boolean isSnowing() { return current == TimeXWeather.SNOW || current == TimeXWeather.BLIZZARD; }
    public static boolean isThundering() { return current.isThundering(); }
}
