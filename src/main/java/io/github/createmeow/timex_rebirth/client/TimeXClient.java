package io.github.createmeow.timex_rebirth.client;

import com.simibubi.create.CreateClient;
import com.simibubi.create.content.decoration.encasing.EncasedCTBehaviour;
import com.simibubi.create.foundation.block.connected.AllCTTypes;
import com.simibubi.create.foundation.block.connected.CTModel;
import com.simibubi.create.foundation.block.connected.CTSpriteShifter;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour;
import io.github.createmeow.timex_rebirth.TimeX;
import io.github.createmeow.timex_rebirth.client.hud.TimeXHudRenderer;
import io.github.createmeow.timex_rebirth.client.key.ModKeys;
import io.github.createmeow.timex_rebirth.heat.FireproofRegistry;
import io.github.createmeow.timex_rebirth.heat.HeatRegistry;
import io.github.createmeow.timex_rebirth.heat.station.HeatStationRegistry;
import io.github.createmeow.timex_rebirth.wheelmenu.WheelMenuRenderer;
import io.github.createmeow.timex_rebirth.food.WastelandFoodRegistry;
import io.github.createmeow.timex_rebirth.wasteland.WastelandRegistry;
import io.github.createmeow.timex_rebirth.wasteland.client.ExplosiveArrowRenderer;
import io.github.createmeow.timex_rebirth.wasteland.client.ThrownFireChargeRenderer;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

@SuppressWarnings("removal")
@EventBusSubscriber(modid = TimeX.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class TimeXClient {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        ModKeys.init();
        registerFireproofCasingCT();
        registerHeatStationCasingCT();
    }

    /**
     * 防火机壳连接纹理：复用 Create 的 CT（Connected Texture）机制。
     * SpriteShift 把 block/fireproof_casing 映射到 128x128 的 _connected 大图，
     * CTModel 根据相邻方块是否同为防火机壳，从大图选取对应的边框组合，
     * 实现与 Create 机壳一致"相邻拼接无缝、外沿出边框"的连接效果。
     * 注意：EncasedCTBehaviour.connectsTo 查的是 CreateClient.CASING_CONNECTIVITY
     * （而非默认"同方块"），必须同时 makeCasing 注册，否则连接判定恒为 false。
     */
    private static void registerFireproofCasingCT() {
        CTSpriteShiftEntry shift = CTSpriteShifter.getCT(
                AllCTTypes.OMNIDIRECTIONAL,
                ResourceLocation.fromNamespaceAndPath(TimeX.MODID, "block/fireproof_casing"),
                ResourceLocation.fromNamespaceAndPath(TimeX.MODID, "block/fireproof_casing_connected"));
        CreateClient.CASING_CONNECTIVITY.makeCasing(FireproofRegistry.FIREPROOF_CASING.get(), shift);
        ConnectedTextureBehaviour behaviour = new EncasedCTBehaviour(shift);
        CreateClient.MODEL_SWAPPER.getCustomBlockModels().register(
                ResourceLocation.fromNamespaceAndPath(TimeX.MODID, "fireproof_casing"),
                model -> new CTModel(model, behaviour));
    }

    /**
     * 热源供应站两个站位方块（加热底座结构件 / 热源发生器结构件）的连接纹理。
     * 与防火机壳同一 CT 机制：相邻同种站位方块无缝拼接，外沿出机器板面倒角/阴影边框。
     * 两个方块使用各自的 SpriteShift，互不连接（EncasedCTBehaviour 校验两侧 shift 相同）。
     */
    private static void registerHeatStationCasingCT() {
        registerCasingCT("heat_base_casing", HeatStationRegistry.HEAT_BASE_CASING.get());
        registerCasingCT("heat_station_casing", HeatStationRegistry.HEAT_STATION_CASING.get());
    }

    private static void registerCasingCT(String blockId, Block block) {
        CTSpriteShiftEntry shift = CTSpriteShifter.getCT(
                AllCTTypes.OMNIDIRECTIONAL,
                ResourceLocation.fromNamespaceAndPath(TimeX.MODID, "block/" + blockId),
                ResourceLocation.fromNamespaceAndPath(TimeX.MODID, "block/" + blockId + "_connected"));
        CreateClient.CASING_CONNECTIVITY.makeCasing(block, shift);
        ConnectedTextureBehaviour behaviour = new EncasedCTBehaviour(shift);
        CreateClient.MODEL_SWAPPER.getCustomBlockModels().register(
                ResourceLocation.fromNamespaceAndPath(TimeX.MODID, blockId),
                model -> new CTModel(model, behaviour));
    }

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(ModKeys.KEY_OPEN_WHEEL_MENU);
        event.register(ModKeys.KEY_CROP_INFO);
    }

    /** 爆炸箭 / 投掷火焰弹实体渲染器注册。 */
    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(WastelandRegistry.EXPLOSIVE_ARROW_ENTITY.get(), ExplosiveArrowRenderer::new);
        event.registerEntityRenderer(WastelandRegistry.THROWN_FIRE_CHARGE_ENTITY.get(), ThrownFireChargeRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(TimeX.rl("wheel_menu"), WheelMenuRenderer.OVERLAY);
        // 原生 HUD（复刻 FancyMenu spiffy_overlay 布局）
        TimeXHudRenderer.INSTANCE.register(event);
    }

    /** 热源供应站界面注册。 */
    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(HeatStationRegistry.HEAT_STATION_MENU.get(), HeatStationScreen::new);
        event.register(HeatStationRegistry.HEAT_MODULE_SLOT_MENU.get(), HeatModuleSlotScreen::new);
    }

    /** 热流流体客户端渲染：橙色水纹理（阶段1占位，后续替换为专用纹理）。 */
    @SubscribeEvent
    public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public int getTintColor() {
                return 0xFFFF8A3D;
            }

            @Override
            public ResourceLocation getStillTexture() {
                return ResourceLocation.withDefaultNamespace("block/water_still");
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return ResourceLocation.withDefaultNamespace("block/water_flow");
            }
        }, HeatRegistry.HEAT_FLUX_TYPE);

        // 草药茶流体渲染：与热流同机制——原版水纹理换色（#969a72 拉高亮度 → #D6DD92 亮黄绿）
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public int getTintColor() {
                return 0xFFD6DD92;
            }

            @Override
            public ResourceLocation getStillTexture() {
                return ResourceLocation.withDefaultNamespace("block/water_still");
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return ResourceLocation.withDefaultNamespace("block/water_flow");
            }
        }, WastelandFoodRegistry.HERBAL_TEA_FLUID_TYPE);
    }
}