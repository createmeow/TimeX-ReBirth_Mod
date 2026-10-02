package com.createmeow.cm_plugins;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.Tag;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 世界实体规则（仅服务端）：
 * <ul>
 * <li>点燃的 TNT（{@link PrimedTnt}）每维度最多存在 {@link #MAX_TNT} 个，超出直接阻止加入（移除）</li>
 * <li>禁止玩家放置盔甲架、物品展示框、发光物品展示框</li>
 * <li>"禁人盒/书"防护：NBT 序列化大小超过 {@link #MAX_ITEM_NBT_CHARS} 的物品视为异常数据物品，
 *     出现即移除（掉落物加入世界时、玩家背包与打开的容器内定期扫描）</li>
 * </ul>
 */
public class WorldRulesHandler {

    /** 每维度同时存在的点燃 TNT 上限 */
    public static final int MAX_TNT = 256;

    /** 异常数据物品判定阈值：物品 NBT 序列化字符串长度上限（≈64KB） */
    public static final int MAX_ITEM_NBT_CHARS = 65536;

    /**
     * 每维度点燃 TNT 增量计数（join +1 / leave -1）。
     * 不用全表 AABB 查询：Sable 的实体查询优化会拦截无限大 AABB 并中止（返回空列表），
     * 导致计数永远为 0；且连锁爆炸时每个新 TNT 触发一次全表遍历开销极大。
     */
    private static final java.util.Map<net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level>, Integer> TNT_COUNT =
            new java.util.HashMap<>();

    // ── TNT 数量限制 ──

    @SubscribeEvent
    public void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;

        // 超大 NBT 掉落物：出现即移除（阻止加入）
        if (event.getEntity() instanceof ItemEntity item) {
            if (isOversized(item.getItem(), event.getLevel())) {
                event.setCanceled(true);
                createmeowsplugins.LOGGER.warn("[WorldRules] 检测到异常数据掉落物（NBT 过大），已移除: {}",
                        item.getItem().getItem());
            }
            return;
        }

        // 点燃的 TNT：超过上限的阻止加入（增量计数，O(1)）
        if (event.getEntity() instanceof PrimedTnt) {
            var key = event.getLevel().dimension();
            int count = TNT_COUNT.getOrDefault(key, 0);
            if (count >= MAX_TNT) {
                event.setCanceled(true);
                createmeowsplugins.LOGGER.warn("[WorldRules] 点燃的 TNT 已达上限 {}，新 TNT 被移除", MAX_TNT);
                return;
            }
            TNT_COUNT.put(key, count + 1);
        }
    }

    /** 点燃的 TNT 移除（爆炸消亡/discard/区块卸载）时递减计数，与 join 平衡。 */
    @SubscribeEvent
    public void onEntityLeave(net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getEntity() instanceof PrimedTnt) {
            var key = event.getLevel().dimension();
            TNT_COUNT.computeIfPresent(key, (k, v) -> v <= 1 ? null : v - 1);
        }
    }

    /** 世界关闭时清空计数，避免下次进入残留。 */
    @SubscribeEvent
    public void onServerStopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event) {
        TNT_COUNT.clear();
    }

    // ── 禁止放置盔甲架 / 物品展示框 / 发光物品展示框 ──

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (stack.is(Items.ARMOR_STAND) || stack.is(Items.ITEM_FRAME) || stack.is(Items.GLOW_ITEM_FRAME)) {
            event.setCanceled(true);
        }
    }

    // ── 异常数据物品（禁人盒/书）：定期扫描玩家背包与打开的容器 ──

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.tickCount % 40 != 0) return;
        if (player.level().isClientSide()) return;

        boolean removed = false;

        // 玩家自身背包（含盔甲/副手）
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (isOversized(inv.getItem(i), player.level())) {
                inv.setItem(i, ItemStack.EMPTY);
                removed = true;
            }
        }

        // 打开的容器（箱子/潜影盒界面等；背包槽与上面重复扫描无害）
        if (player.containerMenu != null) {
            for (Slot slot : player.containerMenu.slots) {
                if (isOversized(slot.getItem(), player.level())) {
                    slot.set(ItemStack.EMPTY);
                    removed = true;
                }
            }
        }

        if (removed) {
            player.displayClientMessage(Component.literal("[安全防护] 检测到异常数据物品，已移除")
                    .withStyle(ChatFormatting.RED), true);
            player.level().playSound(null, player.blockPosition(),
                    SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.5F, 0.6F);
            createmeowsplugins.LOGGER.warn("[WorldRules] 已从玩家 {} 的背包/容器移除异常数据物品",
                    player.getName().getString());
        }
    }

    /**
     * 是否为"异常数据物品"（禁人盒/书）：物品整体 NBT 序列化后超长。
     * 正常物品的 NBT 极小，开销可忽略；扫描频率为每 40 tick 一次。
     */
    private static boolean isOversized(ItemStack stack, net.minecraft.world.level.Level level) {
        if (stack.isEmpty()) return false;
        try {
            net.minecraft.nbt.Tag tag = stack.save(level.registryAccess());
            return tag != null && tag.toString().length() > MAX_ITEM_NBT_CHARS;
        } catch (Exception e) {
            // 序列化失败的物品本身就是异常物品
            return true;
        }
    }
}
