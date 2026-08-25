package io.github.createmeow.timex_rebirth.weather;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 本模组的每日天气类型。
 * 由模组随机决定，覆盖原版天气更替。
 * CLEAR=晴 RAIN=雨 SNOW=雪 THUNDER=雷雨 BLIZZARD=暴风雪（下雪+打雷+特效）
 */
public enum TimeXWeather {
    CLEAR, RAIN, SNOW, THUNDER, BLIZZARD;

    public ItemStack getIcon() {
        return switch (this) {
            case CLEAR -> new ItemStack(Items.SUNFLOWER);
            case RAIN -> new ItemStack(Items.WATER_BUCKET);
            case SNOW -> new ItemStack(Items.SNOWBALL);
            case THUNDER -> new ItemStack(Items.LIGHTNING_ROD);
            case BLIZZARD -> new ItemStack(Items.SNOW_BLOCK);
        };
    }

    public String getTranslationKey() {
        return "weather." + "timex_rebirth" + "." + name().toLowerCase();
    }

    public boolean isPrecipitating() {
        return this != CLEAR;
    }

    public boolean isThundering() {
        return this == THUNDER || this == BLIZZARD;
    }
}
