package com.createmeow.cm_plugins;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 遗留插件功能的统一持久化数据（复刻 Bukkit 插件的 data.yml / config.yml 存储）：
 * <ul>
 *   <li>禁言列表（Mute）</li>
 *   <li>兑换码（Redemption）</li>
 *   <li>跨维度回退位置（DimensionBack）</li>
 * </ul>
 * 使用 NBT 压缩文件持久化到 {@code config/cm_plugins/legacy_data.dat}。
 */
public class LegacyPluginData {

    private static final Path DATA_FILE = Path.of("config/cm_plugins/legacy_data.dat");

    // ── 禁言（存 UUID -> 到期时间戳毫秒；Long.MAX_VALUE 表示永久）──
    private static final Map<UUID, Long> MUTED = new HashMap<>();

    // ── 兑换码 ──
    public static final Map<Integer, RedemptionCode> CODES = new LinkedHashMap<>();
    public static int nextCodeId = 1;

    // ── 维度回退位置：维度的 ResourceLocation 字符串 + "." + UUID -> [x, y, z, yaw, pitch] ──
    private static final Map<String, double[]> DIM_BACK = new HashMap<>();

    /** 兑换码数据对象。 */
    public static final class RedemptionCode {
        public final int id;
        public String code;
        public CompoundTag item;
        public String usedBy; // 使用者的 UUID 字符串，null 表示未使用

        public RedemptionCode(int id, String code, CompoundTag item, String usedBy) {
            this.id = id;
            this.code = code;
            this.item = item;
            this.usedBy = usedBy;
        }
    }

    /** 从磁盘加载数据（ServerStartingEvent 时调用）。 */
    public static synchronized void load(MinecraftServer server) {
        try {
            if (!Files.exists(DATA_FILE)) return;
            CompoundTag root = NbtIo.readCompressed(DATA_FILE, NbtAccounter.unlimitedHeap());

            MUTED.clear();
            CODES.clear();
            DIM_BACK.clear();

            nextCodeId = Math.max(1, root.getInt("nextId"));

            ListTag mutedList = root.getList("muted", Tag.TAG_COMPOUND);
            for (int i = 0; i < mutedList.size(); i++) {
                CompoundTag mTag = mutedList.getCompound(i);
                MUTED.put(UUID.fromString(mTag.getString("uuid")), mTag.getLong("until"));
            }

            ListTag codeList = root.getList("codes", Tag.TAG_COMPOUND);
            for (int i = 0; i < codeList.size(); i++) {
                CompoundTag c = codeList.getCompound(i);
                int id = c.getInt("id");
                String code = c.getString("code");
                CompoundTag item = c.getCompound("item");
                String usedBy = c.contains("usedBy") ? c.getString("usedBy") : null;
                CODES.put(id, new RedemptionCode(id, code, item, usedBy));
            }

            ListTag dimList = root.getList("dimBack", Tag.TAG_COMPOUND);
            for (int i = 0; i < dimList.size(); i++) {
                CompoundTag d = dimList.getCompound(i);
                String key = d.getString("key");
                double[] pos = new double[]{
                        d.getDouble("x"), d.getDouble("y"), d.getDouble("z"),
                        d.getDouble("yaw"), d.getDouble("pitch")};
                DIM_BACK.put(key, pos);
            }
        } catch (IOException e) {
            createmeowsplugins.LOGGER.error("[LegacyPluginData] 加载遗留插件数据失败", e);
        }
    }

    /** 将数据写回磁盘（数据变更后调用）。 */
    public static synchronized void save(MinecraftServer server) {
        try {
            Files.createDirectories(DATA_FILE.getParent());
            CompoundTag root = new CompoundTag();
            root.putInt("nextId", nextCodeId);

            ListTag mutedList = new ListTag();
            for (Map.Entry<UUID, Long> e : MUTED.entrySet()) {
                CompoundTag mTag = new CompoundTag();
                mTag.putString("uuid", e.getKey().toString());
                mTag.putLong("until", e.getValue());
                mutedList.add(mTag);
            }
            root.put("muted", mutedList);

            ListTag codeList = new ListTag();
            for (RedemptionCode rc : CODES.values()) {
                CompoundTag c = new CompoundTag();
                c.putInt("id", rc.id);
                c.putString("code", rc.code);
                c.put("item", rc.item.copy());
                if (rc.usedBy != null) {
                    c.putString("usedBy", rc.usedBy);
                }
                codeList.add(c);
            }
            root.put("codes", codeList);

            ListTag dimList = new ListTag();
            for (Map.Entry<String, double[]> e : DIM_BACK.entrySet()) {
                CompoundTag d = new CompoundTag();
                d.putString("key", e.getKey());
                double[] p = e.getValue();
                d.putDouble("x", p[0]);
                d.putDouble("y", p[1]);
                d.putDouble("z", p[2]);
                d.putDouble("yaw", p[3]);
                d.putDouble("pitch", p[4]);
                dimList.add(d);
            }
            root.put("dimBack", dimList);

            NbtIo.writeCompressed(root, DATA_FILE);
        } catch (IOException e) {
            createmeowsplugins.LOGGER.error("[LegacyPluginData] 保存遗留插件数据失败", e);
        }
    }

    // ── 禁言 ──
    public static boolean isMuted(UUID uuid) {
        Long until = MUTED.get(uuid);
        return until != null && until > System.currentTimeMillis();
    }

    public static void mute(UUID uuid, long untilMillis) {
        MUTED.put(uuid, untilMillis);
    }

    public static void unmute(UUID uuid) {
        MUTED.remove(uuid);
    }

    /** 解析时长字符串（如 {@code 10d10h10m10s10t}）为 tick 数。 */
    public static long parseDuration(String input) {
        Matcher matcher = Pattern.compile("(\\d+)([tsmhd])").matcher(input.toLowerCase(Locale.ROOT));
        long totalTicks = 0;
        while (matcher.find()) {
            long value = Long.parseLong(matcher.group(1));
            char unit = matcher.group(2).charAt(0);
            totalTicks += value * ticksPerUnit(unit);
        }
        return totalTicks;
    }

    private static long ticksPerUnit(char unit) {
        return switch (unit) {
            case 't' -> 1L;
            case 's' -> 20L;
            case 'm' -> 1200L;      // 60 秒
            case 'h' -> 72000L;     // 3600 秒
            case 'd' -> 1728000L;   // 86400 秒（24 小时）
            default -> 0L;
        };
    }

    // ── 维度回退 ──
    public static void recordDimPos(String dimensionId, UUID uuid, double x, double y, double z, float yaw, float pitch) {
        DIM_BACK.put(dimKey(dimensionId, uuid), new double[]{x, y, z, yaw, pitch});
    }

    public static double[] getDimPos(String dimensionId, UUID uuid) {
        return DIM_BACK.get(dimKey(dimensionId, uuid));
    }

    private static String dimKey(String dimensionId, UUID uuid) {
        return dimensionId + "." + uuid;
    }

    // ── ItemStack 序列化辅助 ──
    public static CompoundTag itemToTag(ItemStack stack, MinecraftServer server) {
        Tag tag = stack.saveOptional(server.registryAccess());
        return tag instanceof CompoundTag ct ? ct : new CompoundTag();
    }

    public static ItemStack tagToItem(CompoundTag tag, MinecraftServer server) {
        return tag == null || tag.isEmpty() ? ItemStack.EMPTY
                : ItemStack.parseOptional(server.registryAccess(), tag);
    }
}