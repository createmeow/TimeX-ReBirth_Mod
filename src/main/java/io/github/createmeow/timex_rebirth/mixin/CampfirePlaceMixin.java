package io.github.createmeow.timex_rebirth.mixin;

import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 篝火"不可虚空燃烧"：放置时强制熄灭（原版放置即燃）。
 *
 * <p>注入 {@code getStateForPlacement}（CampfireBlock 自身声明的方法）
 * 强制返回 LIT=false，参考 FrostedHeart CampfireBlockMixin_TimeLimit。
 * 玩家需填充燃料并打火才能点燃。</p>
 */
@Mixin(net.minecraft.world.level.block.CampfireBlock.class)
public abstract class CampfirePlaceMixin {

    @Inject(method = "getStateForPlacement", at = @At("RETURN"), cancellable = true)
    private void timex_rebirth$extinguishOnPlace(BlockPlaceContext context, CallbackInfoReturnable<BlockState> cir) {
        cir.setReturnValue(cir.getReturnValue().setValue(net.minecraft.world.level.block.CampfireBlock.LIT, false));
    }
}
