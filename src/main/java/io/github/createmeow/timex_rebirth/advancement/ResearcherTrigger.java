package io.github.createmeow.timex_rebirth.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;
import java.util.Optional;

/**
 * 我学会了！成就触发器：
 * 成功完成一项研究
 */
public class ResearcherTrigger extends SimpleCriterionTrigger<ResearcherTrigger.Instance> {

    @Override
    public Codec<Instance> codec() {
        return Instance.CODEC;
    }

    public void trigger(ServerPlayer player) {
        this.trigger(player, instance -> true);
    }

    public record Instance(Optional<ContextAwarePredicate> player) implements SimpleInstance {
        public static final Codec<Instance> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                ContextAwarePredicate.CODEC.optionalFieldOf("player").forGetter(Instance::player)
        ).apply(inst, Instance::new));
    }
}
