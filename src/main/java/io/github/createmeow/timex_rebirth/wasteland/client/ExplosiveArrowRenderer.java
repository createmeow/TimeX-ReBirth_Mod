package io.github.createmeow.timex_rebirth.wasteland.client;

import io.github.createmeow.timex_rebirth.wasteland.ExplosiveArrow;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.TippableArrowRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 爆炸箭渲染：复用原版箭矢模型与贴图（仅客户端加载，服务端无此类）。
 */
@OnlyIn(Dist.CLIENT)
public class ExplosiveArrowRenderer extends ArrowRenderer<ExplosiveArrow> {
    public ExplosiveArrowRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(ExplosiveArrow entity) {
        return TippableArrowRenderer.NORMAL_ARROW_LOCATION;
    }
}
