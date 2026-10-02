package io.github.createmeow.timex_rebirth.mixin;

import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 取消 AppleSkin 的图形饥饿/饱和度 tooltip（FoodTooltip + FoodTooltipRenderer），
 * 改由 {@link io.github.createmeow.timex_rebirth.client.FoodValuesTooltipHandler}
 * 以自定义字体符号文本格式（\uEC03 饥饿 \uEC04 饱和）显示。
 * <p>仅当 AppleSkin 加载时由 {@link TimeXMixinPlugin} 启用。
 */
@Mixin(targets = "squeek.appleskin.client.TooltipOverlayHandler", remap = false)
public class AppleSkinTooltipMixin {

    @Inject(method = "gatherTooltips", at = @At("HEAD"), cancellable = true, remap = false)
    private void timex_rebirth$cancelAppleSkinFoodTooltip(RenderTooltipEvent.GatherComponents event, CallbackInfo ci) {
        ci.cancel();
    }
}
