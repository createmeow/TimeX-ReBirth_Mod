package io.github.createmeow.timex_rebirth.wheelmenu;

import io.github.createmeow.timex_rebirth.TimeX;
import io.github.createmeow.timex_rebirth.client.TechTreeScreen;
import io.github.createmeow.timex_rebirth.client.WeatherForecastScreen;
import io.github.createmeow.timex_rebirth.compat.CorpseCompat;
import io.github.createmeow.timex_rebirth.compat.XaeroCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.lang.reflect.Constructor;

/**
 * 轮盘选项注册（事件驱动）。
 * 通过 WheelMenuSelectionRegisterEvent + SelectionBuilder 注册所有选项。
 */
@EventBusSubscriber(modid = TimeX.MODID, value = Dist.CLIENT)
public class WheelActions {

    @SubscribeEvent
    public static void onRegisterSelections(WheelMenuSelectionRegisterEvent event) {
        // --- Close button ---
        SelectionBuilder.create()
                .message(Component.translatable("gui." + TimeX.MODID + ".wheel_menu.close"))
                .icon(Items.BARRIER)
                .selected(WheelMenuRenderer::close)
                .register(event, TimeX.rl("close"));

        // --- Death History (Corpse mod integration) ---
        if (CorpseCompat.isLoaded()) {
            SelectionBuilder.create()
                    .message(Component.translatable("gui." + TimeX.MODID + ".wheel_menu.death_history"))
                    .icon(Items.SKELETON_SKULL)
                    .selected(CorpseCompat::openDeathHistory)
                    .register(event, TimeX.rl("death_history"));
        }

        // --- Temporary Waypoint (Xaero Minimap integration) ---
        if (XaeroCompat.isLoaded()) {
            SelectionBuilder.create()
                    .message(Component.translatable("gui." + TimeX.MODID + ".wheel_menu.temp_waypoint"))
                    .icon(Items.COMPASS)
                    .selected(XaeroCompat::createTemporaryWaypoint)
                    .register(event, TimeX.rl("temp_waypoint"));
        }

        // --- Open Map (World Map integration) ---
        if (isWorldMapLoaded()) {
            SelectionBuilder.create()
                    .message(Component.translatable("gui." + TimeX.MODID + ".wheel_menu.open_map"))
                    .icon(Items.FILLED_MAP)
                    .selected(WheelActions::openWorldMap)
                    .register(event, TimeX.rl("open_map"));
        }

        // --- Weather Forecast (本模组天气预报) ---
        SelectionBuilder.create()
                .message(Component.translatable("gui." + TimeX.MODID + ".wheel_menu.weather_forecast"))
                .icon(Items.CLOCK)
                .selected(() -> Minecraft.getInstance().setScreen(new WeatherForecastScreen()))
                .register(event, TimeX.rl("weather_forecast"));

        // --- Tech Tree (本模组科技树查看) ---
        SelectionBuilder.create()
                .message(Component.translatable("gui." + TimeX.MODID + ".wheel_menu.tech_tree"))
                .icon(Items.BOOK)
                .selected(() -> Minecraft.getInstance().setScreen(new TechTreeScreen()))
                .register(event, TimeX.rl("tech_tree"));

        TimeX.LOGGER.info("Registered {} wheel menu selections", event.getRegisteredCount());
    }

    private static boolean isWorldMapLoaded() {
        try {
            Class.forName("xaero.map.WorldMap");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Open the Xaero World Map GUI.
     * Creates a GuiMap directly, mirroring ControlsHandler.keyDown():
     *   mc.setScreen(new GuiMap(null, null, mapProcessor, mc.getCameraEntity()));
     */
    private static void openWorldMap() {
        try {
            Minecraft mc = Minecraft.getInstance();

            // Get WorldMapSession.getCurrentSession()
            Class<?> sessionClass = Class.forName("xaero.map.WorldMapSession");
            Object session = sessionClass.getMethod("getCurrentSession").invoke(null);
            if (session == null) {
                TimeX.LOGGER.error("WorldMapSession is null, cannot open world map");
                return;
            }

            // Get MapProcessor from session
            Object mapProcessor = sessionClass.getMethod("getMapProcessor").invoke(session);
            if (mapProcessor == null) {
                TimeX.LOGGER.error("MapProcessor is null, cannot open world map");
                return;
            }

            // Create GuiMap(parent, escape, mapProcessor, player)
            Class<?> guiMapClass = Class.forName("xaero.map.gui.GuiMap");
            Class<?> mapProcessorClass = Class.forName("xaero.map.MapProcessor");
            Constructor<?> constructor = guiMapClass.getDeclaredConstructor(
                    Screen.class, Screen.class, mapProcessorClass, Entity.class);
            constructor.setAccessible(true);
            Screen guiMap = (Screen) constructor.newInstance(null, null, mapProcessor, mc.getCameraEntity());

            // setScreen replaces the WheelMenuScreen; its removed() cleans up wheel state
            mc.setScreen(guiMap);
        } catch (Exception e) {
            TimeX.LOGGER.error("Failed to open Xaero World Map", e);
        }
    }
}
