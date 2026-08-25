package io.github.createmeow.timex_rebirth.features;

import io.github.createmeow.timex_rebirth.TimeXConfig;
import io.github.createmeow.timex_rebirth.compat.ColdSweatCompat;
import io.github.createmeow.timex_rebirth.compat.ImmersiveWeatheringCompat;
import io.github.createmeow.timex_rebirth.heat.HeatFieldState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * 冻土转换系统（区块级采样，tickChunk 挂钩）：
 * - 泥土/耕地/砂土 在环境温度低的位置 → 冻土（immersive_weathering:permafrost）
 * - 草方块 在环境温度低的位置 → 冻草块（immersive_weathering:grassy_permafrost）
 * 环境温度由 Cold Sweat 决定（未安装时回退到生物群系温度）。
 * 入口：ServerLevelMixin_PlaceExtraSnow（tickChunk RETURN），与雪累积/水面结冰同节奏，
 * 每区块每 tick 仅采样少量随机位置，避免强制大量方块参与随机刻的性能开销。
 */
public class FrozenSoilHandler {

    /**
     * 对区块执行一次冻土转换尝试（每区块每 tick 固定采样 1 个位置，避免性能开销）。
     */
    public static void tickChunk(ServerLevel level, ChunkAccess chunk) {
        // 未安装 Immersive Weathering 则无冻土目标
        if (ImmersiveWeatheringCompat.getPermafrost() == null) return;

        int blockX = chunk.getPos().getMinBlockX();
        int blockZ = chunk.getPos().getMinBlockZ();
        // 取地表最高非空气方块位置（可能为作物/草/雪等），从其向下扫描数层
        BlockPos surfacePos = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE,
                level.getBlockRandomPos(blockX, 0, blockZ, 15));
        // 扫描地表到下方 3 层：无论上方是作物/草/雪还是空气，只要该层是
        // 泥土/耕地/砂土/草方块，就按环境温度决定是否转换为冻土
        for (int i = 0; i < 4; i++) {
            tryConvert(level, surfacePos.below(i));
        }
    }

    /**
     * 尝试转换单个位置，成功返回 true。
     */
    private static boolean tryConvert(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!isTargetBlock(state)) return false;
        if (!isColdEnvironment(level, pos)) return false;

        BlockState target;
        if (state.is(Blocks.GRASS_BLOCK)) {
            Block grassy = ImmersiveWeatheringCompat.getGrassyPermafrost();
            Block permafrost = ImmersiveWeatheringCompat.getPermafrost();
            target = (grassy != null ? grassy : permafrost).defaultBlockState();
        } else {
            target = ImmersiveWeatheringCompat.getPermafrost().defaultBlockState();
        }
        level.setBlockAndUpdate(pos, target);
        return true;
    }

    /**
     * 是否是需要可能转换为冻土的方块。
     */
    public static boolean isTargetBlock(BlockState state) {
        return state.is(Blocks.DIRT)
                || state.is(Blocks.FARMLAND)
                || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.GRASS_BLOCK);
    }

    /**
     * 环境温度是否低于阈值（Cold Sweat 优先，生物群系温度兜底）。
     * 处于基地热场暖场范围内的位置视为环境温暖，不转换为冻土。
     */
    private static boolean isColdEnvironment(ServerLevel level, BlockPos pos) {
        if (HeatFieldState.isHeated(level, pos)) return false;
        double temp = ColdSweatCompat.getTemperatureAt(level, pos);
        if (!Double.isNaN(temp)) {
            return temp < TimeXConfig.FROZEN_SOIL_TEMP_THRESHOLD.get();
        }
        // 兜底：生物群系基础温度
        return level.getBiome(pos).value().getBaseTemperature() < 0.2;
    }
}
