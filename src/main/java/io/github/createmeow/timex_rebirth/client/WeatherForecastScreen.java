package io.github.createmeow.timex_rebirth.client;

import io.github.createmeow.timex_rebirth.TimeX;
import io.github.createmeow.timex_rebirth.calendar.CalendarSystem;
import io.github.createmeow.timex_rebirth.weather.TimeXWeather;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 天气预报界面：显示今天 + 未来 7 天的天气，并显示当前换算日期（年/月/日）。
 * 日期由原版世界天数（dayTime / 24000）经 CalendarSystem 换算得到。
 */
public class WeatherForecastScreen extends Screen {
    private static final int DAYS = 8;

    public WeatherForecastScreen() {
        super(Component.translatable("gui." + TimeX.MODID + ".weather_forecast.title"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int y = this.height / 2 + 60;
        this.addRenderableWidget(Button.builder(
                        Component.translatable("gui." + TimeX.MODID + ".weather_forecast.close"),
                        btn -> this.onClose())
                .bounds(centerX - 50, y, 100, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int centerX = this.width / 2;
        int startY = this.height / 2 - 50;

        graphics.drawCenteredString(this.font,
                Component.translatable("gui." + TimeX.MODID + ".weather_forecast.title"),
                centerX, startY - 30, 0xFFFFFF);

        // 当前日期（由原版 dayTime 换算）
        String dateText = getCurrentDateText();
        graphics.drawCenteredString(this.font, dateText, centerX, startY - 12 + 60, 0xFFFF55);

        for (int i = 0; i < DAYS; i++) {
            int x = centerX - DAYS * 22 + i * 44 + 22;
            int y = startY;
            TimeXWeather weather = ClientWeatherState.getForecast(i);

            // 天标签
            String label = i == 0
                    ? Component.translatable("gui." + TimeX.MODID + ".weather_forecast.today").getString()
                    : (i + "d");
            graphics.drawCenteredString(this.font, label, x, y - 12, 0xAAAAAA);

            // 天气图标（物品渲染）
            graphics.renderItem(weather.getIcon(), x - 8, y);

            // 天气名（暴风雪用红色预警强调）
            String name = Component.translatable(weather.getTranslationKey()).getString();
            if (weather == TimeXWeather.BLIZZARD) {
                name = "⚠" + name;
                graphics.drawCenteredString(this.font, name, x, y + 20, 0xFF5555);
            } else {
                graphics.drawCenteredString(this.font, name, x, y + 20, 0xFFFFFF);
            }
        }
    }

    /**
     * 从客户端原版世界天数换算当前日期（X 年 X 月 X 日 / 第 N 天）。
     */
    private String getCurrentDateText() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return "";
        long dayIndex = mc.level.getDayTime() / 24000L;
        CalendarSystem.DateInfo date = CalendarSystem.fromDayIndex(dayIndex);
        return "§6" + date.year() + " §7年 §6" + date.month() + " §7月 §6"
                + date.dayOfMonth() + " §7日 §8(第 " + dayIndex + " 天)";
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
