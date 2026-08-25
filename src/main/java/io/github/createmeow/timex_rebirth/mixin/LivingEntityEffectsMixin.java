package io.github.createmeow.timex_rebirth.mixin;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 防御性修复 1.21 的 hiddenEffect 效果卡住缺陷（MC-305658 同类）：
 * 同一效果不同等级反复施加时，低等级长时效果会覆盖高等级并把高等级存为
 * hiddenEffect；主效果到期恢复 hiddenEffect 时可能产生 duration<=0 的效果，
 * 导致效果图标永久卡在 00:00 且 /effect clear 无法清除（概率性出现）。
 * fiahi 反复吃不同等级冻/腐食物（SHIVER/缓慢/反胃等）易触发。
 * 这里在每 tick 效果处理前清理 duration<=0 且非无限的效果。
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityEffectsMixin {

    @Inject(method = "tickEffects", at = @At("HEAD"))
    private void timex_rebirth$removeStuckEffects(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        List<Holder<MobEffect>> stuck = new ArrayList<>();
        for (MobEffectInstance effect : self.getActiveEffectsMap().values()) {
            // duration==0：效果已到期但未被移除（hiddenEffect 卡 00:00 的异常状态）
            if (effect.getDuration() == 0) {
                stuck.add(effect.getEffect());
            }
        }
        for (Holder<MobEffect> holder : stuck) {
            self.removeEffect(holder);
        }
    }
}
