package io.github.createmeow.timex_rebirth.advancement;

import io.github.createmeow.timex_rebirth.TimeX;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 成就系统注册（只注册需要"运行时事件"才能触发的自定义触发器；
 * 其余"获得物品/食用"类成就直接用原版 JSON 触发器，无需注册）：
 * - 它晾干了：目睹潮湿物品被晾晒干（自定义触发器）
 * - 咋这么臭呢？：晾晒湿水的破袜子（自定义触发器，隐藏成就）
 * - 我学会了！：成功完成一项研究（自定义触发器）
 * - 不再寒冷：组装好一台热源供应站（自定义触发器）
 * - 真正的温泉：泡在热流中感受温暖（自定义触发器）
 */
public class AdvancementRegistry {
    public static final DeferredRegister<CriterionTrigger<?>> TRIGGERS =
            DeferredRegister.create(Registries.TRIGGER_TYPE, TimeX.MODID);

    // ── 自定义触发器 ──
    /** 获得 8 个任意废品。 */
    public static final DeferredHolder<CriterionTrigger<?>, ScrapCollectorTrigger> SCRAP_COLLECTOR =
            TRIGGERS.register("scrap_collector", ScrapCollectorTrigger::new);
    /** 目睹潮湿物品被晾晒干。 */
    public static final DeferredHolder<CriterionTrigger<?>, DryingWitnessTrigger> DRYING_WITNESS =
            TRIGGERS.register("drying_witness", DryingWitnessTrigger::new);
    /** 晾晒湿水的破袜子（隐藏成就）。 */
    public static final DeferredHolder<CriterionTrigger<?>, SmellySocksTrigger> SMELLY_SOCKS =
            TRIGGERS.register("smelly_socks", SmellySocksTrigger::new);
    /** 成功完成一项研究。 */
    public static final DeferredHolder<CriterionTrigger<?>, ResearcherTrigger> RESEARCHER =
            TRIGGERS.register("researcher", ResearcherTrigger::new);
    /** 组装好一台热源供应站。 */
    public static final DeferredHolder<CriterionTrigger<?>, HeatStationBuiltTrigger> HEAT_STATION_BUILT =
            TRIGGERS.register("heat_station_built", HeatStationBuiltTrigger::new);
    /** 泡在热流中感受温暖。 */
    public static final DeferredHolder<CriterionTrigger<?>, HotSpringTrigger> HOT_SPRING =
            TRIGGERS.register("hot_spring", HotSpringTrigger::new);

    private AdvancementRegistry() {
    }

    public static final AdvancementRegistry INSTANCE = new AdvancementRegistry();

    public static void register(IEventBus modEventBus) {
        TRIGGERS.register(modEventBus);
    }
}
