package io.github.createmeow.timex_rebirth.antifreeze;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;
import java.util.Optional;

/**
 * 防冻剂使用成就触发器（自定义 advancement trigger，NeoForge 1.21.1 注册于 Registries.TRIGGER_TYPE）：
 * 玩家用防冻剂成功转换土壤时触发，按 soil_type 区分：
 * - DIRT：泥土/草方块 → 抗冻土壤
 * - FARMLAND：耕地 → 抗冻耕地
 * - PERMAFROST：冻土 → 抗冻土壤（解冻）
 */
public class AntiFreezeUsedTrigger extends SimpleCriterionTrigger<AntiFreezeUsedTrigger.Instance> {

    @Override
    public Codec<Instance> codec() {
        return Instance.CODEC;
    }

    public void trigger(ServerPlayer player, SoilType soilType) {
        this.trigger(player, instance -> instance.soilType().isEmpty() || instance.soilType().get() == soilType);
    }

    public enum SoilType {
        DIRT, FARMLAND, PERMAFROST;
        public static final Codec<SoilType> CODEC = Codec.STRING.xmap(
                s -> valueOf(s.toUpperCase(Locale.ROOT)),
                s -> s.name().toLowerCase(Locale.ROOT));
    }

    public record Instance(Optional<ContextAwarePredicate> player, Optional<SoilType> soilType) implements SimpleInstance {
        public static final Codec<Instance> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                ContextAwarePredicate.CODEC.optionalFieldOf("player").forGetter(Instance::player),
                SoilType.CODEC.optionalFieldOf("soil_type").forGetter(Instance::soilType)
        ).apply(inst, Instance::new));
    }
}
