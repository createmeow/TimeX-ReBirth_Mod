package io.github.createmeow.timex_rebirth.mixin;

import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 农夫乐事炉灶"不可虚空燃烧"：
 * <ul>
 *   <li>放置时 FD 默认 LIT=true → 注入 getStateForPlacement 强制熄灭。</li>
 *   <li>serverTick 扣减燃料，归零自动熄灭（见 FarmersDelightStoveTickFuelMixin）。</li>
 * </ul>
 * 点燃限制与填充燃料在 FireInteractHandler 事件层处理。
 */
@Mixin(vectorwing.farmersdelight.common.block.AbstractStoveBlock.class)
public abstract class FarmersDelightStoveFuelMixin {

    /** 放置时强制熄灭（FD 默认 LIT=true）。 */
    @Inject(method = "getStateForPlacement", at = @At("RETURN"), cancellable = true)
    private void timex_rebirth$extinguishOnPlace(BlockPlaceContext context, CallbackInfoReturnable<BlockState> cir) {
        cir.setReturnValue(cir.getReturnValue().setValue(vectorwing.farmersdelight.common.block.AbstractStoveBlock.LIT, false));
    }
}
