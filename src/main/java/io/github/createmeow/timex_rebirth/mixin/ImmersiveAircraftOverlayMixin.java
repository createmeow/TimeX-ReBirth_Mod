package io.github.createmeow.timex_rebirth.mixin;

import immersive_aircraft.client.OverlayRenderer;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 隐藏 Immersive Aircraft 原生耐久/引擎 HUD。
 * 1.4.6 通过 mixin 注入 Gui#renderHotbarAndDecorations(HEAD) 调用
 * {@code OverlayRenderer.renderOverlay} 绘制飞机仪表盘；直接取消该方法本体。
 */
@Mixin(OverlayRenderer.class)
public class ImmersiveAircraftOverlayMixin {
    @Inject(method = "renderOverlay", at = @At("HEAD"), cancellable = true, remap = false)
    private static void timex_rebirth$cancelAircraftOverlay(GuiGraphics graphics, float partialTick, int screenWidth, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(0);
    }
}
