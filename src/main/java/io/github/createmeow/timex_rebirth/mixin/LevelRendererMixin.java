package io.github.createmeow.timex_rebirth.mixin;

import io.github.createmeow.timex_rebirth.client.BlizzardRenderer;
import io.github.createmeow.timex_rebirth.client.ClientWeatherState;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 暴风雪渲染与音效挂钩（参照 FrostedHeart LevelRendererMixin）：
 * - 暴风雪天气下以自定义暴风雪渲染替代原版雨雪渲染（原版 renderSnowAndRain 为 private，需 mixin 注入）
 * - 暴风雪时循环雨声、周期雷声增强沉浸感（原版 renderSnowAndRain 被替代后雨声微弱）
 */
@OnlyIn(Dist.CLIENT)
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Shadow
    @Final
    private Minecraft minecraft;
    @Shadow
    private ClientLevel level;
    @Shadow
    private int ticks;
    @Shadow
    private int rainSoundTime;

    /** 暴风雪雷声间隔计时（tick）。 */
    @Unique
    private int timex_rebirth$blizzardThunderTime = 0;

    @Inject(method = "renderSnowAndRain", at = @At("HEAD"), cancellable = true)
    private void timex_rebirth$renderBlizzard(LightTexture lightTexture, float partialTick,
                                              double x, double y, double z, CallbackInfo ci) {
        if (ClientWeatherState.isBlizzard() && this.level != null) {
            BlizzardRenderer.renderBlizzard(this.minecraft, this.level, lightTexture,
                    this.ticks, partialTick, x, y, z);
            ci.cancel();
        }
    }

    /**
     * 暴风雪音效：禁用原版稀疏雨声，改为更明显的循环雨声 + 周期雷声（参照 FrostedHeart）。
     */
    @Inject(method = "tickRain", at = @At("HEAD"))
    private void timex_rebirth$blizzardSounds(Camera camera, CallbackInfo ci) {
        if (this.level == null || !ClientWeatherState.isBlizzard()) return;

        // 禁用原版雨声（过于稀疏），由下方自定义雨声替代
        this.rainSoundTime = -1;

        BlockPos center = BlockPos.containing(camera.getPosition());

        // 循环雨声：每 2 秒一次
        if (this.ticks % 40 == 0) {
            this.level.playLocalSound(center, SoundEvents.WEATHER_RAIN, SoundSource.WEATHER,
                    0.15F, 0.7F, false);
        }

        // 周期雷声：每 15~25 秒随机一次（营造暴风雪轰鸣感）
        if (--this.timex_rebirth$blizzardThunderTime <= 0) {
            this.timex_rebirth$blizzardThunderTime = 20 * 15 + this.level.random.nextInt(20 * 10);
            this.level.playLocalSound(center, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER,
                    1.0F, 1.0F, false);
        }
    }
}
