package io.github.createmeow.timex_rebirth.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 隐藏原版 HUD 中被原生 HUD 复刻/替代的元素（对照 FancyHUD 布局中 is_hidden=true 的 dummy）。
 *
 * <p>1.21.1 NeoForge 使用 {@code GuiLayerManager} 分层渲染：血条走 {@code renderHealthLevel}
 * （PLAYER_HEALTH 层，经 {@code lambda$new$0}），护甲走 {@code renderArmorLevel}，
 * 饥饿走 {@code renderFoodLevel}，氧气走 {@code renderAirLevel}——均非旧版的
 * {@code maybeRenderPlayerHealth}。经验条/坐骑血/跳跃条/计分板仍走 maybe 系列。
 * 准星/快捷栏/消息/标题等保留。
 */
@Mixin(Gui.class)
public class GuiHudMixin {

    /** 玩家血条（PLAYER_HEALTH 层）。 */
    @Inject(method = "renderHealthLevel", at = @At("HEAD"), cancellable = true)
    private void timex_rebirth$hideHealthLevel(GuiGraphics graphics, CallbackInfo ci) {
        ci.cancel();
    }

    /** 玩家护甲。 */
    @Inject(method = "renderArmorLevel", at = @At("HEAD"), cancellable = true)
    private void timex_rebirth$hideArmorLevel(GuiGraphics graphics, CallbackInfo ci) {
        ci.cancel();
    }

    /** 玩家饥饿。 */
    @Inject(method = "renderFoodLevel", at = @At("HEAD"), cancellable = true)
    private void timex_rebirth$hideFoodLevel(GuiGraphics graphics, CallbackInfo ci) {
        ci.cancel();
    }

    /** 玩家氧气。 */
    @Inject(method = "renderAirLevel", at = @At("HEAD"), cancellable = true)
    private void timex_rebirth$hideAirLevel(GuiGraphics graphics, CallbackInfo ci) {
        ci.cancel();
    }

    /** 经验条。 */
    @Inject(method = "maybeRenderExperienceBar", at = @At("HEAD"), cancellable = true)
    private void timex_rebirth$hideExperienceBar(GuiGraphics graphics, DeltaTracker tracker, CallbackInfo ci) {
        ci.cancel();
    }

    /** 经验等级文本。 */
    @Inject(method = "renderExperienceLevel", at = @At("HEAD"), cancellable = true)
    private void timex_rebirth$hideExperienceLevel(GuiGraphics graphics, DeltaTracker tracker, CallbackInfo ci) {
        ci.cancel();
    }

    /** 坐骑生命条。 */
    @Inject(method = "maybeRenderVehicleHealth", at = @At("HEAD"), cancellable = true)
    private void timex_rebirth$hideVehicleHealth(GuiGraphics graphics, DeltaTracker tracker, CallbackInfo ci) {
        ci.cancel();
    }

    /** 跳跃条。 */
    @Inject(method = "maybeRenderJumpMeter", at = @At("HEAD"), cancellable = true)
    private void timex_rebirth$hideJumpMeter(GuiGraphics graphics, DeltaTracker tracker, CallbackInfo ci) {
        ci.cancel();
    }

    /** 计分板侧栏。 */
    @Inject(method = "renderScoreboardSidebar", at = @At("HEAD"), cancellable = true)
    private void timex_rebirth$hideScoreboardSidebar(GuiGraphics graphics, DeltaTracker tracker, CallbackInfo ci) {
        ci.cancel();
    }
}
