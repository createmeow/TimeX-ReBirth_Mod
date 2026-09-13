package com.createmeow.underwaterplugin;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 水深计算工具：计算玩家眼睛上方连续水柱的方块数（含眼睛所在方块）。
 * 返回 0 表示眼睛不在水中。
 */
public final class WaterDepthUtil {

    /** 水柱扫描上限（防超深坐标异常导致长循环） */
    private static final int MAX_SCAN = 320;

    private WaterDepthUtil() {}

    public static float getWaterColumnDepth(Player player, Level level) {
        Vec3 eye = player.getEyePosition();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(
                Mth.floor(eye.x), Mth.floor(eye.y), Mth.floor(eye.z));
        if (!isWaterAt(level, pos)) return 0.0f;
        float depth = 0.0f;
        while (depth < MAX_SCAN && pos.getY() < level.getMaxBuildHeight() && isWaterAt(level, pos)) {
            pos.setY(pos.getY() + 1);
            depth += 1.0f;
        }
        return depth;
    }

    private static boolean isWaterAt(Level level, BlockPos pos) {
        return level.getFluidState(pos).is(FluidTags.WATER);
    }
}
