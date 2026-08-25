package io.github.createmeow.timex_rebirth.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * Cold Sweat 联动（反射调用，无编译期依赖）。
 * - 读取任意位置的环境温度（WorldHelper.getTemperatureAt）
 * - 降低玩家环境温度使其寒冷更快（Temperature.add, Trait.WORLD）
 */
public class ColdSweatCompat {
    private static boolean checked = false;
    private static boolean loaded = false;

    private static Class<?> temperatureClass;
    private static Class<?> traitClass;
    private static Class<?> worldHelperClass;
    private static Object worldTrait;
    private static Object coreTrait;

    public static boolean isLoaded() {
        if (!checked) {
            checked = true;
            try {
                temperatureClass = Class.forName("com.momosoftworks.coldsweat.api.util.Temperature");
                traitClass = Class.forName("com.momosoftworks.coldsweat.api.util.Temperature$Trait");
                worldHelperClass = Class.forName("com.momosoftworks.coldsweat.util.world.WorldHelper");
                Object[] traits = (Object[]) traitClass.getMethod("values").invoke(null);
                for (Object t : traits) {
                    String name = ((Enum<?>) t).name();
                    if ("WORLD".equals(name)) {
                        worldTrait = t;
                    } else if ("CORE".equals(name)) {
                        coreTrait = t;
                    }
                }
                loaded = true;
            } catch (Exception e) {
                loaded = false;
            }
        }
        return loaded;
    }

    /**
     * 获取某个位置的环境温度（Cold Sweat MC 单位）。
     * 若 Cold Sweat 未安装返回 Double.NaN。
     */
    public static double getTemperatureAt(Level level, BlockPos pos) {
        if (!isLoaded()) return Double.NaN;
        try {
            Object result = worldHelperClass.getMethod("getTemperatureAt", Level.class, BlockPos.class)
                    .invoke(null, level, pos);
            return (double) result;
        } catch (Exception e) {
            return Double.NaN;
        }
    }

    /**
     * 降低玩家环境温度（使其寒冷更快）。amount 为 MC 单位。
     */
    public static void boostCold(ServerPlayer player, double amount) {
        if (!isLoaded() || amount <= 0) return;
        try {
            temperatureClass.getMethod("add", LivingEntity.class, traitClass, double.class)
                    .invoke(null, player, worldTrait, -amount);
        } catch (Exception e) {
            // 忽略，避免影响游戏
        }
    }

    /**
     * 提高玩家核心体温（如热食驱寒）。amount 为 MC 单位。
     */
    public static void addCore(ServerPlayer player, double amount) {
        if (!isLoaded() || amount <= 0) return;
        try {
            temperatureClass.getMethod("add", LivingEntity.class, traitClass, double.class)
                    .invoke(null, player, coreTrait, amount);
        } catch (Exception e) {
            // 忽略，避免影响游戏
        }
    }
}
