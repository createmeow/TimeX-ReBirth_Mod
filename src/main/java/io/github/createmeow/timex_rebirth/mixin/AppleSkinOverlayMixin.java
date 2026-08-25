package io.github.createmeow.timex_rebirth.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 隐藏 AppleSkin 原生 HUD 覆盖条（仅当 AppleSkin 加载时由 TimeXMixinPlugin 启用）。
 * AppleSkin 的 Health/Hunger/Saturation/Exhaustion 四个覆盖层都继承自
 * {@code HUDOverlayHandler$Overlay} 的 final {@code render(GuiGraphics, DeltaTracker)}，
 * 在此处取消即可全部隐藏；替代显示由 TimeXHudRenderer（hungry_none/thirsty_none 饱和度）负责。
 */
@Mixin(targets = "squeek.appleskin.client.HUDOverlayHandler$Overlay")
public class AppleSkinOverlayMixin {

    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V",
            at = @At("HEAD"), cancellable = true)
    private void timex_rebirth$hideAppleSkinOverlays(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        ci.cancel();
    }
}
