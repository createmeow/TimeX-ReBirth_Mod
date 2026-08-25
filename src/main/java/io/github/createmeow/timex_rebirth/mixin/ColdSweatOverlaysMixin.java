package io.github.createmeow.timex_rebirth.mixin;

import com.momosoftworks.coldsweat.client.gui.Overlays;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 隐藏 Cold Sweat 原生温度 HUD：其 Overlays.registerOverlays 注册的原生 GUI 层
 * 由本 Mixin 取消（自定义 HUD 已复刻体温条）。仅当本模组启用原生 HUD 时生效。
 */
@Mixin(Overlays.class)
public class ColdSweatOverlaysMixin {
    @Inject(method = "registerOverlays", at = @At("HEAD"), cancellable = true, remap = false)
    private static void timex_rebirth$cancelColdSweatOverlays(RegisterGuiLayersEvent event, CallbackInfo ci) {
        ci.cancel();
    }
}
