package io.github.createmeow.timex_rebirth.heat;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

/**
 * 活跃热场注册表：让土地/植物/水面等"环境温度"判定感知基地热场。
 * 处于热场暖场立方体（以核心为中心、边长 range×2）内的位置视为环境温暖，
 * 不再触发冻土转换/植物枯萎/水面结冰。
 * 由 HeatReceiverBlockEntity 在 tick 时注册/刷新，销毁或卸载时移除。
 */
public class HeatFieldState {
    private static final Map<BlockPos, Field> FIELDS = new HashMap<>();

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

    /** 判断位置是否处于任一活跃热场的暖场范围内（与接收器暖场 AABB 一致）。 */
    public static boolean isHeated(Level level, BlockPos pos) {
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
