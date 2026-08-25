package io.github.createmeow.timex_rebirth.mixin;

import dev.ghen.thirst.foundation.gui.ThirstBarRenderer;
import dev.ghen.thirst.foundation.gui.RenderGuiEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 隐藏 ThirstWasTaken 原生口渴 HUD（ThirstBarRenderer.onBeginRenderAir 取消）。
 * 自定义 HUD 已复刻口渴条。
 */
@Mixin(ThirstBarRenderer.class)
public class ThirstBarRendererMixin {
    @Inject(method = "onBeginRenderAir", at = @At("HEAD"), cancellable = true, remap = false)
    private static void timex_rebirth$cancelThirstRender(RenderGuiEvent.Pre event, CallbackInfo ci) {
        ci.cancel();
    }
}
