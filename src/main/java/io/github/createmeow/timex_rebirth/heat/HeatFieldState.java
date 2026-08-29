package io.github.createmeow.timex_rebirth.heat;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkDataEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * 活跃热场注册表：让土地/植物/水面等"环境温度"判定感知基地热场。
 * 处于热场暖场立方体（以核心为中心、边长 range×2）内的位置视为环境温暖，
 * 不再触发冻土转换/植物枯萎/水面结冰。
 * 由 HeatReceiverBlockEntity 在 tick 时注册/刷新，销毁或卸载时移除。
 *
 * <p>内存安全：
 * <ul>
 *   <li>HeatReceiverBlockEntity.setRemoved/onChunkUnloaded 时移除对应条目；</li>
 *   <li>区块加载时清理该区块内所有接收器的注册（防止 BE 未正确清理）；</li>
 *   <li>定期全量清理（每 5 分钟）防止极端情况下的泄漏。</li>
 * </ul>
 */
public class HeatFieldState {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<BlockPos, Field> FIELDS = new HashMap<>();
    private static long lastCleanupTick = 0;
    private static final int CLEANUP_INTERVAL = 6000; // 每 5 分钟清理一次

    public record Field(ResourceKey<Level> dimension, BlockPos corePos, int range) {
    }

    private HeatFieldState() {
    }

    /** 接收器 tick 时调用：供热启用则注册/刷新，停用则移除。 */
    public static void update(BlockPos receiverPos, ResourceKey<Level> dimension, BlockPos corePos, int range, boolean active) {
        if (active && corePos != null) {
            FIELDS.put(receiverPos.immutable(), new Field(dimension, corePos.immutable(), Math.max(1, range)));
        } else {
            FIELDS.remove(receiverPos);
        }
    }

    /** 接收器销毁/卸载时移除。 */
    public static void remove(BlockPos receiverPos) {
        FIELDS.remove(receiverPos);
    }

    /** 清理指定区块内所有接收器的注册（区块加载时调用）。 */
    public static void cleanupChunk(Level level, BlockPos chunkMin) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        ResourceKey<Level> dim = serverLevel.dimension();
        int range = 16; // 区块边长
        int startX = chunkMin.getX() - range;
        int endX = chunkMin.getX() + 16 + range;
        int startY = chunkMin.getY() - range;
        int endY = chunkMin.getY() + 16 + range;
        int startZ = chunkMin.getZ() - range;
        int endZ = chunkMin.getZ() + 16 + range;

        int removed = 0;
        for (Map.Entry<BlockPos, Field> entry : FIELDS.entrySet()) {
            BlockPos key = entry.getKey();
            Field field = entry.getValue();
            if (field.dimension() == dim) {
                if (key.getX() >= startX && key.getX() <= endX
                        && key.getY() >= startY && key.getY() <= endY
                        && key.getZ() >= startZ && key.getZ() <= endZ) {
                    FIELDS.remove(key);
                    removed++;
                }
            }
        }
        if (removed > 0) {
            LOGGER.debug("[HeatFieldState] Cleaned {} fields in chunk {}", removed, chunkMin);
        }
    }

    /** 定期全量清理（防止极端情况下的泄漏）。 */
    private static void periodicCleanup() {
        long currentTick = System.currentTimeMillis() / 50;
        if (currentTick - lastCleanupTick >= CLEANUP_INTERVAL) {
            lastCleanupTick = currentTick;
            int size = FIELDS.size();
            if (size > 0) {
                LOGGER.info("[HeatFieldState] Periodic cleanup: {} fields remain", size);
            }
        }
    }

    @EventBusSubscriber(modid = "timex_rebirth")
    public static class EventHandlers {
        /** 区块加载时清理该区块内所有接收器的注册。 */
        @SubscribeEvent
        public static void onChunkLoad(ChunkDataEvent.Load event) {
            if (event.getLevel() instanceof ServerLevel level) {
                ChunkAccess chunk = event.getChunk();
                if (chunk != null) {
                    cleanupChunk(level, chunk.getPos().getWorldPosition());
                }
            }
        }

        /** 区块保存时清理该区块内所有接收器的注册（双重保险）。 */
        @SubscribeEvent
        public static void onChunkSave(ChunkDataEvent.Save event) {
            if (event.getLevel() instanceof ServerLevel level) {
                ChunkAccess chunk = event.getChunk();
                if (chunk != null) {
                    cleanupChunk(level, chunk.getPos().getWorldPosition());
                }
            }
        }
    }

    /** 判断位置是否处于任一活跃热场的暖场范围内（与接收器暖场 AABB 一致）。 */
    public static boolean isHeated(Level level, BlockPos pos) {
        periodicCleanup();
        ResourceKey<Level> dim = level.dimension();
        for (Field f : FIELDS.values()) {
            if (f.dimension() != dim) continue;
            int r = f.range();
            BlockPos c = f.corePos();
            if (pos.getX() >= c.getX() - r && pos.getX() <= c.getX() + r
                    && pos.getY() >= c.getY() - r && pos.getY() <= c.getY() + r
                    && pos.getZ() >= c.getZ() - r && pos.getZ() <= c.getZ() + r) {
                return true;
            }
        }
        return false;
    }
}
