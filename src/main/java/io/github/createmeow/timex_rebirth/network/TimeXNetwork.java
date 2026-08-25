package io.github.createmeow.timex_rebirth.network;

import io.github.createmeow.timex_rebirth.client.ClientWeatherState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 本模组网络通道注册。
 */
public class TimeXNetwork {

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("timex_rebirth");
        registrar.commonToClient(
                WeatherSyncPacket.TYPE,
                WeatherSyncPacket.STREAM_CODEC,
                TimeXNetwork::handleWeatherSync
        );
    }

    private static void handleWeatherSync(WeatherSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ClientWeatherState.handlePacket(packet));
    }
}
