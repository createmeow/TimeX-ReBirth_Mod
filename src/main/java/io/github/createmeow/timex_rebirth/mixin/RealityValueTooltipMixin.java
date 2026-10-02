package io.github.createmeow.timex_rebirth.mixin;

import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 取消 RealityValue 的图形恢复量 tooltip（RestoreTooltip + RestoreTooltipRenderer，
 * 由 AppleskinCompat.onGatherTooltips 通过 RenderTooltipEvent.GatherComponents 注入）。
 * <p>RealityValue 新版（1.0.2+）仿照 AppleSkin/ThirstWasTaken 添加了图形化恢复量 tooltip，
 * 此处取消以避免与 {@link io.github.createmeow.timex_rebirth.client.FoodValuesTooltipHandler}
 * 的符号文本格式重复或冲突。
 * <p>仅当 RealityValue 加载时由 {@link TimeXMixinPlugin} 启用。
 */
@Mixin(targets = "dev.anye.mc.reality_value.client.gui.appleskin.AppleskinCompat", remap = false)
public class RealityValueTooltipMixin {

    @Inject(method = "onGatherTooltips", at = @At("HEAD"), cancellable = true, remap = false)
    private void timex_rebirth$cancelRealityValueRestoreTooltip(RenderTooltipEvent.GatherComponents event, CallbackInfo ci) {
        ci.cancel();
    }
}
