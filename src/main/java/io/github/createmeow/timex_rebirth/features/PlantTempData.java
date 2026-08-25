package io.github.createmeow.timex_rebirth.features;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Optional;

/**
 * 植物温度数据（数据驱动，NeoForge DataMap）。
 * JSON 位于 data/timex_rebirth/plant_temp.json，按方块 ID（或 #标签）键控：
 * <pre>
 * {
 *   "replace": false,
 *   "values": {
 *     "minecraft:wheat": { "frost": "low", "heat": "low" }
 *   }
 * }
 * </pre>
 * frost/heat 缺省时回退到标签或内置默认；
 * snow_vulnerable 控制抗冻低植物是否在露天降水时枯萎；
 * blizzard_vulnerable 控制抗冻中植物是否在暴风雪/雷暴露天时枯萎；
 * dead 指定枯萎后变成的方块；will_die 为 false 时植物永不枯萎。
 */
public record PlantTempData(
        PlantFrostHandler.Rating frost,
        PlantFrostHandler.Rating heat,
        boolean snowVulnerable,
        boolean blizzardVulnerable,
        Block dead,
        boolean willDie
) {
    public static final Codec<PlantTempData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            PlantFrostHandler.Rating.CODEC.optionalFieldOf("frost").forGetter(o -> Optional.ofNullable(o.frost)),
            PlantFrostHandler.Rating.CODEC.optionalFieldOf("heat").forGetter(o -> Optional.ofNullable(o.heat)),
            Codec.BOOL.optionalFieldOf("snow_vulnerable", true).forGetter(PlantTempData::snowVulnerable),
            Codec.BOOL.optionalFieldOf("blizzard_vulnerable", true).forGetter(PlantTempData::blizzardVulnerable),
            BuiltInRegistries.BLOCK.byNameCodec().optionalFieldOf("dead", Blocks.DEAD_BUSH).forGetter(PlantTempData::dead),
            Codec.BOOL.optionalFieldOf("will_die", true).forGetter(PlantTempData::willDie)
    ).apply(instance, (frost, heat, snowVul, blizzardVul, dead, willDie) ->
            new PlantTempData(frost.orElse(null), heat.orElse(null), snowVul, blizzardVul, dead, willDie)));
}
