package com.createmeow.cm_plugins;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = createmeowsplugins.MODID, value = Dist.CLIENT)
public class CombatClientHandler {
    private static final ResourceLocation COMBAT_ICON =
            ResourceLocation.fromNamespaceAndPath(createmeowsplugins.MODID, "textures/gui/combat_icon.png");

    private static final int ICON_SIZE = 9;

    // ========== Register HUD Layer ==========
    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(
                VanillaGuiLayers.FOOD_LEVEL,
                ResourceLocation.fromNamespaceAndPath(createmeowsplugins.MODID, "combat_indicator"),
                COMBAT_LAYER);
    }

    private static final LayeredDraw.Layer COMBAT_LAYER = (guiGraphics, deltaTracker) -> {
        int ticks = createmeowsplugins.clientCombatTicks;
        if (ticks <= 0) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        // Position: right of food bar, same Y
        int x = screenWidth / 2 + 93;
        int y = screenHeight - 39;

        // Pulsing effect
        float pulse = (float) Math.sin((System.currentTimeMillis() % 1000) / 1000.0 * Math.PI * 2) * 0.2f + 0.8f;
        guiGraphics.setColor(1.0f, 1.0f, 1.0f, pulse);

        // Draw icon
        guiGraphics.blit(COMBAT_ICON, x, y, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);

        // Draw seconds text to the right of the icon
        int seconds = (ticks + 19) / 20; // Ceiling division: round up
        String text = String.valueOf(seconds);
        int textX = x + ICON_SIZE + 2;
        int textY = y;
        guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        guiGraphics.drawString(mc.font, Component.literal("§c" + text), textX, textY, 0xFFFFFFFF);
    };
}