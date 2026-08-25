package io.github.createmeow.timex_rebirth.compat;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

/**
 * RealityValue（理智+健康硬核生存模组）联动（反射调用，无编译期依赖）。
 * - 修改/读取玩家理智值（PlayerExCap.addSanity / getSanity）
 * 未安装 RealityValue 时所有方法静默跳过，不影响游戏。
 */
public class RealityValueCompat {
    private static final String MOD_ID = "reality_value";
    private static boolean checked = false;
    private static boolean loaded = false;
    private static Class<?> playerExCapClass;

    public static boolean isLoaded() {
        if (!checked) {
            checked = true;
            try {
                if (ModList.get().isLoaded(MOD_ID)) {
                    playerExCapClass = Class.forName("dev.anye.mc.reality_value.cap.PlayerExCap");
                    loaded = true;
                }
            } catch (Exception e) {
                loaded = false;
            }
        }
        return loaded;
    }

    /**
     * 扣除/增加玩家健康值（负数表示减少）。amount 为 RealityValue 的健康点数。
     * 与 addSanity 一样走反射调用 PlayerExCap.addHealth。
     */
    public static void addHealth(ServerPlayer player, int amount) {
        if (!isLoaded() || amount == 0) return;
        try {
            Object cap = playerExCapClass.getMethod("get", ServerPlayer.class).invoke(null, player);
            playerExCapClass.getMethod("addHealth", int.class, ServerPlayer.class).invoke(cap, amount, player);
        } catch (Exception e) {
            // 忽略，避免影响游戏
        }
    }

    /**
     * 扣除玩家理智值（负数表示减少）。amount 为 RealityValue 的理智点数。
     */
    public static void addSanity(ServerPlayer player, int amount) {
        if (!isLoaded() || amount == 0) return;
        try {
            Object cap = playerExCapClass.getMethod("get", ServerPlayer.class).invoke(null, player);
            playerExCapClass.getMethod("addSanity", int.class, ServerPlayer.class).invoke(cap, amount, player);
        } catch (Exception e) {
            // 忽略，避免影响游戏
        }
    }
}
