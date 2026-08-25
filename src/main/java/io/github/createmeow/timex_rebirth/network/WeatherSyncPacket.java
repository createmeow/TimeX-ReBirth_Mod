package io.github.createmeow.timex_rebirth.network;

import io.github.createmeow.timex_rebirth.TimeX;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * 天气同步包：服务端 → 客户端。
 * forecast[0]=今天，[1..7]=未来 7 天（TimeXWeather ordinal）。
 */
public record WeatherSyncPacket(byte[] forecast) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<WeatherSyncPacket> TYPE =
            new CustomPacketPayload.Type<>(TimeX.rl("weather_sync"));

    public static final StreamCodec<ByteBuf, WeatherSyncPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeByte(packet.forecast().length);
                buf.writeBytes(packet.forecast());
            },
            buf -> {
                int len = buf.readByte() & 0xFF;
                byte[] forecast = new byte[len];
                buf.readBytes(forecast);
                return new WeatherSyncPacket(forecast);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
