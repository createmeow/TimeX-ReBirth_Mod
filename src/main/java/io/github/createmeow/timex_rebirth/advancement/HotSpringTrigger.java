package io.github.createmeow.timex_rebirth.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * "真正的温泉"成就触发器：
 * 泡在热流中，感受它带来的温暖。
 */
public class HotSpringTrigger extends SimpleCriterionTrigger<HotSpringTrigger.Instance> {

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
