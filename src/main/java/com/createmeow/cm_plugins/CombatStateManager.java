package com.createmeow.cm_plugins;

import com.mojang.brigadier.ParseResults;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Set;

public class CombatStateManager {
    private static final String TAG_IN_COMBAT = "cm_inCombat";
    private static final String TAG_COMBAT_TIME = "cm_combatTime";
    private static final String TAG_COMBAT_SOURCE = "cm_combatSource";

    private static final String SOURCE_ATTACK = "attack";
    private static final String SOURCE_INTERACT = "interact";

    // 已知的存储方块注册名（防止 instanceof Container 漏检）
    private static final Set<String> STORAGE_BLOCK_IDS = Set.of(
            // 原版
            "minecraft:chest", "minecraft:trapped_chest",
            "minecraft:barrel",
            "minecraft:furnace", "minecraft:blast_furnace", "minecraft:smoker",
            "minecraft:dispenser", "minecraft:dropper",
            "minecraft:hopper",
            "minecraft:brewing_stand",
            "minecraft:shulker_box",
            "minecraft:white_shulker_box", "minecraft:orange_shulker_box", "minecraft:magenta_shulker_box",
            "minecraft:light_blue_shulker_box", "minecraft:yellow_shulker_box", "minecraft:lime_shulker_box",
            "minecraft:pink_shulker_box", "minecraft:gray_shulker_box", "minecraft:light_gray_shulker_box",
            "minecraft:cyan_shulker_box", "minecraft:purple_shulker_box", "minecraft:blue_shulker_box",
            "minecraft:brown_shulker_box", "minecraft:green_shulker_box", "minecraft:red_shulker_box",
            "minecraft:black_shulker_box"
    );

    // 已知的可打开存储的物品注册名（手持右键）
    private static final Set<String> STORAGE_ITEM_IDS = Set.of(
            "minecraft:shulker_box",
            "minecraft:white_shulker_box", "minecraft:orange_shulker_box", "minecraft:magenta_shulker_box",
            "minecraft:light_blue_shulker_box", "minecraft:yellow_shulker_box", "minecraft:lime_shulker_box",
            "minecraft:pink_shulker_box", "minecraft:gray_shulker_box", "minecraft:light_gray_shulker_box",
            "minecraft:cyan_shulker_box", "minecraft:purple_shulker_box", "minecraft:blue_shulker_box",
            "minecraft:brown_shulker_box", "minecraft:green_shulker_box", "minecraft:red_shulker_box",
            "minecraft:black_shulker_box"
    );

    // 空投方块类名（白名单，打开会进入战斗状态）
    private static final String AIRDROP_BLOCK_ENTITY_CLASS = "com.createmeow.airdrop.block.AirDropBlockEntity";

    private static int getDurationTicks() {
        return Config.COMBAT_DURATION.getAsInt() * 20;
    }

    private static boolean isAirdropBlockEntity(BlockEntity be) {
        return be != null && AIRDROP_BLOCK_ENTITY_CLASS.equals(be.getClass().getName());
    }

    private static boolean isCorpseMenu(ResourceLocation menuId) {
        return menuId != null && "corpse".equals(menuId.getNamespace());
    }

    private static void sendCombatBlockMessage(ServerPlayer player) {
        player.sendSystemMessage(Component.literal("§c⚔ 战斗中无法打开容器！"), true);
    }

    // ========== Combat Detection (from attack/damage) ==========
    @SubscribeEvent
    public void onLivingDamage(LivingDamageEvent.Post event) {
        if (event.getEntity().level().isClientSide()) return;

        LivingEntity target = event.getEntity();
        LivingEntity attacker = event.getSource().getDirectEntity() instanceof LivingEntity le ? le :
                event.getSource().getEntity() instanceof LivingEntity le2 ? le2 : null;

        if (attacker == null || attacker == target) return;

        boolean isPvP = attacker instanceof ServerPlayer && target instanceof ServerPlayer;
        boolean isMobAttack = !(attacker instanceof ServerPlayer) && target instanceof ServerPlayer;
        boolean isPlayerAttack = attacker instanceof ServerPlayer;
        float damage = event.getNewDamage();

        // 玩家攻击（伤害 > 2 才进入战斗状态）：
        // - 攻击玩家 → 25s；攻击怪物（Monster）→ 10s；攻击友好/中立生物 → 不进入战斗状态
        if (isPlayerAttack && damage > 2) {
            ServerPlayer attackerPlayer = (ServerPlayer) attacker;
            if (isPvP && Config.COMBAT_TRIGGER_PVP.getAsBoolean()) {
                setCombat(attackerPlayer, 25);
            } else if (target instanceof Monster && Config.COMBAT_TRIGGER_PLAYER_ATTACK.getAsBoolean()) {
                setCombat(attackerPlayer, 10);
            }
        }

        // PvP: target enters combat 25s（伤害 > 2）
        if (isPvP && damage > 2 && Config.COMBAT_TRIGGER_PVP.getAsBoolean()) {
            ServerPlayer targetPlayer = (ServerPlayer) target;
            setCombat(targetPlayer, 25);
            targetPlayer.getCooldowns().addCooldown(Items.ENDER_PEARL, getDurationTicks());
        }
        // Mob attacking player: target enters combat 10s
        else if (isMobAttack && Config.COMBAT_TRIGGER_MOB_DAMAGE.getAsBoolean()) {
            ServerPlayer targetPlayer = (ServerPlayer) target;
            setCombat(targetPlayer, 10);
            targetPlayer.getCooldowns().addCooldown(Items.ENDER_PEARL, getDurationTicks());
        }
    }

    // ========== Combat Detection (from opening airdrop/corpse) ==========
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onContainerOpen(PlayerContainerEvent.Open event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // Check airdrop block by container class name (no hard dependency)
        boolean isAirdrop = false;
        if (event.getContainer() instanceof ChestMenu chestMenu) {
            var container = chestMenu.getContainer();
            if (isAirdropBlockEntity(container instanceof BlockEntity be ? be : null)) {
                setCombatInteract(player, 300);
                isAirdrop = true;
            }
        }

        // 战斗中禁止打开存储容器（兜底拦截，遗体/空投排除在外）
        if (isInCombat(player)) {
            ResourceLocation menuId = BuiltInRegistries.MENU.getKey(event.getContainer().getType());
            if (!isCorpseMenu(menuId) && !isAirdrop) {
                player.closeContainer();
                sendCombatBlockMessage(player);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(event.getTarget().getType());

        // Check corpse mod entity by registry name: corpse:corpse（进入 150s 战斗状态）
        if (entityId != null && "corpse".equals(entityId.getNamespace()) && "corpse".equals(entityId.getPath())) {
            setCombatInteract(player, 150);
            return;
        }

        // 战斗中禁止交互有库存的实体
        // 1) 直接实现 Container 接口的实体
        // 2) 有箱子的马（AbstractChestedHorse，带 ChestCarrier 接口但可能没直接实现 Container）
        // 3) 村民（AbstractVillager）—— 交易也禁止
        if (isInCombat(player)) {
            boolean hasInventory = event.getTarget() instanceof net.minecraft.world.Container
                    || event.getTarget() instanceof AbstractChestedHorse
                    || event.getTarget() instanceof AbstractVillager;
            if (hasInventory) {
                event.setCanceled(true);
                sendCombatBlockMessage(player);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!isInCombat(player)) return;

        BlockEntity be = event.getLevel().getBlockEntity(event.getPos());

        // 空投排除（打开空投会使玩家进入战斗状态）
        if (isAirdropBlockEntity(be)) return;

        // 双重判断：instanceof Container 或 方块注册名在已知存储列表中
        boolean isStorage = be instanceof net.minecraft.world.Container;
        if (!isStorage) {
            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(event.getLevel().getBlockState(event.getPos()).getBlock());
            if (blockId != null && STORAGE_BLOCK_IDS.contains(blockId.toString())) {
                isStorage = true;
            }
        }

        if (isStorage) {
            event.setCanceled(true);
            sendCombatBlockMessage(player);
        }
    }

    // 拦截手持潜影盒等物品右键（不需要点方块）
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!isInCombat(player)) return;

        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;

        Item item = stack.getItem();
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);

        // 1) 按注册名判断（已知的潜影盒等）
        // 2) BlockItem 且方块是 ShulkerBoxBlock
        boolean isStorageItem = itemId != null && STORAGE_ITEM_IDS.contains(itemId.toString());
        if (!isStorageItem && item instanceof BlockItem blockItem) {
            isStorageItem = blockItem.getBlock() instanceof ShulkerBoxBlock;
        }

        if (isStorageItem) {
            event.setCanceled(true);
            sendCombatBlockMessage(player);
        }
    }

    // ========== Tick Timer ==========
    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!isInCombat(player)) continue;

            int time = getCombatTime(player);
            if (time > 0) {
                setCombatTime(player, time - 1);
            } else {
                endCombat(player);
            }
        }
    }

    // ========== Death Handling ==========
    @SubscribeEvent
    public void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && isInCombat(player)) {
            endCombat(player);
        }
    }

    // ========== Disconnect Handling ==========
    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!isInCombat(player)) return;

        MinecraftServer server = player.getServer();
        if (server == null) return;

        // Execute punish command if configured
        String punishCommand = Config.COMBAT_PUNISH_COMMAND.get();
        if (punishCommand != null && !punishCommand.trim().isEmpty()) {
            String cmd = punishCommand.replace("{player}", player.getName().getString());
            try {
                server.getCommands().performPrefixedCommand(
                        server.createCommandSourceStack().withPermission(4), cmd);
            } catch (Exception e) {
                createmeowsplugins.LOGGER.error("[CombatLog] Failed to execute punish command: {}", cmd, e);
            }
        }

        // Kill player if configured
        if (Config.COMBAT_PUNISH_DEATH.getAsBoolean()) {
            player.hurt(player.damageSources().fellOutOfWorld(), Float.MAX_VALUE);

            // Broadcast custom death message
            String deathMsg = Config.COMBAT_DEATH_MESSAGE.get()
                    .replace("{player}", player.getDisplayName().getString());
            server.getPlayerList().broadcastSystemMessage(
                    Component.literal("§c" + deathMsg), false);
        }
    }

    // ========== Command Blocking ==========
    @SubscribeEvent
    public void onCommand(CommandEvent event) {
        ParseResults<CommandSourceStack> parseResults = event.getParseResults();
        CommandSourceStack source = parseResults.getContext().getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) return;
        if (!isInCombat(player)) return;

        String fullCommand = parseResults.getReader().getString();
        String[] parts = fullCommand.split("\\s+");
        String cmdName = parts[0].toLowerCase();

        if (!cmdName.equals("msg") && !cmdName.equals("w")
                && !cmdName.equals("tell") && !cmdName.equals("r")
                && !cmdName.equals("combatmode")) {
            event.setCanceled(true);
            player.sendSystemMessage(
                    Component.literal("§c⚔ 战斗中无法使用此命令！"), true);
        }
    }

    // ========== Helper Methods ==========

    /** Called from attack/damage — does NOT override "interact" source combat */
    public static void setCombat(ServerPlayer player, int seconds) {
        // Don't override special combat (from airdrop/corpse interaction)
        if (SOURCE_INTERACT.equals(player.getPersistentData().getString(TAG_COMBAT_SOURCE))) {
            return;
        }
        player.getPersistentData().putBoolean(TAG_IN_COMBAT, true);
        player.getPersistentData().putInt(TAG_COMBAT_TIME, seconds * 20);
        player.getPersistentData().putString(TAG_COMBAT_SOURCE, SOURCE_ATTACK);
        syncToClient(player);
    }

    /** Called from airdrop/corpse interaction — always overrides with given seconds */
    public static void setCombatInteract(ServerPlayer player, int seconds) {
        player.getPersistentData().putBoolean(TAG_IN_COMBAT, true);
        player.getPersistentData().putInt(TAG_COMBAT_TIME, seconds * 20);
        player.getPersistentData().putString(TAG_COMBAT_SOURCE, SOURCE_INTERACT);
        syncToClient(player);
    }

    public static void endCombat(ServerPlayer player) {
        player.getPersistentData().putBoolean(TAG_IN_COMBAT, false);
        player.getPersistentData().putInt(TAG_COMBAT_TIME, 0);
        player.getPersistentData().putString(TAG_COMBAT_SOURCE, "");
        syncToClient(player);
    }

    public static boolean isInCombat(ServerPlayer player) {
        return player.getPersistentData().getBoolean(TAG_IN_COMBAT);
    }

    public static int getCombatTime(ServerPlayer player) {
        return player.getPersistentData().getInt(TAG_COMBAT_TIME);
    }

    // ========== /combatmode Command API ==========

    /** 增加战斗状态时长（秒） */
    public static void addCombatTime(ServerPlayer player, int seconds) {
        int newTicks = getCombatTime(player) + seconds * 20;
        setCombatState(player, newTicks);
    }

    /** 设置战斗状态时长（秒），<=0 时结束战斗 */
    public static void setCombatSeconds(ServerPlayer player, int seconds) {
        if (seconds <= 0) {
            endCombat(player);
            return;
        }
        setCombatState(player, seconds * 20);
    }

    /** 减少战斗状态时长（秒），<=0 时结束战斗 */
    public static void removeCombatTime(ServerPlayer player, int seconds) {
        int newTicks = getCombatTime(player) - seconds * 20;
        if (newTicks <= 0) {
            endCombat(player);
        } else {
            setCombatState(player, newTicks);
        }
    }

    /** 重置（清除）战斗状态 */
    public static void resetCombat(ServerPlayer player) {
        endCombat(player);
    }

    private static void setCombatState(ServerPlayer player, int ticks) {
        player.getPersistentData().putBoolean(TAG_IN_COMBAT, true);
        player.getPersistentData().putInt(TAG_COMBAT_TIME, Math.max(0, ticks));
        syncToClient(player);
    }

    private static void setCombatTime(ServerPlayer player, int ticks) {
        player.getPersistentData().putInt(TAG_COMBAT_TIME, ticks);
        if (ticks % 5 == 0) {
            syncToClient(player);
        }
    }

    private static void syncToClient(ServerPlayer player) {
        int ticks = getCombatTime(player);
        PacketDistributor.sendToPlayer(player, new CombatStatePayload(ticks));
    }

    public static void onPlayerLogin(ServerPlayer player) {
        if (isInCombat(player)) {
            endCombat(player);
        } else {
            // 客户端可能在之前断连时残留旧的战斗状态，始终同步清零
            syncToClient(player);
        }
    }
}