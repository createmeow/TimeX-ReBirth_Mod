package io.github.createmeow.timex_rebirth.compat;

import io.github.createmeow.timex_rebirth.TimeX;
import io.github.createmeow.timex_rebirth.TimeXConfig;
import io.github.createmeow.timex_rebirth.weather.WeatherSystem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Cold Sweat 联动：寒冷天气（雪/雷/暴风雪）下使玩家寒冷更快。
 * 通过反射向玩家 WORLD 温度累加负值。
 */
@EventBusSubscriber(modid = TimeX.MODID)
public class ColdSweatHandler {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;

        double boost = TimeXConfig.COLD_SWEAT_BOOST.get();
        if (boost <= 0) return;

        // 仅在寒冷天气且玩家露天时加速寒冷
        if (WeatherSystem.isColdWeather(level) && level.canSeeSky(player.blockPosition())) {
            ColdSweatCompat.boostCold(player, boost);
        }
    }
}
