package io.github.createmeow.timex_rebirth.research;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 玩家研究数据（持久化于 player.getPersistentData()，参照 basecore PartHolder 模式）。
 * - 研究点数 research_points
 * - 已解锁节点集合 unlocked（字符串列表）
 */
public class ResearchData {
    public static final String KEY = "timex_research";
    private static final String POINTS = "points";
    private static final String UNLOCKED = "unlocked";
    /** 活动研究会话快照（兜底持久化，独立于研究站区块保存路径） */
    private static final String ACTIVE_SESSION = "active_session";

    /** 旧节点 ID → 新节点 ID（研究细分/统一重构后的存档迁移） */
    private static final Map<String, String> LEGACY_NODE_IDS = new HashMap<>();

    static {
        LEGACY_NODE_IDS.put("rye_milling", "crop_cultivation");
        LEGACY_NODE_IDS.put("rye_baking", "baking");
    }

    private ResearchData() {
    }

    private static CompoundTag getTag(Player player) {
        CompoundTag root = player.getPersistentData();
        if (!root.contains(KEY, Tag.TAG_COMPOUND)) {
            root.put(KEY, new CompoundTag());
        }
        return root.getCompound(KEY);
    }

    /**
     * 将会话快照保存到研究者玩家持久数据（随玩家存档保存，防止研究站区块数据丢失时进度全失）。
     * 快照内容 = 会话 NBT + 研究站坐标（用于匹配对应研究站）。
     */
    public static void saveActiveSession(ServerPlayer researcher, CompoundTag sessionTag, long stationPosKey) {
        CompoundTag tag = getTag(researcher);
        CompoundTag snap = new CompoundTag();
        snap.put("session", sessionTag);
        snap.putLong("station", stationPosKey);
        tag.put(ACTIVE_SESSION, snap);
    }

    public static CompoundTag getActiveSession(Player player) {
        CompoundTag tag = getTag(player);
        if (!tag.contains(ACTIVE_SESSION, Tag.TAG_COMPOUND)) return null;
        return tag.getCompound(ACTIVE_SESSION);
    }

    public static void clearActiveSession(Player player) {
        CompoundTag tag = getTag(player);
        tag.remove(ACTIVE_SESSION);
    }

    public static boolean hasActiveSession(Player player) {
        return getTag(player).contains(ACTIVE_SESSION, Tag.TAG_COMPOUND);
    }

    public static int getPoints(Player player) {
        return getTag(player).getInt(POINTS);
    }

    public static void addPoints(Player player, int delta) {
        if (delta == 0) return;
        int value = Math.max(0, getPoints(player) + delta);
        getTag(player).putInt(POINTS, value);
        if (player instanceof ServerPlayer serverPlayer) {
            ResearchNetwork.syncPlayer(serverPlayer);
        }
    }

    /**
     * 尝试扣除研究点数，余额不足返回 false。
     */
    public static boolean spendPoints(Player player, int amount) {
        if (amount < 0) return false;
        int current = getPoints(player);
        if (current < amount) return false;
        getTag(player).putInt(POINTS, current - amount);
        if (player instanceof ServerPlayer serverPlayer) {
            ResearchNetwork.syncPlayer(serverPlayer);
        }
        return true;
    }

    /** 检查研究点数是否足够（不扣除）。 */
    public static boolean hasEnoughPoints(Player player, int amount) {
        return amount >= 0 && getPoints(player) >= amount;
    }

    public static Set<String> getUnlocked(Player player) {
        Set<String> set = new HashSet<>();
        ListTag list = getTag(player).getList(UNLOCKED, Tag.TAG_STRING);
        for (Tag tag : list) {
            set.add(LEGACY_NODE_IDS.getOrDefault(tag.getAsString(), tag.getAsString()));
        }
        return set;
    }

    public static boolean isUnlocked(Player player, String nodeId) {
        return getUnlocked(player).contains(nodeId);
    }

    public static void unlock(ServerPlayer player, String nodeId) {
        CompoundTag tag = getTag(player);
        ListTag list = tag.getList(UNLOCKED, Tag.TAG_STRING);
        for (Tag t : list) {
            if (t.getAsString().equals(nodeId)) return;
        }
        list.add(StringTag.valueOf(nodeId));
        tag.put(UNLOCKED, list);
        ResearchNetwork.syncPlayer(player);
    }

    /**
     * 完全重置玩家的研究数据（调试命令用）：
     * 点数归零、清空已解锁节点、移除活动会话快照，并终止所有研究站上涉及该玩家的会话。
     */
    public static void reset(ServerPlayer player) {
        player.getPersistentData().remove(KEY);
        ResearchStationBlockEntity.terminateSessions(player);
        ResearchNetwork.syncPlayer(player);
    }
}
