package io.github.createmeow.timex_rebirth.client;

import io.github.createmeow.timex_rebirth.TimeX;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/**
 * 暴风雪屏幕白幕（沉浸感增强）：暴风雪天气下全屏覆盖一层淡白，
 * 配合白雾/视场角收窄与暴风雪雪花墙，营造暴风雪的压迫感。
 * 覆盖层强度平滑过渡：暴风雪来临淡入较快，结束后缓慢"解冻"淡出（模拟冰面融化的过程）。
 * 未直接接触暴风雪（躲进室内/檐下/洞穴，头顶被遮挡）时同样缓慢淡出。
 * 仅用全屏淡白覆盖层（渐变易与光影冲突，故不用）。
 */
@SuppressWarnings("removal")
@EventBusSubscriber(modid = TimeX.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class BlizzardOverlay {

    private static final ResourceLocation ID = TimeX.rl("blizzard_overlay");
    /** 淡入速率（强度/秒，约 0.5 秒完全显现） */
    private static final float FADE_IN_RATE = 2.0F;
    /** 淡出速率（强度/秒，约 6.7 秒缓慢解冻） */
    private static final float FADE_OUT_RATE = 0.15F;
    /** 全屏覆盖层的基础 alpha */
    private static final int BASE_ALPHA = 0x1E;

    /** 当前覆盖强度 0~1，跨帧平滑过渡。 */
    private static float currentStrength = 0.0F;
    private static long lastMillis = -1L;

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ID, (graphics, delta) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.player == null) return;

            // 按真实时间平滑过渡：淡入快、淡出慢（解冻过程）
            long now = Util.getMillis();
            if (lastMillis == -1L) lastMillis = now;
            float dt = Math.min((now - lastMillis) / 1000.0F, 0.1F);
            lastMillis = now;
            // 仅当暴风雪且玩家头顶直接暴露于天空（未受室内/檐下/洞穴遮挡）时保持覆盖层；
            // 未直接接触暴风雪时缓慢淡出（canSeeSky 基于高度图，不受昼夜亮度影响）
            float target = ClientWeatherState.isBlizzard()
                    && mc.level.canSeeSky(BlockPos.containing(mc.player.getEyePosition())) ? 1.0F : 0.0F;
            if (target > currentStrength) {
                currentStrength = Math.min(currentStrength + dt * FADE_IN_RATE, target);
            } else {
                currentStrength = Math.max(currentStrength - dt * FADE_OUT_RATE, target);
            }
            if (currentStrength <= 0.0F) return;

            // 全屏淡白覆盖层（强度缩放 alpha）
            int alpha = (int) (BASE_ALPHA * currentStrength);
            graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), alpha << 24 | 0xFFFFFF);
        });
    }
}
