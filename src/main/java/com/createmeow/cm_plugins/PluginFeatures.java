package com.createmeow.cm_plugins;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Zombie;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Random;

public class PluginFeatures {
    private static final Random RANDOM = new Random();

    // ========== Random Zombie Health ==========
    @SubscribeEvent(priority = EventPriority.LOW)
    public void onZombieSpawn(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof Zombie zombie)) return;

        MobSpawnType spawnReason = MobSpawnType.NATURAL;
        if (zombie.getPersistentData().contains("SpawnReason")) {
            try {
                spawnReason = MobSpawnType.valueOf(zombie.getPersistentData().getString("SpawnReason"));
            } catch (IllegalArgumentException ignored) {}
        }

        if (!shouldProcessZombie(spawnReason)) return;

        int min = Config.ZOMBIE_MIN_HEALTH.getAsInt();
        int max = Config.ZOMBIE_MAX_HEALTH.getAsInt();
        if (max <= min) max = min + 1;
        int health = min + RANDOM.nextInt(max - min);

        var attr = zombie.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
        if (attr != null) {
            attr.setBaseValue(health);
            zombie.setHealth(health);
        }

        if (Config.ZOMBIE_DEBUG.getAsBoolean()) {
            createmeowsplugins.LOGGER.info("[随机僵尸血量] 设置僵尸血量: {} (位置: {},{},{})",
                    health, zombie.blockPosition().getX(), zombie.blockPosition().getY(), zombie.blockPosition().getZ());
        }
    }

    private boolean shouldProcessZombie(MobSpawnType reason) {
        return switch (reason) {
            case NATURAL -> true;
            case SPAWNER -> Config.ZOMBIE_INCLUDE_SPAWNERS.getAsBoolean();
            case SPAWN_EGG -> Config.ZOMBIE_INCLUDE_EGGS.getAsBoolean();
            case COMMAND -> Config.ZOMBIE_INCLUDE_COMMANDS.getAsBoolean();
            default -> false;
        };
    }

    // ========== No Mob Spawn ==========
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (event.getLevel() == null || event.getLevel().isClientSide()) return;

        var entity = event.getEntity();
        String entityName = EntityType.getKey(entity.getType()).getPath().toUpperCase();

        if (Config.getBannedMobs().contains(entityName)) {
            event.setSpawnCancelled(true);

            if (Config.NOMOBS_DEBUG.getAsBoolean()) {
                var pos = entity.blockPosition();
                createmeowsplugins.LOGGER.info("[NoMobSpawn] 阻止了 {} 在 ({},{},{}) 生成",
                        entityName, pos.getX(), pos.getY(), pos.getZ());
            }
        }
    }

    // ========== Spatial Inventory: Player Data Load/Save ==========
    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            SpatialInventoryManager.loadPlayerData(serverPlayer);
            CombatStateManager.onPlayerLogin(serverPlayer);

            // 首次同步「自动合并钱币」开关状态到客户端
            PacketDistributor.sendToPlayer(serverPlayer, new AutoCoinStatePayload(
                    PlayerCoinConsolidationHandler.isAutoConsolidate(serverPlayer.getUUID())));

            // 登录时强制同步 NumismaticOverhaul 货币：用 setValue 重置当前值触发
            // 服务端→客户端同步，等价于「+1 铜 再 -1 铜」但不刷提示、不改余额。
            if (NumismaticHelper.isAvailable()) {
                NumismaticHelper.setValue(serverPlayer, NumismaticHelper.getValue(serverPlayer));
            }
        }
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            SpatialInventoryManager.savePlayerData(serverPlayer);
            SpatialInventoryManager.cleanupPlayer(serverPlayer);
        }
    }

    // 死亡后重生时客户端货币值可能显示为 0（服务端余额仍保留），需重新同步一次。
    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer && NumismaticHelper.isAvailable()) {
            NumismaticHelper.setValue(serverPlayer, NumismaticHelper.getValue(serverPlayer));
        }
    }
}