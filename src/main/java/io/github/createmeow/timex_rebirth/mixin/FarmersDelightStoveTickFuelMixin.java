package io.github.createmeow.timex_rebirth.mixin;

import io.github.createmeow.timex_rebirth.features.FireManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 农夫乐事炉灶 BE 每 tick 扣减燃料，归零自动熄灭。
 * 注入 AbstractStoveBlockEntity.serverTick 头部（StoveBlockEntity 继承自它）。
 */
@Mixin(vectorwing.farmersdelight.common.block.entity.AbstractStoveBlockEntity.class)
public abstract class FarmersDelightStoveTickFuelMixin {

    @Inject(method = "serverTick", at = @At("HEAD"))
    private static void timex_rebirth$consumeFuel(Level level, BlockPos pos, BlockState state,
                                                  vectorwing.farmersdelight.common.block.entity.AbstractStoveBlockEntity stove,
                                                  CallbackInfo ci) {
        FireManager.consumeTick(level, pos, state, stove);
    }
}
