package io.github.createmeow.timex_rebirth.mixin;

import io.github.createmeow.timex_rebirth.TimeXConfig;
import io.github.createmeow.timex_rebirth.heat.HeatFieldState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Cold Sweat WorldHelper.getTemperatureAt 挂钩：
 * 基地热场暖场范围内的位置返回不低于 heat.field_temp 的环境温度。
 * 与壁炉供暖同一路径，使 fiahi 的箱子内食物/掉落物（用 getTemperatureAt
 * 判定温度）在热场内正常腐烂、不再冻结。
 * 依赖链保证 Cold Sweat 必然存在（fiahi 强制依赖 cold_sweat），故直接应用。
 */
@Mixin(targets = "com.momosoftworks.coldsweat.util.world.WorldHelper")
public class ColdSweatWorldHelperMixin {

    @Inject(method = "getTemperatureAt(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)D",
            at = @At("RETURN"), cancellable = true)
    private static void timex_rebirth$heatFieldTemperature(Level level, BlockPos pos, CallbackInfoReturnable<Double> cir) {
        if (HeatFieldState.isHeated(level, pos)) {
            double floor = TimeXConfig.HEAT_FIELD_TEMP.get();
            if (cir.getReturnValue() < floor) {
                cir.setReturnValue(floor);
            }
        }
    }
}
