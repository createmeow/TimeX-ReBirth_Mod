package io.github.createmeow.timex_rebirth.mixin;

import dev.anye.mc.reality_value.client.gui.PlayerExHudRenderer;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 隐藏 RealityValue 原生理智/健康 HUD（PlayerExHudRenderer.register 注册的 GUI 层取消）。
 * 自定义 HUD 已复刻理智/健康条。
 */
@Mixin(PlayerExHudRenderer.class)
public class RealityValueHudMixin {
    @Inject(method = "register", at = @At("HEAD"), cancellable = true, remap = false)
    private static void timex_rebirth$cancelRealityValueHud(RegisterGuiLayersEvent event, CallbackInfo ci) {
        ci.cancel();
    }
}
