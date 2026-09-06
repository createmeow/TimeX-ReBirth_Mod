package io.github.createmeow.timex_rebirth.features;

import io.github.createmeow.timex_rebirth.TimeXConfig;
import io.github.createmeow.timex_rebirth.compat.ColdSweatCompat;
import io.github.createmeow.timex_rebirth.compat.ImmersiveWeatheringCompat;
import io.github.createmeow.timex_rebirth.features.flint.FlintGearRegistry;
import io.github.createmeow.timex_rebirth.weather.TimeXWeather;
import io.github.createmeow.timex_rebirth.weather.WeatherSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Set;

/**
 * 雪累积系统（参考 FrostedHeart ClimateCommonEvents.placeExtraSnow）：
 * - 降雪天气下，已覆盖雪层的方块雪层 +1（上限 5 层）
 * - 积雪下方的土壤类方块替换为 Immersive Weathering 的冻土变体
 * 入口：ServerLevelMixin_PlaceExtraSnow（tickChunk RETURN）。
 */
public class SnowAccumulationHandler {

    /** 积雪时可替换为冻土的土壤类方块（grass 单独映射为 grassy_permafrost） */
    private static final Set<Block> SOIL_BLOCKS = Set.of(
            Blocks.GRASS_BLOCK, Blocks.DIRT, Blocks.COARSE_DIRT, Blocks.PODZOL, Blocks.MYCELIUM,
            Blocks.ROOTED_DIRT, Blocks.SAND, Blocks.RED_SAND, Blocks.GRAVEL, Blocks.CLAY, Blocks.MUD
    );

    /**
     * 对区块执行一次雪累积尝试（每个区块每个随机刻至多一次）。
     */
    public static void placeExtraSnow(ServerLevel level, ChunkAccess chunk) {
        if (!TimeXConfig.SNOW_ACCUMULATION_ENABLED.get()) return;
        if (level.getRandom().nextFloat() >= TimeXConfig.SNOW_ACCUMULATION_CHANCE.get()) return;

        int blockX = chunk.getPos().getMinBlockX();
        int blockZ = chunk.getPos().getMinBlockZ();
        // 与区块内随机位置绑定，保证确定性
        BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING,
                level.getBlockRandomPos(blockX, 0, blockZ, 15));
        BlockState state = level.getBlockState(pos);
        Biome biome = level.getBiome(pos).value();
        if (level.isRaining() && biome.coldEnoughToSnow(pos)
                && isColdEnough(level, pos)
                && level.getBrightness(LightLayer.BLOCK, pos) < 10
                && state.getBlock() == Blocks.SNOW) {
            int layers = state.getValue(BlockStateProperties.LAYERS);
            if (layers < 5) {
                // 暴雪升级：少量概率把增厚中的雪层转化为「可疑的积雪」（内含可刷刮的战利品）
                // 冰面上不生成可疑积雪（只正常加厚雪层）
                if (WeatherSystem.getWeather(level) == TimeXWeather.BLIZZARD
                        && level.getRandom().nextFloat() < TimeXConfig.BLIZZARD_SUSPICIOUS_SNOW_CHANCE.get()
                        && !isIceBelow(level, pos)) {
                    BlockState suspicious = FlintGearRegistry.SUSPICIOUS_SNOW.get().defaultBlockState()
                            .setValue(BlockStateProperties.LAYERS, 1 + layers);
                    level.setBlockAndUpdate(pos, suspicious);
                } else {
                    level.setBlockAndUpdate(pos, state.setValue(BlockStateProperties.LAYERS, 1 + layers));
                }
            }

            // 积雪下方的地面替换为冻土变体
            BlockPos belowPos = pos.below();
            BlockState belowState = level.getBlockState(belowPos);
            Block replacement = getSnowyTerrainReplacement(belowState.getBlock());
            if (replacement != null) {
                BlockState replacementState = replacement.defaultBlockState();
                if (replacementState.hasProperty(BlockStateProperties.SNOWY)) {
                    replacementState = replacementState.setValue(BlockStateProperties.SNOWY, true);
                }
                level.setBlockAndUpdate(belowPos, replacementState);
            } else if (belowState.hasProperty(BlockStateProperties.SNOWY)) {
                level.setBlockAndUpdate(belowPos, belowState.setValue(BlockStateProperties.SNOWY, true));
            }
        }
    }

    /**
     * 环境温度是否足够低（Cold Sweat 优先，生物群系温度兜底）。
     */
    private static boolean isColdEnough(ServerLevel level, BlockPos pos) {
        double temp = ColdSweatCompat.getTemperatureAt(level, pos);
        if (!Double.isNaN(temp)) {
            return temp < TimeXConfig.SNOW_ACCUMULATION_TEMP.get();
        }
        return level.getBiome(pos).value().getBaseTemperature() < 0.2;
    }

    /** 雪层下方是否为冰面（普通冰/浮冰/蓝冰/霜冰） */
    private static boolean isIceBelow(ServerLevel level, BlockPos pos) {
        Block below = level.getBlockState(pos.below()).getBlock();
        return below == Blocks.ICE || below == Blocks.PACKED_ICE
                || below == Blocks.BLUE_ICE || below == Blocks.FROSTED_ICE;
    }

    private static Block getSnowyTerrainReplacement(Block block) {
        if (block == Blocks.GRASS_BLOCK) {
            return ImmersiveWeatheringCompat.getGrassyPermafrost();
        }
        if (SOIL_BLOCKS.contains(block)) {
            return ImmersiveWeatheringCompat.getPermafrost();
        }
        return null;
    }
}
