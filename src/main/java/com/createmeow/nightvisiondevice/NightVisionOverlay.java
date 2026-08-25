package com.createmeow.nightvisiondevice;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

public class NightVisionOverlay {

    static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath(createmeow.MODID, "night_vision_overlay"),
                NightVisionOverlay::render);
    }

    private static void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null)
            return;

        ItemStack helmet = mc.player.getItemBySlot(EquipmentSlot.HEAD);
        if (!(helmet.getItem() instanceof NightVisionDeviceItem))
            return;

        Boolean noGreen = helmet.get(NVItems.NO_GREEN.get());
        if (noGreen != null && noGreen)
            return;

        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();

        guiGraphics.fill(0, 0, width, height, 0x3300FF00);
    }
}