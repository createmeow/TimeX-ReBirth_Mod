package io.github.createmeow.timex_rebirth.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.BossHealthOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 隐藏原版 Boss 条：TimeXHudRenderer 已在自定义 HUD 中复刻 Boss 条（{@code renderBossOverlay}），
 * 原版 {@code Gui} 的 BOSS_OVERLAY 层（{@link BossHealthOverlay#render}）若未取消会与其叠加显示双条。
 */
@Mixin(BossHealthOverlay.class)
public class BossHealthOverlayMixin {

    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;)V", at = @At("HEAD"), cancellable = true)
    private void timex_rebirth$hideVanillaBossBar(GuiGraphics graphics, CallbackInfo ci) {
        ci.cancel();
    }
}
