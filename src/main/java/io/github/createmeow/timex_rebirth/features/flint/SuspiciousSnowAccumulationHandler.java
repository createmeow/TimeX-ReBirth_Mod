package io.github.createmeow.timex_rebirth.features.flint;

import io.github.createmeow.timex_rebirth.TimeX;
import io.github.createmeow.timex_rebirth.weather.TimeXWeather;
import io.github.createmeow.timex_rebirth.weather.WeatherSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;

/**
 * 暴风雪下可疑积雪缓慢积累层数。
 * 每隔 ACCUMULATE_INTERVAL tick，在暴风雪期间为玩家附近暴露于天空下的可疑积雪 +1 层（最多 8）。
 */
@EventBusSubscriber(modid = TimeX.MODID)
public class SuspiciousSnowAccumulationHandler {

    private static final int ACCUMULATE_INTERVAL = 400; // 20秒

    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ServerLevel level = event.getServer().overworld();
        if (level == null) return;
        if (WeatherSystem.getWeather(level) != TimeXWeather.BLIZZARD) return;

        tickCounter++;
        if (tickCounter < ACCUMULATE_INTERVAL) return;
        tickCounter = 0;

        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (player.level() != level) continue;
            accumulateNearPlayer(level, player);
        }
    }

    private static void accumulateNearPlayer(ServerLevel level, ServerPlayer player) {
        BlockPos origin = player.blockPosition();
        int radius = 32;
        // 随机采样若干位置
        for (int i = 0; i < 8; i++) {
            int dx = level.getRandom().nextInt(radius * 2) - radius;
            int dz = level.getRandom().nextInt(radius * 2) - radius;
            BlockPos pos = origin.offset(dx, 0, dz);
            // 从顶部向下查找可疑积雪
            int topY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, pos.getX(), pos.getZ());
            for (int y = topY; y >= pos.getY() - 16; y--) {
                BlockPos checkPos = new BlockPos(pos.getX(), y, pos.getZ());
                BlockState state = level.getBlockState(checkPos);
                if (state.getBlock() instanceof SuspiciousSnowBlock) {
                    if (!level.canSeeSky(checkPos)) continue;
                    int layers = state.getValue(BlockStateProperties.LAYERS);
                    if (layers < 8) {
                        level.setBlock(checkPos, state.setValue(BlockStateProperties.LAYERS, layers + 1),
                                net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
                    }
                    break; // 只处理最顶层
                }
            }
        }
    }
}
