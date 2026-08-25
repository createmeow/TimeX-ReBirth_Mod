package com.createmeow.cm_plugins;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SpatialInventoryManager {
    private static final Path DATA_DIR = Path.of("config/cm_plugins/playerdata");

    private static final Map<UUID, PlayerSpatialData> playerDataMap = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> pendingPurchases = new ConcurrentHashMap<>();

    public static class PlayerSpatialData {
        public int unlockedSlots = 9;
        public List<ItemStack> items = new ArrayList<>();
    }

    public static void loadPlayerData(ServerPlayer player) {
        UUID uuid = player.getUUID();
        Path filePath = DATA_DIR.resolve(uuid + ".dat");
        PlayerSpatialData data = new PlayerSpatialData();

        if (Files.exists(filePath)) {
            data = readDataFile(player, filePath, null);
        } else {
            // 离线模式下 UUID 由用户名派生，改名后 UUID 变化导致旧数据丢失。
            // 按用户名回退查找并自动迁移到新 UUID 文件。
            PlayerSpatialData oldData = findDataByPlayerName(player);
            if (oldData != null) {
                data = oldData;
                savePlayerData(player); // 迁移到新 UUID 文件
                createmeowsplugins.LOGGER.info("[量子空间] 玩家 {} 改名迁移完成: {} → {}",
                        player.getName().getString(), oldData.unlockedSlots, data.unlockedSlots);
            } else {
                data.unlockedSlots = Config.SPATIAL_INITIAL_SLOTS.getAsInt();
            }
        }

        playerDataMap.put(uuid, data);
    }

    /** 读取指定数据文件 */
    private static PlayerSpatialData readDataFile(ServerPlayer player, Path filePath, PlayerSpatialData fallback) {
        PlayerSpatialData data = new PlayerSpatialData();
        try {
            CompoundTag tag = NbtIo.readCompressed(filePath, NbtAccounter.unlimitedHeap());
            int unlocked = tag.getInt("UnlockedSlots");

            NonNullList<ItemStack> items = NonNullList.withSize(54, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(tag, items, player.serverLevel().registryAccess());
            data.items = new ArrayList<>();
            for (ItemStack stack : items) {
                data.items.add(stack.copy());
            }

            // 解锁记录缺失/为0但物品存在时，按物品所在最大槽位推断，防止解锁记录丢失
            if (unlocked <= 0) {
                int maxOccupied = -1;
                for (int i = 0; i < items.size(); i++) {
                    if (!items.get(i).isEmpty()) maxOccupied = i;
                }
                unlocked = maxOccupied >= 0 ? Math.min(maxOccupied + 1, Config.SPATIAL_MAX_SLOTS.getAsInt())
                        : Config.SPATIAL_INITIAL_SLOTS.getAsInt();
                createmeowsplugins.LOGGER.info("[量子空间] {} 解锁记录缺失，按物品推断为 {} 格",
                        player.getName().getString(), unlocked);
            }
            data.unlockedSlots = Math.max(unlocked, Config.SPATIAL_INITIAL_SLOTS.getAsInt());
        } catch (IOException e) {
            createmeowsplugins.LOGGER.error("[量子空间] 加载玩家数据失败: {}", filePath, e);
            if (fallback != null) return fallback;
            data.unlockedSlots = Config.SPATIAL_INITIAL_SLOTS.getAsInt();
        }
        return data;
    }

    /** 按玩家名在数据目录中查找旧数据（用于改名/离线UUID变化后的恢复） */
    private static PlayerSpatialData findDataByPlayerName(ServerPlayer player) {
        String playerName = player.getName().getString();
        try {
            if (!Files.isDirectory(DATA_DIR)) return null;
            try (var stream = Files.list(DATA_DIR)) {
                var files = stream.filter(p -> p.getFileName().toString().endsWith(".dat")).toList();
                for (Path path : files) {
                    try {
                        CompoundTag tag = NbtIo.readCompressed(path, NbtAccounter.unlimitedHeap());
                        if (tag.contains("PlayerName") && playerName.equals(tag.getString("PlayerName"))) {
                            // 找到匹配的旧数据，读取并删除旧文件
                            PlayerSpatialData data = readDataFile(player, path, null);
                            Files.deleteIfExists(path);
                            return data;
                        }
                    } catch (Exception ignored) {
                        // 跳过损坏或旧格式文件
                    }
                }
            }
        } catch (IOException e) {
            createmeowsplugins.LOGGER.warn("[量子空间] 按玩家名查找数据失败: {}", e.getMessage());
        }
        return null;
    }

    public static void savePlayerData(ServerPlayer player) {
        UUID uuid = player.getUUID();
        PlayerSpatialData data = playerDataMap.get(uuid);
        if (data == null) return;

        try {
            Files.createDirectories(DATA_DIR);
            Path filePath = DATA_DIR.resolve(uuid + ".dat");

            CompoundTag tag = new CompoundTag();
            tag.putInt("UnlockedSlots", data.unlockedSlots);
            tag.putString("PlayerName", player.getName().getString());

            NonNullList<ItemStack> items = NonNullList.withSize(54, ItemStack.EMPTY);
            for (int i = 0; i < Math.min(data.items.size(), 54); i++) {
                if (data.items.get(i) != null) {
                    items.set(i, data.items.get(i).copy());
                }
            }
            ContainerHelper.saveAllItems(tag, items, player.serverLevel().registryAccess());

            NbtIo.writeCompressed(tag, filePath);
        } catch (IOException e) {
            createmeowsplugins.LOGGER.error("[量子空间] 保存玩家数据失败: {}", uuid, e);
        }
    }

    public static void cleanupPlayer(ServerPlayer player) {
        playerDataMap.remove(player.getUUID());
        pendingPurchases.remove(player.getUUID());
    }

    public static PlayerSpatialData getData(Player player) {
        return playerDataMap.computeIfAbsent(player.getUUID(), uuid -> {
            // 优先从磁盘加载，避免创建默认值覆盖真实数据
            if (player instanceof ServerPlayer serverPlayer) {
                Path filePath = DATA_DIR.resolve(uuid + ".dat");
                if (Files.exists(filePath)) {
                    return readDataFile(serverPlayer, filePath, null);
                }
                PlayerSpatialData oldData = findDataByPlayerName(serverPlayer);
                if (oldData != null) {
                    // 迁移到新 UUID：直接写文件（此时尚未放入 map）
                    try {
                        Files.createDirectories(DATA_DIR);
                        CompoundTag tag = new CompoundTag();
                        tag.putInt("UnlockedSlots", oldData.unlockedSlots);
                        tag.putString("PlayerName", serverPlayer.getName().getString());
                        NonNullList<ItemStack> items = NonNullList.withSize(54, ItemStack.EMPTY);
                        for (int i = 0; i < Math.min(oldData.items.size(), 54); i++) {
                            if (oldData.items.get(i) != null) {
                                items.set(i, oldData.items.get(i).copy());
                            }
                        }
                        ContainerHelper.saveAllItems(tag, items, serverPlayer.serverLevel().registryAccess());
                        NbtIo.writeCompressed(tag, filePath);
                    } catch (IOException e) {
                        createmeowsplugins.LOGGER.error("[量子空间] 迁移玩家数据失败: {}", uuid, e);
                    }
                    return oldData;
                }
            }
            PlayerSpatialData data = new PlayerSpatialData();
            data.unlockedSlots = Config.SPATIAL_INITIAL_SLOTS.getAsInt();
            return data;
        });
    }

    // ========== Container ==========
    public static class SpatialContainer extends AbstractContainerMenu {
        private static final int CONTAINER_SIZE = 54;
        private final SimpleContainer container;
        private final Player player;
        private final int unlockedSlots;

        public SpatialContainer(int containerId, Inventory playerInventory) {
            this(containerId, playerInventory, new SimpleContainer(CONTAINER_SIZE), 9);
        }

        public SpatialContainer(int containerId, Inventory playerInventory,
                                SimpleContainer container, int unlockedSlots) {
            super(createmeowsplugins.SPATIAL_MENU.get(), containerId);
            this.container = container;
            this.player = playerInventory.player;
            int maxSlots = Config.SPATIAL_MAX_SLOTS.getAsInt();
            this.unlockedSlots = Math.min(unlockedSlots, maxSlots);

            // Fill locked slots with barrier items
            int fillStart = this.unlockedSlots;
            int fillEnd = Math.min(maxSlots, CONTAINER_SIZE);
            for (int i = fillStart; i < fillEnd; i++) {
                ItemStack barrier = new ItemStack(Items.BARRIER);
                barrier.set(DataComponents.CUSTOM_NAME, Component.literal("§c未获取的空间"));
                container.setItem(i, barrier);
            }

            // Always create 54 container slots on both sides
            for (int index = 0; index < CONTAINER_SIZE; index++) {
                final int slotIndex = index;
                this.addSlot(new Slot(container, index, 8 + (index % 9) * 18, 18 + (index / 9) * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return slotIndex < fillStart;
                    }

                    @Override
                    public boolean mayPickup(Player player) {
                        return slotIndex < fillStart;
                    }
                });
            }

            // Player inventory (3 rows of 9)
            int playerInvStartY = 18 + 6 * 18 + 14;
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 9; col++) {
                    this.addSlot(new Slot(playerInventory, col + row * 9 + 9,
                            8 + col * 18, playerInvStartY + row * 18));
                }
            }

            // Player hotbar (9 slots)
            int hotbarY = playerInvStartY + 3 * 18 + 4;
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col, 8 + col * 18, hotbarY));
            }
        }

        @Override
        public ItemStack quickMoveStack(Player player, int slotIndex) {
            Slot slot = this.slots.get(slotIndex);
            if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

            ItemStack stack = slot.getItem();
            if (stack.is(Items.BARRIER)) return ItemStack.EMPTY;

            ItemStack result = stack.copy();
            int containerSlots = CONTAINER_SIZE;
            int playerSlots = 36;

            if (slotIndex < containerSlots) {
                if (!this.moveItemStackTo(stack, containerSlots, containerSlots + playerSlots, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.moveItemStackTo(stack, 0, unlockedSlots, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            return result;
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        public void removed(Player player) {
            super.removed(player);

            if (!player.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
                // 只在地图中存在玩家数据时才保存，防止用默认值覆盖丢失解锁记录
                PlayerSpatialData data = playerDataMap.get(player.getUUID());
                if (data == null) return;

                // 从容器读取当前展示的物品（已解锁区域）
                List<ItemStack> newItems = new ArrayList<>();
                for (int i = 0; i < unlockedSlots; i++) {
                    ItemStack stack = container.getItem(i);
                    newItems.add(stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
                }

                // 保留旧数据中超出本次展示范围的物品，防止截断丢失
                if (newItems.size() < data.items.size()) {
                    newItems.addAll(data.items.subList(newItems.size(), data.items.size()));
                }
                data.items = newItems;
                savePlayerData(serverPlayer);

                // Play close sound
                player.playNotifySound(SoundEvents.ENDER_CHEST_CLOSE, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
        }
    }

    // ========== GUI Opening ==========
    public static void openSpatialInventory(ServerPlayer player) {
        PlayerSpatialData data = getData(player);
        int maxSlots = Config.SPATIAL_MAX_SLOTS.getAsInt();
        int unlockedSlots = Math.min(data.unlockedSlots, maxSlots);

        SimpleContainer container = new SimpleContainer(54);

        for (int i = 0; i < unlockedSlots && i < data.items.size(); i++) {
            if (data.items.get(i) != null && !data.items.get(i).isEmpty()) {
                container.setItem(i, data.items.get(i).copy());
            }
        }

        player.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> new SpatialContainer(id, inv, container, unlockedSlots),
                Component.literal("§8量子空间")
        ));

        player.playNotifySound(SoundEvents.ENDER_CHEST_OPEN, SoundSource.BLOCKS, 1.0F, 1.2F);
    }

    // ========== Slot Purchase Logic ==========
    public static void handleAddCommand(ServerPlayer player) {
        UUID uuid = player.getUUID();
        int maxSlots = Config.SPATIAL_MAX_SLOTS.getAsInt();
        PlayerSpatialData data = getData(player);

        if (data.unlockedSlots >= maxSlots) {
            player.sendSystemMessage(Component.literal("§c已达到最大空间格数! 无法继续购买"));
            player.playNotifySound(SoundEvents.VILLAGER_NO, SoundSource.BLOCKS, 1.0F, 1.0F);
            return;
        }

        Long pendingTime = pendingPurchases.get(uuid);
        if (pendingTime != null) {
            int timeoutSecs = Config.SPATIAL_CONFIRMATION_TIMEOUT.getAsInt();
            boolean expired = (System.currentTimeMillis() - pendingTime) > (timeoutSecs * 1000L);
            pendingPurchases.remove(uuid);

            if (expired) {
                player.sendSystemMessage(Component.literal("§c购买请求已过期，请重新输入"));
                player.playNotifySound(SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 1.0F);
                return;
            }

            if (data.unlockedSlots >= maxSlots) {
                player.sendSystemMessage(Component.literal("§c已达到最大空间格数! 购买已取消"));
                player.playNotifySound(SoundEvents.VILLAGER_NO, SoundSource.BLOCKS, 1.0F, 1.0F);
                return;
            }

            int cost = Config.SPATIAL_SLOT_COST.getAsInt();
            long balance = NumismaticHelper.getValue(player);
            if (balance < cost) {
                player.sendSystemMessage(Component.literal("§c余额不足! 需要 §e" + cost + " §c青铜币"));
                player.playNotifySound(SoundEvents.VILLAGER_NO, SoundSource.BLOCKS, 1.0F, 1.0F);
                return;
            }

            // Deduct currency using numismaticoverhaul
            NumismaticHelper.modify(player, -cost);
            data.unlockedSlots++;

            player.sendSystemMessage(Component.literal("§a成功解锁新空间格! 当前空间: §e" + data.unlockedSlots + "格"));
            player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 1.0F, 1.5F);
            savePlayerData(player);

            openSpatialInventory(player);
        } else {
            pendingPurchases.put(uuid, System.currentTimeMillis());
            int cost = Config.SPATIAL_SLOT_COST.getAsInt();

            player.sendSystemMessage(Component.literal("§e确定要花费 §c" + cost + " §e青铜币购买空间格吗?"));
            player.sendSystemMessage(Component.literal("§6请再次输入 §a/spatial add §6确认购买"));
            player.sendSystemMessage(Component.literal("§7(输入 §c/spatial cancel §7取消操作)"));
            player.playNotifySound(SoundEvents.NOTE_BLOCK_HARP.value(), SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    public static void handleCancelCommand(ServerPlayer player) {
        UUID uuid = player.getUUID();
        if (pendingPurchases.remove(uuid) != null) {
            player.sendSystemMessage(Component.literal("§a已取消最近的购买请求"));
            player.playNotifySound(SoundEvents.ITEM_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
        } else {
            player.sendSystemMessage(Component.literal("§c没有待处理的购买请求"));
            player.playNotifySound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 0.8F, 0.5F);
        }
    }
}