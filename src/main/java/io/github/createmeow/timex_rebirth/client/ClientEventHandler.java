package io.github.createmeow.timex_rebirth.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.createmeow.timex_rebirth.TimeX;
import io.github.createmeow.timex_rebirth.wheelmenu.WheelMenuRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = TimeX.MODID, value = Dist.CLIENT)
public class ClientEventHandler {

    private static boolean initialized = false;
    /** Tracks ALT key state for rising-edge detection (independent of KeyMapping). */
    private static boolean wasAltDown = false;

    @SubscribeEvent
    public static void onPlayerLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        if (!initialized) {
            WheelMenuRenderer.initSelections();
            initialized = true;
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        // Detect ALT key press via rising-edge on InputConstants.
        // This bypasses KeyMapping entirely, so it works even when a Screen is open
        // (KeyMapping.isDown/consumeClick may not update while a Screen has focus).
        long window = net.minecraft.client.Minecraft.getInstance().getWindow().getWindow();
        boolean altDown = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_ALT)
                        || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_ALT);

        if (altDown && !wasAltDown) {
            // ALT just pressed — open the wheel menu
            if (!WheelMenuRenderer.isOpen()) {
                WheelMenuRenderer.open();
            }
        }
        wasAltDown = altDown;

        WheelMenuRenderer.tick();
    }
}
