package io.github.createmeow.timex_rebirth;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 本模组（timex_rebirth）的配置文件。
 * 涵盖：每日天气权重、暴风雪特效、冻土转换、植物枯萎、Cold Sweat 联动、种子奖励。
 */
public class TimeXConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // ── 每日天气（温度曲线机制，确定性分段）──
    public static final ModConfigSpec.IntValue WEATHER_CYCLE_LENGTH = BUILDER
            .comment("温度曲线周期（天）：每个周期按 晴→雪→暴风雪→雪→晴 分段演进，"
                    + "暴风雪段每周期仅一次且夹在雪段之间（结束后转普通降雪），"
                    + "与前次暴风雪间隔接近整个周期（远超 5 天）；不再生成雨/雷雨（积雪群系中表现为普通降雪）")
            .defineInRange("weather.cycle_length", 30, 10, 120);

    // ── 暴风雪特效 ──
    public static final ModConfigSpec.DoubleValue BLIZZARD_FOV_REDUCTION = BUILDER
            .comment("暴风雪天气下露天玩家的视场角减少量")
            .defineInRange("blizzard.fov_reduction", 10.0, 0.0, 60.0);
    public static final ModConfigSpec.DoubleValue BLIZZARD_FOG_SCALE = BUILDER
            .comment("暴风雪白雾远平面缩放系数（0~1，1 = 不缩放维持原渲染距离；0.2 = 雾结束于 20% 渲染距离处）")
            .defineInRange("blizzard.fog_scale", 0.2, 0.05, 1.0);
    public static final ModConfigSpec.DoubleValue BLIZZARD_FOG_TRANSITION_SECONDS = BUILDER
            .comment("白雾出现/消失的平滑过渡时间（秒）")
            .defineInRange("blizzard.fog_transition_seconds", 10.0, 1.0, 30.0);

    // ── 雪累积 ──
    public static final ModConfigSpec.BooleanValue SNOW_ACCUMULATION_ENABLED = BUILDER
            .comment("是否启用降雪累积（降雪天气下已覆盖的雪层逐渐增厚）")
            .define("snow.accumulation_enabled", true);
    public static final ModConfigSpec.DoubleValue SNOW_ACCUMULATION_CHANCE = BUILDER
            .comment("每个区块每次随机刻尝试累积雪层的概率（与原版降雪速率同量级）")
            .defineInRange("snow.accumulation_chance", 0.05, 0.0, 1.0);
    public static final ModConfigSpec.DoubleValue SNOW_ACCUMULATION_TEMP = BUILDER
            .comment("允许雪累积的环境温度上限（Cold Sweat MC 单位，低于该值才累积）")
            .defineInRange("snow.accumulation_temp_threshold", 0.4, -3.0, 3.0);

    public static final ModConfigSpec.DoubleValue BLIZZARD_SUSPICIOUS_SNOW_CHANCE = BUILDER
            .comment("暴雪天气下每次雪层增厚时，原雪层转为「可疑的积雪」（可用刷子刷出废品）的概率")
            .defineInRange("snow.blizzard_suspicious_chance", 0.02, 0.0, 1.0);

    // ── 水面结冰 ──
    public static final ModConfigSpec.BooleanValue WATER_FREEZE_ENABLED = BUILDER
            .comment("是否启用水面结冰（低温环境下露天水源转为冰）")
            .define("water.freeze_enabled", true);
    public static final ModConfigSpec.DoubleValue WATER_FREEZE_CHANCE = BUILDER
            .comment("每个区块每次随机刻尝试冻结水面的概率")
            .defineInRange("water.freeze_chance", 0.05, 0.0, 1.0);
    public static final ModConfigSpec.DoubleValue WATER_FREEZE_TEMP = BUILDER
            .comment("水面结冰的环境温度上限（Cold Sweat MC 单位，低于该值才结冰）")
            .defineInRange("water.freeze_temp_threshold", 0.0, -3.0, 3.0);

    // ── 冻土转换 ──
    public static final ModConfigSpec.DoubleValue FROZEN_SOIL_TEMP_THRESHOLD = BUILDER
            .comment("泥土/耕地转为冻土的环境温度阈值（Cold Sweat MC 单位，0.5≈12.5℃；低于该值才转换）")
            .defineInRange("frozen_soil.temp_threshold", 0.3, -3.0, 3.0);
    public static final ModConfigSpec.DoubleValue FROZEN_SOIL_CHANCE = BUILDER
            .comment("每次随机 tick 泥土/耕地转为冻土的概率")
            .defineInRange("frozen_soil.chance", 0.03, 0.0, 1.0);

    // ── 植物枯萎 ──
    public static final ModConfigSpec.DoubleValue PLANT_WILT_CHANCE = BUILDER
            .comment("每次随机 tick 符合条件的植物枯萎的概率")
            .defineInRange("plant.wilt_chance", 0.01, 0.0, 1.0);
    public static final ModConfigSpec.DoubleValue PLANT_COLD_TEMP = BUILDER
            .comment("判定\"环境温度低\"的阈值（Cold Sweat MC 单位，低于该值视为低温）")
            .defineInRange("plant.cold_temp_threshold", 0.3, -3.0, 3.0);
    public static final ModConfigSpec.DoubleValue PLANT_HOT_TEMP = BUILDER
            .comment("判定\"环境温度高\"的阈值（Cold Sweat MC 单位，高于该值视为高温）")
            .defineInRange("plant.hot_temp_threshold", 1.8, -3.0, 3.0);

    // ── Cold Sweat 联动 ──
    public static final ModConfigSpec.DoubleValue COLD_SWEAT_BOOST = BUILDER
            .comment("寒冷天气（雪/雷/暴风雪）下玩家每次 tick 额外降低的环境温度（Cold Sweat MC 单位）")
            .defineInRange("cold_sweat.cold_boost", 0.08, 0.0, 1.0);

    // ── 种子奖励 ──
    public static final ModConfigSpec.DoubleValue PERMAFROST_SEED_CHANCE = BUILDER
            .comment("营火烧炼冻土完成时额外掉落作物种子的概率")
            .defineInRange("permafrost.seed_chance", 0.15, 0.0, 1.0);

    // ── 食物温度联动（Freeze-It-And-Heat-It / 热食驱寒）──
    public static final ModConfigSpec.IntValue FIAHI_FROZEN_SANITY_LOSS = BUILDER
            .comment("吃冷冻食物时每级扣除的理智值（fiahi 联动，按冻结等级相乘）")
            .defineInRange("food.frozen_sanity_loss", 1, 0, 10);
    public static final ModConfigSpec.IntValue FIAHI_ROTTEN_SANITY_LOSS = BUILDER
            .comment("吃腐败食物时每级扣除的理智值（fiahi 联动，按腐败等级相乘）")
            .defineInRange("food.rotten_sanity_loss", 2, 0, 10);
    public static final ModConfigSpec.DoubleValue HOT_FOOD_WARMTH = BUILDER
            .comment("食用 timex_rebirth:heating_food 标签内物品时提升的核心体温（Cold Sweat MC 单位，中性体温约 0.5，一次热食约 0.3~0.5）")
            .defineInRange("food.hot_food_warmth", 0.4, 0.0, 5.0);

    // ── 基地供热（BaseCore × 热流 × Create 联动）──
    public static final ModConfigSpec.IntValue HEAT_RECEIVER_CAPACITY = BUILDER
            .comment("热源接收器储罐容量 (mb)")
            .defineInRange("heat.receiver_capacity", 4000, 100, 100000);
    public static final ModConfigSpec.IntValue HEAT_CONSUME_MB_PER_LEVEL = BUILDER
            .comment("热流消耗速率：每个抽流间隔、每等级消耗的热流 (mb × 等级)。10 级满配约 5mb/tick，与热源发生器 6mb/tick 产出匹配，避免热流瞬间耗尽")
            .defineInRange("heat.consume_mb_per_level", 10, 1, 10000);
    public static final ModConfigSpec.IntValue HEAT_DRAIN_INTERVAL = BUILDER
            .comment("热流抽取间隔 (tick)，供热开启期间按此节奏消耗储罐热流")
            .defineInRange("heat.drain_interval", 20, 1, 24000);
    public static final ModConfigSpec.IntValue HEAT_WARM_UP_TICKS = BUILDER
            .comment("暖场预热时长 (tick)：开启供热后暖场强度随时间线性爬升至满档")
            .defineInRange("heat.warm_up_ticks", 20, 1, 24000);
    public static final ModConfigSpec.IntValue HEAT_MAX_WARMTH_LEVEL = BUILDER
            .comment("热场最大温暖效果等级（对应 cold_sweat:warmth 效果，冷汗壁炉上限为 10；1/2/3 个桥接模块分别提供 3/7/10 级，受此上限限制）")
            .defineInRange("heat.max_warmth_level", 10, 1, 20);
    public static final ModConfigSpec.DoubleValue HEAT_FIELD_TEMP = BUILDER
            .comment("热场暖场范围内位置的环境温度下限（Cold Sweat MC 单位，中性约 1.1；用于 fiahi 食物冻结/腐烂等位置温度判定，使热场内食物正常腐烂）")
            .defineInRange("heat.field_temp", 1.2, 0.0, 3.0);
    public static final ModConfigSpec.IntValue HEAT_BRIDGE_MODULE_COST = BUILDER
            .comment("热源桥接模块在零件升级界面的售价（component 模式）")
            .defineInRange("heat.bridge_module_cost", 480, 1, 10000);

    // ── 热源供应站（松散多方块：发生器 + 加热底座 + 燃料接收器 + 适配器 + 模块插槽）──
    public static final ModConfigSpec.IntValue HEAT_STATION_BASE_RATE = BUILDER
            .comment("热源发生器基础热流产出速率 (mb/tick)，受增产模块加成")
            .defineInRange("heat_station.base_rate", 6, 1, 10000);
    public static final ModConfigSpec.IntValue HEAT_STATION_TANK_CAPACITY = BUILDER
            .comment("热源发生器熔岩/热流储罐容量 (mb)")
            .defineInRange("heat_station.tank_capacity", 20000, 100, 1000000);
    public static final ModConfigSpec.IntValue HEAT_STATION_BASE_HEALTH = BUILDER
            .comment("热源供应站结构基础血量（基地核心式方块血量，自动修复模块可恢复）")
            .defineInRange("heat_station.base_health", 100, 10, 10000);
    public static final ModConfigSpec.IntValue HEAT_ADAPTER_CAPACITY = BUILDER
            .comment("热源适配器储罐容量 (mb)")
            .defineInRange("heat_station.adapter_capacity", 4000, 100, 100000);

    // ── 团队研究 / 科技树 ──
    public static final ModConfigSpec.IntValue RESEARCH_POINTS_PER_HOSTILE = BUILDER
            .comment("击杀敌对生物获得的研究点数")
            .defineInRange("research.points_per_hostile", 1, 0, 100);
    public static final ModConfigSpec.IntValue RESEARCH_POINTS_PER_BOSS = BUILDER
            .comment("击杀 BOSS（凋灵/末影龙）获得的研究点数")
            .defineInRange("research.points_per_boss", 10, 0, 1000);

    // ── 客户端 UI ──
    public static final ModConfigSpec.BooleanValue CUSTOM_HUD_ENABLED = BUILDER
            .comment("是否启用自定义 HUD（关闭后隐藏本模组全部自定义界面元素）")
            .define("client.custom_hud_enabled", true);

    // ── 火焰燃料机制（篝火/炉灶不可虚空燃烧，参考 FrostedHeart 设计）──
    public static final ModConfigSpec.DoubleValue FIRE_STEEL_IGNITION_CHANCE = BUILDER
            .comment("双手打火（铁锭/粒或锌锭/粒 + 燧石）单次成功率")
            .defineInRange("fire.steel_ignition_chance", 0.25, 0.0, 1.0);
    public static final ModConfigSpec.DoubleValue FIRE_STICK_IGNITION_CHANCE = BUILDER
            .comment("钻木取火（木棍对干燥的木条）单次成功率")
            .defineInRange("fire.stick_ignition_chance", 0.20, 0.0, 1.0);
    public static final ModConfigSpec.DoubleValue FIRE_CONSUME_CHANCE = BUILDER
            .comment("双手打火时双手物品同时消耗的概率（无论生火成败）")
            .defineInRange("fire.consume_chance", 0.10, 0.0, 1.0);
    public static final ModConfigSpec.IntValue FIRE_FUEL_MULTIPLIER = BUILDER
            .comment("燃烧时长倍率：实际燃烧 tick = 物品熔炉燃烧值 × 该倍率（篝火/灶台体系；1 = 与熔炉燃料对等）")
            .defineInRange("fire.fuel_multiplier", 1, 1, 20);
    public static final ModConfigSpec.IntValue FIRE_FUEL_CAP_TICKS = BUILDER
            .comment("火焰燃料上限 (tick)：19200 ≈ 16 分钟")
            .defineInRange("fire.fuel_cap_ticks", 19200, 200, 240000);

    public static final ModConfigSpec SPEC = BUILDER.build();
}
