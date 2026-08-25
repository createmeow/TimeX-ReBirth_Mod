package io.github.createmeow.timex_rebirth.mixin;

import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 隐藏农夫乐事原生 HUD 覆盖条（Nourishment 滋养条 / Comfort 舒适条）。
 * FD 在 {@code HUDOverlays.register(RegisterGuiLayersEvent)} 中一次性注册两个覆盖层，
 * 直接取消注册入口即可全部隐藏；替代显示由 TimeXHudRenderer 负责
 * （滋养 → 饥饿条 hungry_nourish 材质，舒适 → 血条 0xFF8fb3c7）。
 * 字符串目标 + TimeXMixinPlugin 门控，FD 未安装时不应用。
 */
@Mixin(targets = "vectorwing.farmersdelight.client.gui.HUDOverlays")
public class FarmersDelightHudOverlaysMixin {

    @Inject(method = "register", at = @At("HEAD"), cancellable = true, remap = false)
    private static void timex_rebirth$cancelRegister(RegisterGuiLayersEvent event, CallbackInfo ci) {
        ci.cancel();
    }
}
