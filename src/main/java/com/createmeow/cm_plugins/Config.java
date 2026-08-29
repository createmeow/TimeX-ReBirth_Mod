package com.createmeow.cm_plugins;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = createmeowsplugins.MODID)
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // ========== Random Zombie Health ==========
    public static final ModConfigSpec.IntValue ZOMBIE_MIN_HEALTH = BUILDER
            .comment("Random zombie health: minimum health value")
            .defineInRange("zombieMinHealth", 25, 1, 1024);

    public static final ModConfigSpec.IntValue ZOMBIE_MAX_HEALTH = BUILDER
            .comment("Random zombie health: maximum health value")
            .defineInRange("zombieMaxHealth", 46, 2, 1024);

    public static final ModConfigSpec.BooleanValue ZOMBIE_INCLUDE_SPAWNERS = BUILDER
            .comment("Random zombie health: apply to spawner-spawned zombies")
            .define("zombieIncludeSpawners", true);

    public static final ModConfigSpec.BooleanValue ZOMBIE_INCLUDE_EGGS = BUILDER
            .comment("Random zombie health: apply to spawn egg-spawned zombies")
            .define("zombieIncludeEggs", true);

    public static final ModConfigSpec.BooleanValue ZOMBIE_INCLUDE_COMMANDS = BUILDER
            .comment("Random zombie health: apply to command-spawned zombies")
            .define("zombieIncludeCommands", true);

    public static final ModConfigSpec.BooleanValue ZOMBIE_DEBUG = BUILDER
            .comment("Random zombie health: enable debug logging")
            .define("zombieDebug", false);

    // ========== No Mob Spawn ==========
    private static final ModConfigSpec.ConfigValue<List<? extends String>> BANNED_MOBS = BUILDER
            .comment("No mob spawn: list of banned mob type names (e.g. PHANTOM, SKELETON, CREEPER)")
            .defineListAllowEmpty("bannedMobs",
                    List.of("PHANTOM", "SKELETON", "CREEPER", "SPIDER", "ENDERMAN", "WARDEN", "STRAY", "BOGGED", "CAVE_SPIDER", "HUSK"),
                    () -> "", o -> o instanceof String);

    public static final ModConfigSpec.BooleanValue NOMOBS_DEBUG = BUILDER
            .comment("No mob spawn: enable debug logging")
            .define("nomobspawnDebug", false);

    // ========== Spatial Inventory ==========
    public static final ModConfigSpec.IntValue SPATIAL_INITIAL_SLOTS = BUILDER
            .comment("Spatial inventory: initial unlocked slots")
            .defineInRange("spatialInitialSlots", 9, 1, 54);

    public static final ModConfigSpec.IntValue SPATIAL_MAX_SLOTS = BUILDER
            .comment("Spatial inventory: maximum unlockable slots")
            .defineInRange("spatialMaxSlots", 54, 1, 54);

    public static final ModConfigSpec.IntValue SPATIAL_SLOT_COST = BUILDER
            .comment("Spatial inventory: cost per slot in numismaticoverhaul currency (bronze coins)")
            .defineInRange("spatialSlotCost", 5000, 1, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue SPATIAL_CONFIRMATION_TIMEOUT = BUILDER
            .comment("Spatial inventory: purchase confirmation timeout in seconds")
            .defineInRange("spatialConfirmationTimeout", 60, 5, 600);

    // ========== Combat State ==========
    public static final ModConfigSpec.BooleanValue COMBAT_TRIGGER_PVP = BUILDER
            .comment("Combat state: trigger when player attacks another player")
            .define("combatTriggerPvP", true);

    public static final ModConfigSpec.BooleanValue COMBAT_TRIGGER_MOB_DAMAGE = BUILDER
            .comment("Combat state: trigger when player is attacked by mobs")
            .define("combatTriggerMobDamage", true);

    public static final ModConfigSpec.BooleanValue COMBAT_TRIGGER_PLAYER_ATTACK = BUILDER
            .comment("Combat state: trigger when player attacks any entity (mob or player)")
            .define("combatTriggerPlayerAttack", true);

    public static final ModConfigSpec.IntValue COMBAT_DURATION = BUILDER
            .comment("Combat state: duration in seconds")
            .defineInRange("combatDuration", 30, 1, 600);

    public static final ModConfigSpec.BooleanValue COMBAT_PUNISH_DEATH = BUILDER
            .comment("Combat state: kill player on combat logout")
            .define("combatPunishDeath", true);

    public static final ModConfigSpec.ConfigValue<String> COMBAT_PUNISH_COMMAND = BUILDER
            .comment("Combat state: command to run on combat logout, {player} will be replaced with player name, leave empty to disable")
            .define("combatPunishCommand", "");

    public static final ModConfigSpec.ConfigValue<String> COMBAT_DEATH_MESSAGE = BUILDER
            .comment("Combat state: custom death message on combat logout, {player} will be replaced with player name")
            .define("combatDeathMessage", " 在战斗中逃跑并付出了生命代价");

    // ========== Legacy Plugins ==========
    public static final ModConfigSpec.ConfigValue<String> LITE_ITEM_SHOW_KEYWORD = BUILDER
            .comment("LiteItemShow: keyword that triggers item display in chat (set empty to disable)")
            .define("liteItemShowKeyword", "[item]");

    static final ModConfigSpec SPEC = BUILDER.build();

    private static Set<String> cachedBannedMobs = Set.of();

    public static Set<String> getBannedMobs() {
        return cachedBannedMobs;
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        cachedBannedMobs = BANNED_MOBS.get().stream()
                .map(s -> s.trim().toUpperCase())
                .collect(Collectors.toSet());
    }
}