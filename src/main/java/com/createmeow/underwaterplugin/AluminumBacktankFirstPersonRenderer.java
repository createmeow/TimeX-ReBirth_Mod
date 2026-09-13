package com.createmeow.underwaterplugin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderArmEvent;

/**
 * 铝背罐第一人称手臂渲染：完整复刻 Create NetheriteBacktankFirstPersonRenderer 的写法
 * （下界合金背罐穿戴时第一人称手臂用 netherite_diving_arm.png 渲染为潜水服手臂），
 * 换成本模组的 aluminum_diving_arm.png（下界合金手臂贴图铝色化）。
 * <ul>
 * <li>ClientTickEvent 维护激活标志：本地玩家胸甲槽穿着铝背罐时激活</li>
 * <li>RenderArmEvent（LOWEST 优先级）取消原版手臂渲染，改用潜水服手臂贴图
 *     渲染 sleeve 模型部件（FULL_BRIGHT + entitySolid）</li>
 * </ul>
 * 与 Create 的铜背罐（无手臂渲染）/下界合金背罐（netherite_diving_arm）行为对应。
 */
@EventBusSubscriber(modid = UnderwaterPlugin.MODID, value = Dist.CLIENT)
public final class AluminumBacktankFirstPersonRenderer {

    private static final ResourceLocation BACKTANK_ARMOR_LOCATION =
        ResourceLocation.fromNamespaceAndPath(UnderwaterPlugin.MODID,
            "textures/models/armor/aluminum_diving_arm.png");

    private static boolean rendererActive = false;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        rendererActive = mc.player != null
            && UnderwaterCreateRegisters.createLoaded()
            && mc.player.getItemBySlot(EquipmentSlot.CHEST)
                .is(UnderwaterCreateRegisters.ALUMINUM_BACKTANK.get());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderPlayerHand(RenderArmEvent event) {
        if (!rendererActive)
            return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        MultiBufferSource buffer = event.getMultiBufferSource();
        if (!(mc.getEntityRenderDispatcher()
            .getRenderer(player) instanceof PlayerRenderer pr))
            return;

        PlayerModel<AbstractClientPlayer> model = pr.getModel();
        model.attackTime = 0.0F;
        model.crouching = false;
        model.swimAmount = 0.0F;
        model.setupAnim(player, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        ModelPart armPart = event.getArm() == HumanoidArm.LEFT ? model.leftSleeve : model.rightSleeve;
        armPart.xRot = 0.0F;
        armPart.render(event.getPoseStack(), buffer.getBuffer(RenderType.entitySolid(BACKTANK_ARMOR_LOCATION)),
            LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        event.setCanceled(true);
    }
}
