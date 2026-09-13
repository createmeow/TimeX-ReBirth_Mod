package com.createmeow.underwaterplugin;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * 客户端初始化（铝背罐渲染，与 Create 原版背罐的双通道渲染保持一致）：
 * <p>
 * 1. Flywheel Visual（正常游戏走这条）：注册 {@code SingleAxisRotatingVisual::backtank}
 *    渲染顶部传动杆的旋转——Create 的 BER 在 Flywheel 启用时直接跳过旋转轴渲染，
 *    漏注册 Visual 会导致铝背罐没有突出的传动杆。
 * <p>
 * 2. BER（{@code BacktankRenderer}，Flywheel 关闭时兜底）：渲染齿轮。
 *    {@code skipVanillaRender(be -> false)} 保证 Flywheel 启用时 BER 仍渲染齿轮
 *    （与 Create 原版 {@code .visual(factory, true)} 的注册语义一致）。
 * <p>
 * 仅在 Create 已加载时执行，方法体内的 Create/Flywheel 类引用惰性解析。
 */
@EventBusSubscriber(modid = UnderwaterPlugin.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class UnderwaterClient {

    private UnderwaterClient() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        if (!UnderwaterCreateRegisters.createLoaded()) return;
        event.enqueueWork(() -> {
            // Flywheel Visual：旋转传动杆（getShaftModel 对非下界合金背罐回退铜色轴模型）
            dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer
                    .builder(UnderwaterCreateRegisters.ALUMINUM_BACKTANK_BE.get())
                    .factory(com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual::backtank)
                    .skipVanillaRender(be -> false)
                    .apply();
        });
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        if (!UnderwaterCreateRegisters.createLoaded()) return;
        event.registerBlockEntityRenderer(
                UnderwaterCreateRegisters.ALUMINUM_BACKTANK_BE.get(),
                com.simibubi.create.content.equipment.armor.BacktankRenderer::new);
    }
}
