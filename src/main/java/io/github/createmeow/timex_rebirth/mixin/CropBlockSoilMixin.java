package io.github.createmeow.timex_rebirth.mixin;

import io.github.createmeow.timex_rebirth.antifreeze.AntiFreezeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 允许所有作物种植在抗冻耕地上（原版 mayPlaceOn 只认 minecraft:farmland）。
 */
@Mixin(CropBlock.class)
public abstract class CropBlockSoilMixin {

    @Inject(method = "mayPlaceOn", at = @At("HEAD"), cancellable = true)
    private void timex_rebirth$allowAntiFreezeFarmland(BlockState state, BlockGetter level, BlockPos pos,
                                                       CallbackInfoReturnable<Boolean> cir) {
        if (state.is(AntiFreezeRegistry.ANTI_FREEZE_FARMLAND.get())) {
            cir.setReturnValue(true);
        }
    }
}
