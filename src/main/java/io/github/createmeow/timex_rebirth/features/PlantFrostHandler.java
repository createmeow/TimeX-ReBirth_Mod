package io.github.createmeow.timex_rebirth.features;

import com.mojang.serialization.Codec;
import io.github.createmeow.timex_rebirth.TimeXConfig;
import io.github.createmeow.timex_rebirth.compat.ColdSweatCompat;
import io.github.createmeow.timex_rebirth.heat.HeatFieldState;
import io.github.createmeow.timex_rebirth.weather.TimeXWeather;
import io.github.createmeow.timex_rebirth.weather.WeatherSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Locale;
import java.util.Optional;

/**
 * 植物抗冻/抗热枯萎系统。
 * 抗冻等级：
 *  - 低：雨天/雷暴/暴风雪露天 或 环境温度低 → 一段时间后枯萎
 *  - 中：雷暴/暴风雪露天 → 一段时间后枯萎
 * 抗热等级：
 *  - 低/中：环境温度高 → 一段时间后枯萎
 * 等级来源：PlantTempData 数据包（data/timex_rebirth/plant_temp.json）优先，
 *  其次是标签（timex_rebirth:frost_low/medium/high、heat_*），最后内置默认兜底。
 */
public class PlantFrostHandler {

    public enum Rating {
        LOW, MEDIUM, HIGH;
        public static final Codec<Rating> CODEC = Codec.STRING
                .xmap(s -> valueOf(s.toUpperCase(Locale.ROOT)), r -> r.name().toLowerCase(Locale.ROOT));
    }

    // 标签
    public static final TagKey<Block> FROST_LOW = BlockTags.create(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("timex_rebirth", "frost_low"));
    public static final TagKey<Block> FROST_MEDIUM = BlockTags.create(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("timex_rebirth", "frost_medium"));
    public static final TagKey<Block> FROST_HIGH = BlockTags.create(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("timex_rebirth", "frost_high"));
    public static final TagKey<Block> HEAT_LOW = BlockTags.create(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("timex_rebirth", "heat_low"));
    public static final TagKey<Block> HEAT_MEDIUM = BlockTags.create(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("timex_rebirth", "heat_medium"));
    public static final TagKey<Block> HEAT_HIGH = BlockTags.create(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("timex_rebirth", "heat_high"));

    public static boolean handleRandomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!isPlant(state)) return false;
        // 枯萎产物（死灌木）不再处理
        if (state.is(Blocks.DEAD_BUSH)) return false;
        // 基地热场暖场范围内的植物受保护：无论温度还是露天降水都不枯萎，正常生长
        if (HeatFieldState.isHeated(level, pos)) return false;

        PlantTempData data = getPlantTempData(level, state.getBlock());
        if (!shouldWilt(level, pos, state, data)) return false;

        // 阻止植物生长：始终取消原 randomTick（低温/恶劣天气下作物不能正常长大）
        if (random.nextDouble() < TimeXConfig.PLANT_WILT_CHANCE.get()) {
            Block dead = data != null ? data.dead() : Blocks.DEAD_BUSH;
            wilt(level, pos, state, dead);
        }
        return true;
    }

    /**
     * 判定植物当前是否满足枯萎条件（含 will_die 检查，不含概率）。
     * 随机刻枯萎与区块刻快速枯萎扫描共用。
     */
    private static boolean shouldWilt(ServerLevel level, BlockPos pos, BlockState state, PlantTempData data) {
        Rating frost = getFrostRating(state, data);
        Rating heat = getHeatRating(state, data);

        boolean exposed = level.canSeeSky(pos);
        TimeXWeather weather = WeatherSystem.getWeather(level);
        boolean blizzard = weather == TimeXWeather.BLIZZARD;
        boolean thunder = weather.isThundering();
        boolean raining = weather == TimeXWeather.RAIN || weather == TimeXWeather.SNOW || thunder || blizzard;

        boolean coldEnv = isColdEnvironment(level, pos);
        boolean hotEnv = isHotEnvironment(level, pos);

        boolean wilt = false;
        // 抗冻
        if (frost == Rating.LOW) {
            // snow_vulnerable 控制露天降水的枯萎条件；环境低温始终触发
            boolean snowVulnerable = data == null || data.snowVulnerable();
            if ((exposed && raining && snowVulnerable) || coldEnv) {
                wilt = true;
            }
        } else if (frost == Rating.MEDIUM) {
            boolean blizzardVulnerable = data == null || data.blizzardVulnerable();
            if (exposed && (blizzard || thunder) && blizzardVulnerable) {
                wilt = true;
            }
        }
        // 抗热（抗热低/中在高温位置枯萎）
        if ((heat == Rating.LOW || heat == Rating.MEDIUM) && hotEnv) {
            wilt = true;
        }

        if (!wilt) return false;
        return data == null || data.willDie();
    }

    /**
     * 区块刻快速枯萎扫描：与冻土转换同节奏（每区块每 tick 采样 1 个地表位置）。
     * 使不抗冻/不抗热植物在恶劣环境迅速枯萎，而非像普通随机刻那样平均数十分钟才轮一次。
     */
    public static void tickChunk(ServerLevel level, ChunkAccess chunk) {
        int blockX = chunk.getPos().getMinBlockX();
        int blockZ = chunk.getPos().getMinBlockZ();
        BlockPos surfacePos = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE,
                level.getBlockRandomPos(blockX, 0, blockZ, 15));
        // 从地表向下扫描（积雪/草可能覆盖在作物上方）
        for (int i = 0; i < 3; i++) {
            BlockPos pos = surfacePos.below(i);
            BlockState state = level.getBlockState(pos);
            if (!state.isAir()) {
                if (isPlant(state)) {
                    PlantTempData data = getPlantTempData(level, state.getBlock());
                    if (shouldWilt(level, pos, state, data)) {
                        Block dead = data != null ? data.dead() : Blocks.DEAD_BUSH;
                        wilt(level, pos, state, dead);
                    }
                }
                return;
            }
        }
    }

    /**
     * 将作物替换为枯萎方块（默认死灌木），若下方是耕地则转为泥土。
     */
    private static void wilt(ServerLevel level, BlockPos pos, BlockState oldState, Block dead) {
        if (level.getBlockState(pos.below()).is(Blocks.FARMLAND)) {
            level.setBlockAndUpdate(pos.below(), Blocks.DIRT.defaultBlockState());
        }
        level.setBlockAndUpdate(pos, dead.defaultBlockState());
    }

    /**
     * 查询方块的植物温度数据（数据包 DataMap）。
     */
    public static PlantTempData getPlantTempData(Level level, Block block) {
        Registry<Block> registry = level.registryAccess().registryOrThrow(Registries.BLOCK);
        return registry.getResourceKey(block)
                .flatMap(registry::getHolder)
                .map(h -> h.getData(PlantTempDataMap.TYPE))
                .orElse(null);
    }

    private static boolean isPlant(BlockState state) {
        Block block = state.getBlock();
        return block instanceof CropBlock
                || block instanceof SweetBerryBushBlock
                || block instanceof SaplingBlock
                || block instanceof CactusBlock
                || block instanceof FlowerBlock
                || block instanceof MushroomBlock
                || block instanceof VineBlock
                || state.is(BlockTags.CROPS)
                || state.is(BlockTags.SAPLINGS);
    }

    public static Rating getFrostRating(BlockState state) {
        return getFrostRating(state, null);
    }

    public static Rating getFrostRating(BlockState state, PlantTempData data) {
        if (data != null && data.frost() != null) return data.frost();
        if (state.is(FROST_LOW)) return Rating.LOW;
        if (state.is(FROST_MEDIUM)) return Rating.MEDIUM;
        if (state.is(FROST_HIGH)) return Rating.HIGH;
        return defaultFrost(state);
    }

    public static Rating getHeatRating(BlockState state) {
        return getHeatRating(state, null);
    }

    public static Rating getHeatRating(BlockState state, PlantTempData data) {
        if (data != null && data.heat() != null) return data.heat();
        if (state.is(HEAT_LOW)) return Rating.LOW;
        if (state.is(HEAT_MEDIUM)) return Rating.MEDIUM;
        if (state.is(HEAT_HIGH)) return Rating.HIGH;
        return defaultHeat(state);
    }

    private static Rating defaultFrost(BlockState state) {
        Block block = state.getBlock();
        // 作物：低抗冻
        if (block instanceof CropBlock || state.is(BlockTags.CROPS)) return Rating.LOW;
        // 高寒植物：高抗冻
        if (block instanceof SweetBerryBushBlock) return Rating.HIGH;
        if (block instanceof SaplingBlock) {
            String name = state.getBlock().getDescriptionId();
            if (name.contains("spruce") || name.contains("pine")) return Rating.HIGH;
        }
        // 仙人掌等热带植物：低抗冻
        if (block instanceof CactusBlock) return Rating.LOW;
        return Rating.MEDIUM;
    }

    private static Rating defaultHeat(BlockState state) {
        Block block = state.getBlock();
        // 作物：低抗热
        if (block instanceof CropBlock || state.is(BlockTags.CROPS)) return Rating.LOW;
        // 高抗热
        if (block instanceof CactusBlock) return Rating.HIGH;
        if (block instanceof SaplingBlock) {
            String name = state.getBlock().getDescriptionId();
            if (name.contains("jungle") || name.contains("acacia") || name.contains("mangrove")) return Rating.HIGH;
        }
        return Rating.MEDIUM;
    }

    private static boolean isColdEnvironment(ServerLevel level, BlockPos pos) {
        // 基地热场暖场范围内视为环境温暖，不触发低温枯萎
        if (HeatFieldState.isHeated(level, pos)) return false;
        double temp = ColdSweatCompat.getTemperatureAt(level, pos);
        if (!Double.isNaN(temp)) {
            return temp < TimeXConfig.PLANT_COLD_TEMP.get();
        }
        return level.getBiome(pos).value().getBaseTemperature() < 0.2;
    }

    private static boolean isHotEnvironment(ServerLevel level, BlockPos pos) {
        double temp = ColdSweatCompat.getTemperatureAt(level, pos);
        if (!Double.isNaN(temp)) {
            return temp > TimeXConfig.PLANT_HOT_TEMP.get();
        }
        return level.getBiome(pos).value().getBaseTemperature() > 0.9;
    }
}
