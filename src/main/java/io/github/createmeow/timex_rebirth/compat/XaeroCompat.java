package io.github.createmeow.timex_rebirth.compat;

import io.github.createmeow.timex_rebirth.TimeX;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.ModList;

/**
 * Integration with Xaero's Minimap mod.
 * Provides the ability to create temporary waypoints from the wheel menu.
 */
public class XaeroCompat {
    private static final String XAERO_MINIMAP_ID = "xaerominimap";
    private static boolean checked = false;
    private static boolean loaded = false;

    /**
     * Check if Xaero's Minimap mod is loaded.
     */
    public static boolean isLoaded() {
        if (!checked) {
            loaded = ModList.get().isLoaded(XAERO_MINIMAP_ID);
            checked = true;
        }
        return loaded;
    }

    /**
     * Create a temporary waypoint at the player's current position.
     * Uses reflection to avoid compile-time dependency on Xaero's Minimap jar.
     */
    public static void createTemporaryWaypoint() {
        if (!isLoaded()) {
            TimeX.LOGGER.warn("Xaero Minimap mod not loaded, cannot create temporary waypoint");
            return;
        }
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) return;

            int playerX = (int) Math.floor(mc.player.getX());
            int playerY = (int) Math.floor(mc.player.getY() + 0.0625);
            int playerZ = (int) Math.floor(mc.player.getZ());

            // Access via: BuiltInHudModules.MINIMAP.getCurrentSession() -> MinimapSession
            // -> getWaypointSession() -> getTemporaryHandler() -> createTemporaryWaypoint(...)
            Class<?> builtInModulesClass = Class.forName("xaero.hud.minimap.BuiltInHudModules");
            Object minmapModule = builtInModulesClass.getField("MINIMAP").get(null);

            // getCurrentSession() returns HudModuleSession
            Object session = minmapModule.getClass().getMethod("getCurrentSession").invoke(minmapModule);
            if (session == null) {
                TimeX.LOGGER.warn("Xaero Minimap session not available");
                return;
            }

            // Get WaypointSession from MinimapSession
            Object waypointSession = session.getClass().getMethod("getWaypointSession").invoke(session);
            if (waypointSession == null) {
                TimeX.LOGGER.warn("Xaero WaypointSession not available");
                return;
            }

            // Get TemporaryWaypointHandler
            Object tempHandler = waypointSession.getClass().getMethod("getTemporaryHandler").invoke(waypointSession);
            if (tempHandler == null) {
                TimeX.LOGGER.warn("Xaero TemporaryWaypointHandler not available");
                return;
            }

            // Get current world
            Object worldManager = session.getClass().getMethod("getWorldManager").invoke(session);
            Object currentWorld = worldManager.getClass().getMethod("getCurrentWorld").invoke(worldManager);

            // Create the temporary waypoint
            tempHandler.getClass().getMethod("createTemporaryWaypoint",
                    Class.forName("xaero.hud.minimap.world.MinimapWorld"),
                    int.class, int.class, int.class)
                    .invoke(tempHandler, currentWorld, playerX, playerY, playerZ);

            TimeX.LOGGER.debug("Created temporary waypoint at ({}, {}, {})", playerX, playerY, playerZ);
        } catch (ClassNotFoundException e) {
            TimeX.LOGGER.error("Xaero Minimap classes not found, but mod claims to be loaded");
            loaded = false;
        } catch (NoSuchFieldException e) {
            // Fallback: try alternative approach via XaeroMinimap.instance
            try {
                createTemporaryWaypointFallback();
            } catch (Exception ex) {
                TimeX.LOGGER.error("Failed to create temporary waypoint (fallback also failed)", ex);
            }
        } catch (Exception e) {
            TimeX.LOGGER.error("Failed to create temporary waypoint", e);
        }
    }

    /**
     * Fallback approach using XaeroMinimap.instance.
     */
    private static void createTemporaryWaypointFallback() throws Exception {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        int playerX = (int) Math.floor(mc.player.getX());
        int playerY = (int) Math.floor(mc.player.getY() + 0.0625);
        int playerZ = (int) Math.floor(mc.player.getZ());

        // Try XaeroMinimap.instance
        Class<?> xaeroMinimapClass = Class.forName("xaero.minimap.XaeroMinimap");
        Object instance = xaeroMinimapClass.getField("instance").get(null);

        // Get session
        Object session = instance.getClass().getMethod("getSession").invoke(instance);

        // Get waypoint session
        Class<?> builtInModulesClass = Class.forName("xaero.hud.minimap.BuiltInHudModules");
        Object minmapModule = builtInModulesClass.getField("MINIMAP").get(null);
        Object minimapSession = minmapModule.getClass().getMethod("getCurrentSession").invoke(minmapModule);
        Object waypointSession = minimapSession.getClass().getMethod("getWaypointSession").invoke(minimapSession);
        Object tempHandler = waypointSession.getClass().getMethod("getTemporaryHandler").invoke(waypointSession);

        // Get current world
        Object worldManager = minimapSession.getClass().getMethod("getWorldManager").invoke(minimapSession);
        Object currentWorld = worldManager.getClass().getMethod("getCurrentWorld").invoke(worldManager);

        tempHandler.getClass().getMethod("createTemporaryWaypoint",
                Class.forName("xaero.hud.minimap.world.MinimapWorld"),
                int.class, int.class, int.class)
                .invoke(tempHandler, currentWorld, playerX, playerY, playerZ);
    }
}