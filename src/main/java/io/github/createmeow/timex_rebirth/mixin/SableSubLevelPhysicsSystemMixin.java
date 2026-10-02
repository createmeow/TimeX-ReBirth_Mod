package io.github.createmeow.timex_rebirth.mixin;

import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Sable 物理模拟性能优化：
 * 通过跳过部分物理 tick 来降低每秒物理模拟次数。
 * 在 timex_rebirth config 中通过 sablePhysicsSkipTicks 配置（0=禁用）。
 */
@Mixin(SubLevelPhysicsSystem.class)
public abstract class SableSubLevelPhysicsSystemMixin {

    @Unique
    private int timex_rebirth$skipCounter = 0;

    @Inject(method = "tickPipelinePhysics", at = @At("HEAD"), cancellable = true)
    private void timex_rebirth$skipPhysicsTick(CallbackInfo ci) {
        int skipTicks = io.github.createmeow.timex_rebirth.TimeXConfig.SABLE_PHYSICS_SKIP_TICKS.get();
        if (skipTicks <= 0) {
            return;
        }
        timex_rebirth$skipCounter++;
        if (timex_rebirth$skipCounter <= skipTicks) {
            ci.cancel();
        } else {
            timex_rebirth$skipCounter = 0;
        }
    }
}
