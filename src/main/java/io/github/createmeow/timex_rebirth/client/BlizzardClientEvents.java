package io.github.createmeow.timex_rebirth.client;

import io.github.createmeow.timex_rebirth.TimeX;
import io.github.createmeow.timex_rebirth.TimeXConfig;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * 暴风雪客户端特效（参考 FrostedHeart FogModification 平滑方案）：
 * - 露天玩家视场角减少（默认 10）
 * - 白雾：按缩放系数收窄远平面，雾强度根据头顶暴露度（天空亮度）平滑过渡，
 *   过渡时间约 10 秒；浸入水中不会立即取消特效，被实体方块遮挡时缓慢淡出。
 * 仅在暴风雪天气下生效。
 */
@EventBusSubscriber(modid = TimeX.MODID, value = Dist.CLIENT)
public class BlizzardClientEvents {

    /** 当前雾强度（0~1），跨帧平滑过渡。 */
    private static float currentFog = 0.0F;
    private static long prevFogTick = -1L;

    /** 目标雾强度：暴风雪 + 头顶天空亮度决定（0 完全遮挡 → 1 完全露天）。 */
    private static float computeTargetFog() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return 0.0F;
        if (!ClientWeatherState.isBlizzard()) return 0.0F;
        // 防寒面罩 + 铝背罐：防寒功能为铝背罐专属（配其他背罐只提供潜水视物，不挡暴雪白雾）。
        // 雾强度目标归零，按过渡时间平滑消散，FOV 同步恢复。
        if (player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).is(
                com.createmeow.underwaterplugin.UnderwaterRegisters.FROST_MASK.get())
                && player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).getItem()
                        instanceof com.createmeow.underwaterplugin.AluminumBacktankItem) return 0.0F;
        Level level = mc.level;
        // 天空亮度作为暴露度指标：头顶有方块 → 亮度低 → 雾淡；水中天空亮度不归零，满足"躲水不停止"
        int light = level.getBrightness(LightLayer.SKY, BlockPos.containing(player.getEyePosition()));
        float exposure = Mth.clampedMap((float) light, 0.0F, 15.0F, 0.0F, 1.0F);
        // 生物群系兜底：任何暴风雪下都保留最低雾（避免完全消失）
        Biome biome = level.getBiome(BlockPos.containing(player.getEyePosition())).value();
        if (biome.getBaseTemperature() >= 0.2F) return 0.0F;
        return Math.max(0.15F, exposure);
    }

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        if (currentFog <= 0.0F) return;
        float reduction = (float) (double) TimeXConfig.BLIZZARD_FOV_REDUCTION.get();
        event.setFOV(Math.max(1.0F, event.getFOV() - reduction * currentFog));
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        Camera camera = event.getCamera();
        if (!(camera.getEntity() instanceof LocalPlayer)) return;

        // 按真实时间平滑过渡（参考 FrostedHeart FogModification）
        long now = Util.getMillis();
        if (prevFogTick == -1L) prevFogTick = now;
        float deltaTick = (float) (now - prevFogTick) * 1.0E-4F; // 10 秒量级
        prevFogTick = now;
        float target = computeTargetFog();
        if (target > currentFog) {
            currentFog = Math.min(currentFog + deltaTick, target);
        } else if (target < currentFog) {
            currentFog = Math.max(currentFog - deltaTick, target);
        }

        if (currentFog <= 0.0F) return;

        // 白雾颜色由 ComputeFogColor 事件设置；此处收窄雾平面（scale 方式，适配任意渲染距离）
        float scale = (float) (double) TimeXConfig.BLIZZARD_FOG_SCALE.get();
        // 曲线插值：雾强度越高，收缩越明显
        float scaledDelta = 1.0F - (1.0F - currentFog) * (1.0F - currentFog);
        float farPlaneScale = (float) Mth.lerp(scaledDelta, 1.0F, scale);
        float nearPlaneScale = (float) Mth.lerp(scaledDelta, 1.0F, 0.3F * scale);
        event.scaleNearPlaneDistance(nearPlaneScale);
        event.scaleFarPlaneDistance(farPlaneScale);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRenderFogColor(ViewportEvent.ComputeFogColor event) {
        if (currentFog <= 0.0F) return;
        // 白雾
        event.setRed(0.90F);
        event.setGreen(0.92F);
        event.setBlue(0.96F);
    }
}
