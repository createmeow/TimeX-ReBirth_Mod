package io.github.createmeow.timex_rebirth.advancement;

import io.github.createmeow.timex_rebirth.TimeX;
import net.minecraft.server.level.ServerPlayer;

/**
 * 成就触发器辅助类：提供运行时事件需要的统一触发接口。
 * （"获得物品/食用"类成就走原版 JSON 触发器，无需调用本类。）
 */
public class AdvancementTriggers {

    /** 触发"长大我要开废品站！"成就（获得 8 个任意废品）。 */
    public static void triggerScrapCollector(ServerPlayer player) {
        TimeX.advancement().SCRAP_COLLECTOR.get().trigger(player);
    }

    /** 触发"它晾干了"成就（目睹潮湿物品被晾晒干）。 */
    public static void triggerDryingWitness(ServerPlayer player) {
        TimeX.advancement().DRYING_WITNESS.get().trigger(player);
    }

    /** 触发"咋这么臭呢？"成就（晾晒湿水的破袜子，隐藏成就）。 */
    public static void triggerSmellySocks(ServerPlayer player) {
        TimeX.advancement().SMELLY_SOCKS.get().trigger(player);
    }

    /** 触发"我学会了！"成就（成功完成一项研究）。 */
    public static void triggerResearcher(ServerPlayer player) {
        TimeX.advancement().RESEARCHER.get().trigger(player);
    }

    /** 触发"不再寒冷"成就（组装好一台热源供应站）。 */
    public static void triggerHeatStationBuilt(ServerPlayer player) {
        TimeX.advancement().HEAT_STATION_BUILT.get().trigger(player);
    }

    /** 触发"真正的温泉"成就（泡在热流中感受温暖）。 */
    public static void triggerHotSpring(ServerPlayer player) {
        TimeX.advancement().HOT_SPRING.get().trigger(player);
    }
}
