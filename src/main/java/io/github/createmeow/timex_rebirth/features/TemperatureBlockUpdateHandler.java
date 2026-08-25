package io.github.createmeow.timex_rebirth.features;

import io.github.createmeow.timex_rebirth.TimeXConfig;
import io.github.createmeow.timex_rebirth.compat.ColdSweatCompat;
import io.github.createmeow.timex_rebirth.heat.HeatFieldState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;

/**
 * 温度方块更新统一调度（参考 FrostedHeart ServerLevelMixin_TemperatureUpdate）：
 * 每个区块每次随机刻执行温度驱动的方块更新，当前包括：
 * - 水面结冰：低温环境下露天水源 → 冰（原版在全积雪世界中结冰分支不会触发，需自行实现）
 * 入口：ServerLevelMixin_PlaceExtraSnow（tickChunk RETURN）与雪累积统一调用。
 */
public class TemperatureBlockUpdateHandler {

    /**
     * 对区块执行一次温度方块更新尝试。
     */
    public static void tickChunk(ServerLevel level, ChunkAccess chunk) {
        if (!TimeXConfig.WATER_FREEZE_ENABLED.get()) return;
        if (level.getRandom().nextFloat() >= TimeXConfig.WATER_FREEZE_CHANCE.get()) return;

        int blockX = chunk.getPos().getMinBlockX();
        int blockZ = chunk.getPos().getMinBlockZ();
        // WORLD_SURFACE：取最顶层的非空气方块（露天水面即为水面方块）
        BlockPos pos = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE,
                level.getBlockRandomPos(blockX, 0, blockZ, 15));
        freezeWater(level, pos);
    }

    /**
     * 低温环境下将露天水源结冰。
     */
    private static void freezeWater(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        // 仅水源（非流动水）且为水面方块
        if (!(state.getBlock() instanceof LiquidBlock)) return;
        if (state.getFluidState().getType() != Fluids.WATER) return;
        if (!isColdEnough(level, pos)) return;
        level.setBlockAndUpdate(pos, Blocks.ICE.defaultBlockState());
    }

    private static boolean isColdEnough(ServerLevel level, BlockPos pos) {
        // 基地热场暖场范围内视为环境温暖，不结冰
        if (HeatFieldState.isHeated(level, pos)) return false;
        double temp = ColdSweatCompat.getTemperatureAt(level, pos);
        if (!Double.isNaN(temp)) {
            return temp < TimeXConfig.WATER_FREEZE_TEMP.get();
        }
        return level.getBiome(pos).value().getBaseTemperature() < 0.2;
    }
}
