package io.github.createmeow.timex_rebirth.research;

import io.github.createmeow.timex_rebirth.TimeXConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * 研究系统事件：
 * - 击杀敌对生物/首领获得研究点数
 * - 使用权限拦截：附魔台（use.enchanting_table）、热源供应站组件（use.heat_station）
 */
public class ResearchEvents {

    // ── 研究点数获取：击杀敌对生物 / 首领 ──

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;

        int points = 0;
        if (event.getEntity() instanceof WitherBoss || event.getEntity() instanceof EnderDragon) {
            points = TimeXConfig.RESEARCH_POINTS_PER_BOSS.get();
        } else if (event.getEntity() instanceof Monster) {
            points = TimeXConfig.RESEARCH_POINTS_PER_HOSTILE.get();
        }
        if (points > 0) {
            ResearchData.addPoints(player, points);
        }
    }

    // ── 死亡重生保留研究数据 ──

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        // PersistentData 在死亡重生时默认不会复制：手动把研究点数/已解锁节点/会话快照带到新实体
        if (!event.isWasDeath()) return;
        Player original = event.getOriginal();
        Player clone = event.getEntity();
        CompoundTag research = original.getPersistentData().getCompound(ResearchData.KEY);
        if (!research.isEmpty()) {
            clone.getPersistentData().put(ResearchData.KEY, research.copy());
        }
    }

    // ── 附魔台使用权限（参考寒霜之心：需研究后才能使用附魔台）──

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        BlockState clicked = event.getLevel().getBlockState(event.getPos());
        // 与已放置的门控方块交互同样需要对应研究（防御炮/加密容器/热源组件/机械动力方块）
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(clicked.getBlock());
        String blockPerm = TechTree.getItemUsePermission(blockId);
        if (blockPerm != null && !TechTree.hasUsePermission(player, blockPerm)) {
            event.setCanceled(true);
            sendLockedMessage(player);
            return;
        }
        if (clicked.is(Blocks.ENCHANTING_TABLE)) {
            if (!TechTree.hasUsePermission(player, "use.enchanting_table")) {
                event.setCanceled(true);
                sendLockedMessage(player);
                return;
            }
        } else if (clicked.is(BlockTags.ANVIL)) {
            if (!TechTree.hasUsePermission(player, "use.anvil")) {
                event.setCanceled(true);
                sendLockedMessage(player);
                return;
            }
        } else if (clicked.is(Blocks.BREWING_STAND)) {
            if (!TechTree.hasUsePermission(player, "use.brewing_stand")) {
                event.setCanceled(true);
                sendLockedMessage(player);
                return;
            }
        }
        // 手持被门控的物品时禁止交互（如未研究前手持热源组件右键放置）
        checkItemUse(player, event.getItemStack(), event);
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        checkItemUse(player, event.getItemStack(), event);
    }

    private static void checkItemUse(ServerPlayer player, ItemStack stack, PlayerInteractEvent event) {
        if (stack.isEmpty()) return;
        ResourceLocation id = stack.getItemHolder().unwrapKey().map(ResourceKey::location).orElse(null);
        if (id == null) return;
        String perm = TechTree.getItemUsePermission(id);
        if (perm != null && !TechTree.hasUsePermission(player, perm)) {
            if (event instanceof ICancellableEvent cancellable) {
                cancellable.setCanceled(true);
            }
            sendLockedMessage(player, stack);
        }
    }

    /**
     * 动作栏提示（参考寒霜之心）：
     * 手持作物/种子放置提示"种植"；手持其它方块物品放置提示"组装"；交互提示"用"。
     */
    private static void sendLockedMessage(Player player, ItemStack stack) {
        boolean isPlant = stack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof net.minecraft.world.level.block.CropBlock;
        boolean isBlockItem = stack.getItem() instanceof BlockItem;
        player.displayClientMessage(Component.translatable(
                isPlant ? "research.timex_rebirth.locked_plant"
                        : isBlockItem ? "research.timex_rebirth.locked_place"
                        : "research.timex_rebirth.locked_use"), true);
    }

    /** 与已放置方块交互场景：恒提示"用"。 */
    private static void sendLockedMessage(Player player) {
        player.displayClientMessage(Component.translatable("research.timex_rebirth.locked_use"), true);
    }
}
