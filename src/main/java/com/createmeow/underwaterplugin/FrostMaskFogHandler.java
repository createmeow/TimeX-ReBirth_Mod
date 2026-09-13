package com.createmeow.underwaterplugin;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * 防寒面罩水下视物：对齐 Create 潜水头盔的雾处理
 * （ClientEvents.getFogDensity——眼睛没入水时把远平面拉远 6.25 倍并取消原版雾），
 * 穿戴防寒面罩（HEAD 槽）时水下不受迷雾影响。
 */
@EventBusSubscriber(modid = UnderwaterPlugin.MODID, value = Dist.CLIENT)
public final class FrostMaskFogHandler {

    private FrostMaskFogHandler() {}

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        Camera camera = event.getCamera();
        if (!(camera.getEntity() instanceof LivingEntity entity)) return;
        if (!entity.getItemBySlot(EquipmentSlot.HEAD).is(UnderwaterRegisters.FROST_MASK.get())) return;

        Level level = Minecraft.getInstance().level;
        if (level == null) return;

        // 眼睛入水判定（与 Create 同款：相机点低于所在方块液面高度）
        BlockPos blockPos = camera.getBlockPosition();
        FluidState fluidState = level.getFluidState(blockPos);
        if (camera.getPosition().y >= blockPos.getY() + fluidState.getHeight(level, blockPos)) return;
        if (!fluidState.is(FluidTags.WATER)) return;

        // 与 Create 潜水头盔同款系数
        event.scaleFarPlaneDistance(6.25f);
        event.setCanceled(true);
    }
}
