package io.github.createmeow.timex_rebirth.mixin;

import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 取消 ThirstWasTaken 的图形口渴/解渴度 tooltip（FoodTooltip + FoodTooltipRenderer），
 * 改由 {@link io.github.createmeow.timex_rebirth.client.FoodValuesTooltipHandler}
 * 以自定义字体符号文本格式（\uEC05 口渴 \uEC06 解渴）显示。
 * <p>仅当 ThirstWasTaken 加载时由 {@link TimeXMixinPlugin} 启用。
 */
@Mixin(targets = "dev.ghen.thirst.foundation.gui.appleskin.TooltipOverlayHandler", remap = false)
public class ThirstTooltipMixin {

    @Inject(method = "gatherTooltips", at = @At("HEAD"), cancellable = true, remap = false)
    private void timex_rebirth$cancelThirstFoodTooltip(RenderTooltipEvent.GatherComponents event, CallbackInfo ci) {
        ci.cancel();
    }
}
