package io.github.createmeow.timex_rebirth.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 暴风雪渲染（参考 FrostedHeart BlizzardRenderer，移植到 1.21.1）：
 * 以雪花纹理渲染围绕玩家的垂直飘雪墙，叠加在原有下雪效果之上形成暴风雪视觉。
 * 由 LevelRendererMixin 在 renderSnowAndRain 中调用。
 */
@OnlyIn(Dist.CLIENT)
public class BlizzardRenderer {

    private static final ResourceLocation SNOW_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/environment/snow.png");

    /**
     * 渲染暴风雪雪花墙。
     *
     * @param mc            客户端实例
     * @param level         客户端世界
     * @param lightTexture  光照纹理（用于雪花受光）
     * @param ticks         游戏刻数
     * @param partialTicks  渲染帧部分刻
     */
    public static void renderBlizzard(Minecraft mc, ClientLevel level, LightTexture lightTexture,
                                      int ticks, float partialTicks,
                                      double cameraX, double cameraY, double cameraZ) {
        float snowStrength = level.getThunderLevel(partialTicks); // 暴风雪强度（打雷等级）
        lightTexture.turnOnLightLayer();

        int camX = Mth.floor(cameraX);
        int camY = Mth.floor(cameraY);
        int camZ = Mth.floor(cameraZ);

        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.setShader(GameRenderer::getParticleShader);

        int renderRadius = Minecraft.useFancyGraphics() ? 10 : 5;
        RenderSystem.depthMask(Minecraft.useShaderTransparency());

        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
        float ticksAndPartialTicks = (float) ticks + partialTicks;

        // 雪快速下落：UV v 方向随时间滚动（参照 FrostedHeart BlizzardRenderer 的 fallSpeed，
        // 暴风雪的雪是高速倾泻而下，而非原版慢速飘落）
        float fallSpeed = ticksAndPartialTicks * 0.4F;

        BufferBuilder bufferBuilder = null;

        for (int z = camZ - renderRadius; z <= camZ + renderRadius; ++z) {
            for (int x = camX - renderRadius; x <= camX + renderRadius; ++x) {
                blockPos.set(x, camY, z);
                Biome biome = level.getBiome(blockPos).value();
                if (!biome.hasPrecipitation()) continue;

                int groundHeight = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                int minY = Math.max(camY - renderRadius, groundHeight);
                int maxY = camY + renderRadius;
                if (minY >= maxY) continue;

                if (bufferBuilder == null) {
                    RenderSystem.setShaderTexture(0, SNOW_TEXTURE);
                    bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
                }

                // 雪花飘动偏移（随时间缓慢摆动）
                double offsetX = Math.sin(ticksAndPartialTicks * 0.01 + x) * 0.5D;
                double offsetZ = Math.cos(ticksAndPartialTicks * 0.01 + z) * 0.5D;

                // 距离衰减：越远越淡
                double dx = x + 0.5D - cameraX;
                double dz = z + 0.5D - cameraZ;
                float distance = (float) Math.sqrt(dx * dx + dz * dz) / (float) renderRadius;
                float alpha = ((1.0F - distance * distance) * 0.3F + 0.5F) * snowStrength;
                int alpha255 = (int) (Mth.clamp(alpha, 0.0F, 1.0F) * 255.0F);

                // 光照采样（u=方块光，v=天空光）
                blockPos.set(x, groundHeight, z);
                int light = LevelRenderer.getLightColor(level, blockPos);
                int blockLight = light & 0xFFFF;
                int skyLight = (light >> 16) & 0xFFFF;

                // 下落滚动后的 v 坐标（v 减小 → 纹理向下滑动 → 雪高速倾泻）
                float vTop = minY * 0.25F - fallSpeed;
                float vBottom = maxY * 0.25F - fallSpeed;

                bufferBuilder.addVertex((float) (x - cameraX - 0.5D + offsetX), (float) (maxY - cameraY), (float) (z - cameraZ - 0.5D + offsetZ))
                        .setUv(0.0F, vTop)
                        .setColor(255, 255, 255, alpha255)
                        .setUv2(blockLight, skyLight);
                bufferBuilder.addVertex((float) (x - cameraX + 0.5D + offsetX), (float) (maxY - cameraY), (float) (z - cameraZ + 0.5D + offsetZ))
                        .setUv(1.0F, vTop)
                        .setColor(255, 255, 255, alpha255)
                        .setUv2(blockLight, skyLight);
                bufferBuilder.addVertex((float) (x - cameraX + 0.5D + offsetX), (float) (minY - cameraY), (float) (z - cameraZ + 0.5D + offsetZ))
                        .setUv(1.0F, vBottom)
                        .setColor(255, 255, 255, alpha255)
                        .setUv2(blockLight, skyLight);
                bufferBuilder.addVertex((float) (x - cameraX - 0.5D + offsetX), (float) (minY - cameraY), (float) (z - cameraZ - 0.5D + offsetZ))
                        .setUv(0.0F, vBottom)
                        .setColor(255, 255, 255, alpha255)
                        .setUv2(blockLight, skyLight);
            }
        }

        if (bufferBuilder != null) {
            MeshData mesh = bufferBuilder.buildOrThrow();
            BufferUploader.drawWithShader(mesh);
        }

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        lightTexture.turnOffLightLayer();
    }
}
