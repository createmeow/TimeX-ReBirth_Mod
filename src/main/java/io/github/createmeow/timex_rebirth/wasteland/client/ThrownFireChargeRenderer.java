package io.github.createmeow.timex_rebirth.wasteland.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.createmeow.timex_rebirth.TimeX;
import io.github.createmeow.timex_rebirth.wasteland.ThrownFireCharge;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

/**
 * 投掷火焰弹渲染：始终面向摄像机的火球贴图（billboard 四方面片）。
 * 1.21.1 未提供原版 FireballRenderer，故自绘；贴图 timex_rebirth:textures/entity/thrown_fire_charge.png。
 */
@OnlyIn(Dist.CLIENT)
public class ThrownFireChargeRenderer extends EntityRenderer<ThrownFireCharge> {
    private static final ResourceLocation TEXTURE = TimeX.rl("textures/entity/thrown_fire_charge.png");

    public ThrownFireChargeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(ThrownFireCharge entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        // 绕自身旋转（火球翻滚效果），再面向相机
        float spin = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-spin));
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.scale(0.4F, 0.4F, 0.4F);
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix4f = pose.pose();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        vertex(consumer, matrix4f, pose, packedLight, 0.0F, 0, 0, 1);
        vertex(consumer, matrix4f, pose, packedLight, 1.0F, 0, 1, 1);
        vertex(consumer, matrix4f, pose, packedLight, 1.0F, 1, 1, 0);
        vertex(consumer, matrix4f, pose, packedLight, 0.0F, 1, 0, 0);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, PoseStack.Pose pose, int light,
                               float x, int u, int v, int alpha) {
        consumer.addVertex(matrix, x - 0.5F, (float) -alpha * 0.5F + 0.25F, 0.0F)
                .setColor(255, 255, 255, 255)
                .setUv((float) u, (float) v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(ThrownFireCharge entity) {
        return TEXTURE;
    }
}
