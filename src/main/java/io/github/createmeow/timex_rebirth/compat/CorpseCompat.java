package io.github.createmeow.timex_rebirth.compat;

import io.github.createmeow.timex_rebirth.TimeX;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Integration with Corpse mod (de.maxhenkel.corpse).
 * Provides the ability to open death history from the wheel menu.
 */
public class CorpseCompat {
    private static final String CORPSE_MOD_ID = "corpse";
    private static boolean checked = false;
    private static boolean loaded = false;

    /**
     * Check if the Corpse mod is loaded.
     */
    public static boolean isLoaded() {
        if (!checked) {
            loaded = ModList.get().isLoaded(CORPSE_MOD_ID);
            checked = true;
        }
        return loaded;
    }

    /**
     * Open the death history screen, simulating the U key press from the Corpse mod.
     * Uses reflection to avoid compile-time dependency.
     */
    public static void openDeathHistory() {
        if (!isLoaded()) {
            TimeX.LOGGER.warn("Corpse mod not loaded, cannot open death history");
            return;
        }
        try {
            // The Corpse mod's U key sends a MessageRequestDeathHistory packet to the server,
            // which returns a MessageOpenHistory with death data.
            // We use reflection to avoid compile-time dependency on the Corpse mod jar.
            Class<?> requestClass = Class.forName("de.maxhenkel.corpse.net.MessageRequestDeathHistory");
            Object request = requestClass.getDeclaredConstructor().newInstance();
            PacketDistributor.sendToServer((CustomPacketPayload) request);
            TimeX.LOGGER.debug("Sent death history request to server");
        } catch (ClassNotFoundException e) {
            TimeX.LOGGER.error("Corpse mod classes not found, but mod claims to be loaded");
            loaded = false;
        } catch (Exception e) {
            TimeX.LOGGER.error("Failed to open death history via Corpse mod", e);
        }
    }
}