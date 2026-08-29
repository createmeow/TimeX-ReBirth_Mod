package io.github.createmeow.timex_rebirth.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.createmeow.timex_rebirth.features.DryingRackBlockEntity;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * 晾晒架渲染器：仿照 Farmer's Delight 的 DefaultStoveRenderer，
 * 用 {@code itemRenderer.renderStatic(..., ItemDisplayContext.FIXED, ...)} 把架上物品渲染在横杆上方。
 */
public class DryingRackRenderer implements BlockEntityRenderer<DryingRackBlockEntity> {

    private static final float SIZE = 0.3F;

    /** 4 个槽位：物品竖直挂在前后两根顶横杆（z≈2 与 z≈14）下方，横向散布在横杆上。
     * 横杆 y≈12~13（0.75~0.81），物品顶部贴横杆底、竖直下垂。 */
    private static final float[][] OFFSETS = {
            { 4.5F / 16.0F, 2.0F / 16.0F },   // 前横杆左
            { 11.5F / 16.0F, 2.0F / 16.0F },  // 前横杆右
            { 4.5F / 16.0F, 14.0F / 16.0F },  // 后横杆左
            { 11.5F / 16.0F, 14.0F / 16.0F }, // 后横杆右
    };

    private final ItemRenderer itemRenderer;

    public DryingRackRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(DryingRackBlockEntity rack, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        for (int slot = 0; slot < DryingRackBlockEntity.SLOTS; slot++) {
            ItemStack stack = rack.getItem(slot);
            if (stack.isEmpty()) continue;

            poseStack.pushPose();
            // 物品竖直悬挂在顶横杆下方（中心 y ≈ 0.62，顶部约贴横杆底）
            poseStack.translate(OFFSETS[slot][0], 0.62D, OFFSETS[slot][1]);
            poseStack.scale(SIZE, SIZE, SIZE);

            int light = LevelRenderer.getLightColor(rack.getLevel(), rack.getBlockPos().above());
            itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, light, packedOverlay,
                    poseStack, buffer, rack.getLevel(), (int) rack.getBlockPos().asLong() + slot);
            poseStack.popPose();
        }
    }
}
