package com.createmeow.cm_plugins;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * 复刻旧 Bukkit 插件的事件处理：禁言（Mute）、聊天展示物品（LiteItemShow）、维度回退（DimensionBack）。
 */
public class LegacyPluginEvents {

    /** 公聊拦截：禁言 + 展示物品。 */
    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();

        // 禁言
        if (LegacyPluginData.isMuted(player.getUUID())) {
            event.setCanceled(true);
            player.sendSystemMessage(Component.literal("§c你已被禁言"));
            return;
        }

        // 展示物品：消息包含关键词时，把关键词替换为带 hover 的手持物品（手上为空则显示空气）
        String keyword = Config.LITE_ITEM_SHOW_KEYWORD.get();
        if (!keyword.isEmpty() && event.getRawText().contains(keyword)) {
            ItemStack held = player.getMainHandItem();
            event.setCanceled(true);
            broadcastItemShow(player, event.getRawText(), keyword, held);
        }
    }

    private static void broadcastItemShow(ServerPlayer player, String rawText, String keyword, ItemStack held) {
        int idx = rawText.indexOf(keyword);
        String left = idx > 0 ? rawText.substring(0, idx) : "";
        String right = idx >= 0 ? rawText.substring(idx + keyword.length()) : "";

        // 手上为空时用「空气」占位物品（合法物品，避免 minecraft:air 及数量 0 的 show_item 编码导致断线）。
        ItemStack show = held.isEmpty() ? createmeowsplugins.AIR_ITEM.get().getDefaultInstance() : held;

        Component itemComponent = Component.literal("[")
                .append(show.getHoverName())
                .append(show.getCount() > 1 ? " x" + show.getCount() : "")
                .append("]")
                .withStyle(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new HoverEvent.ItemStackInfo(show))));

        Component full = Component.literal("<" + player.getGameProfile().getName() + "> " + left)
                .append(itemComponent)
                .append(Component.literal(right));

        player.getServer().getPlayerList().broadcastSystemMessage(full, false);
    }

    /** 跨维度传送前：记录玩家离开当前维度的位置。 */
    @SubscribeEvent
    public static void onTravelToDimension(EntityTravelToDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Level from = player.level();
        LegacyPluginData.recordDimPos(from.dimension().location().toString(), player.getUUID(),
                player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        LegacyPluginData.save(player.getServer());
    }

    /** 维度切换后：若该维度有上次离开记录，则传送回记录位置。 */
    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        String dimId = event.getTo().location().toString();
        double[] pos = LegacyPluginData.getDimPos(dimId, player.getUUID());
        if (pos != null) {
            player.teleportTo(player.serverLevel(), pos[0], pos[1], pos[2], (float) pos[3], (float) pos[4]);
            player.sendSystemMessage(Component.literal("§a[DimensionBack] 已将你传送至上次离开该维度的位置"));
        }
    }
}