package io.github.createmeow.timex_rebirth.features;

import io.github.createmeow.timex_rebirth.TimeXConfig;
import io.github.createmeow.timex_rebirth.compat.ColdSweatCompat;
import io.github.createmeow.timex_rebirth.compat.ImmersiveWeatheringCompat;
import io.github.createmeow.timex_rebirth.heat.HeatFieldState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.HashSet;
import java.util.Set;

/**
 * 冻土转换系统（区块级采样，tickChunk 挂钩）：
 * - 普通土（泥土/耕地/砂土/草方块/土径/灰化土/菌丝/带根泥土/IW 各种土）在低温 → 冻土（immersive_weathering:permafrost）
 * - 农夫乐事沃土（rich_soil / rich_soil_farmland）在低温 → 冻结的沃土（timex_rebirth:frozen_rich_soil），
 *   速度仅为普通土的一半。
 * 环境温度由 Cold Sweat 决定（未安装时回退到生物群系温度）。
 * 入口：ServerLevelMixin_PlaceExtraSnow（tickChunk RETURN），与雪累积/水面结冰同节奏。
 */
public class FrozenSoilHandler {

    // Immersive Weathering 的各种土（仅在 IW 加载时解析，作为"由土组成"的普通土目标）
    private static final String[] IW_SOIL_IDS = {
            "silt", "grassy_silt", "silty_farmland",
            "sandy_dirt", "grassy_sandy_dirt", "sandy_farmland",
            "earthen_clay", "grassy_earthen_clay", "earthen_clay_farmland",
            "loam", "loamy_farmland",
            "mulch_block", "nulch_block", "rooted_grass_block"
    };
    private static Set<Block> iwSoilBlocks;

    // 农夫乐事沃土（仅 FD 加载时解析）
    private static Block richSoil;
    private static Block richSoilFarmland;

    /**
     * 对区块执行一次冻土转换尝试（每区块每 tick 固定采样 1 个位置，避免性能开销）。
     */
    public static void tickChunk(ServerLevel level, ChunkAccess chunk) {
        int blockX = chunk.getPos().getMinBlockX();
        int blockZ = chunk.getPos().getMinBlockZ();
        BlockPos surfacePos = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE,
                level.getBlockRandomPos(blockX, 0, blockZ, 15));
        for (int i = 0; i < 4; i++) {
            tryConvert(level, surfacePos.below(i));
        }
    }

    /**
     * 尝试转换单个位置，成功返回 true。
     */
    private static boolean tryConvert(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);

        // 农夫乐事沃土：低温下以普通土一半的速度冻结成"冻结的沃土"
        if (isRichSoil(state)) {
            if (!isColdEnvironment(level, pos)) return false;
            // 半速：50% 概率本次转换为冻结的沃土（平均采样两次才转一次）
            if (level.getRandom().nextFloat() >= 0.5f) return false;
            level.setBlockAndUpdate(pos, FrozenRichSoilRegistry.FROZEN_RICH_SOIL.get().defaultBlockState());
            return true;
        }

        if (!isTargetBlock(state)) return false;
        // 普通土→冻土需要 Immersive Weathering 提供冻土目标
        Block permafrost = ImmersiveWeatheringCompat.getPermafrost();
        if (permafrost == null) return false;
        if (!isColdEnvironment(level, pos)) return false;

        BlockState target;
        if (state.is(Blocks.GRASS_BLOCK)) {
            Block grassy = ImmersiveWeatheringCompat.getGrassyPermafrost();
            target = (grassy != null ? grassy : permafrost).defaultBlockState();
        } else {
            target = permafrost.defaultBlockState();
        }
        level.setBlockAndUpdate(pos, target);
        return true;
    }

    /**
     * 是否是需要可能转换为冻土（普通速度）的普通土方块。
     * 包括：泥土/耕地/砂土/草方块/土径/灰化土/菌丝/带根泥土，以及 IW 的各种土。
     */
    public static boolean isTargetBlock(BlockState state) {
        Block b = state.getBlock();
        return b == Blocks.DIRT
                || b == Blocks.FARMLAND
                || b == Blocks.COARSE_DIRT
                || b == Blocks.GRASS_BLOCK
                || b == Blocks.DIRT_PATH
                || b == Blocks.PODZOL
                || b == Blocks.ROOTED_DIRT
                || b == Blocks.MYCELIUM
                || isIwSoil(b);
    }

    /**
     * 农夫乐事沃土（rich_soil / rich_soil_farmland），半速冻结成冻结的沃土。
     */
    private static boolean isRichSoil(BlockState state) {
        Block b = state.getBlock();
        if (b == null) return false;
        if (richSoil == null) {
            richSoil = BuiltInRegistries.BLOCK.getOptional(
                    ResourceLocation.fromNamespaceAndPath("farmersdelight", "rich_soil")).orElse(null);
            richSoilFarmland = BuiltInRegistries.BLOCK.getOptional(
                    ResourceLocation.fromNamespaceAndPath("farmersdelight", "rich_soil_farmland")).orElse(null);
        }
        return b == richSoil || b == richSoilFarmland;
    }

    private static boolean isIwSoil(Block block) {
        if (!ImmersiveWeatheringCompat.isLoaded()) return false;
        if (iwSoilBlocks == null) {
            iwSoilBlocks = new HashSet<>();
            for (String id : IW_SOIL_IDS) {
                BuiltInRegistries.BLOCK.getOptional(
                        ResourceLocation.fromNamespaceAndPath(ImmersiveWeatheringCompat.MOD_ID, id))
                        .ifPresent(iwSoilBlocks::add);
            }
        }
        return iwSoilBlocks.contains(block);
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
