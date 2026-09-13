package com.createmeow.underwaterplugin;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 水下探索插件配置。
 * 机制：玩家水下深度超过阈值后，溺水伤害按深度线性增加。
 */
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLED = BUILDER
            .comment("Enable stronger drowning damage")
            .define("enabled", true);

    /** 深度阈值（方块数）：玩家眼睛上方连续水柱超过此格数后开始增伤 */
    public static final ModConfigSpec.IntValue DEPTH_THRESHOLD = BUILDER
            .comment("Drowning damage bonus starts when the water depth above the player's eyes exceeds this many blocks")
            .defineInRange("depthThreshold", 10, 0, 384);

    /** 每超出 1 格增加的伤害 */
    public static final ModConfigSpec.DoubleValue EXTRA_DAMAGE_PER_BLOCK = BUILDER
            .comment("Extra drowning damage added per block beyond the threshold")
            .defineInRange("extraDamagePerBlock", 0.1, 0.0, 100.0);

    /** 单次溺水伤害加成上限；0 表示无上限 */
    public static final ModConfigSpec.DoubleValue MAX_EXTRA_DAMAGE = BUILDER
            .comment("Maximum extra drowning damage per drowning tick; 0 = unlimited")
            .defineInRange("maxExtraDamage", 4.0, 0.0, 1000.0);

    // ---------------- 溺水值系统 ----------------

    /** 溺水值上限（该值时完全无法移动） */
    public static final ModConfigSpec.IntValue DROWNING_VALUE_MAX = BUILDER
            .comment("Max drowning value; reaching it means the player cannot move at all while in water")
            .defineInRange("drowningValueMax", 18, 1, 100);

    /** 溺水值从此点开始削减游动速度（低于该值时保持原版游速） */
    public static final ModConfigSpec.IntValue DROWNING_PENALTY_START = BUILDER
            .comment("Drowning value at which the swim speed penalty starts; below it vanilla swim speed is kept")
            .defineInRange("drowningPenaltyStart", 5, 0, 99);

    // ---------------- 过深耗氧（参考 BetterDiving） ----------------

    /** 是否启用过深耗氧飙升 */
    public static final ModConfigSpec.BooleanValue DEPTH_OXYGEN_ENABLED = BUILDER
            .comment("Enable oxygen consumption rising with depth (BetterDiving style)")
            .define("depthOxygenEnabled", true);

    /** 深度阈值：眼睛上方水柱超过此格数后氧气消耗开始飙升 */
    public static final ModConfigSpec.IntValue DEPTH_OXYGEN_THRESHOLD = BUILDER
            .comment("Depth (water blocks above eyes) beyond which oxygen consumption rises")
            .defineInRange("depthOxygenThreshold", 20, 0, 384);

    /** 每下潜 N 格，额外氧气消耗速率 +1（BetterDiving: oxygenEfficiencyRate） */
    public static final ModConfigSpec.IntValue DEPTH_OXYGEN_BLOCKS_PER_UNIT = BUILDER
            .comment("Every N blocks beyond the threshold adds +1 to the extra oxygen drain rate")
            .defineInRange("depthOxygenBlocksPerUnit", 8, 1, 1024);

    // ---------------- 深度温度削减（Cold Sweat 联动） ----------------

    /** 是否启用下潜温度削减 */
    public static final ModConfigSpec.BooleanValue DEPTH_TEMP_ENABLED = BUILDER
            .comment("Enable Cold Sweat temperature dropping faster with depth")
            .define("depthTempEnabled", true);

    /**
     * 每下潜 1 格，Cold Sweat 环境温度额外降低多少摄氏度。
     * 注意：Cold Sweat WORLD trait 内部为 MC 标度（×25 = °C），换算由
     * {@link ColdSweatIntegration} 自动完成，这里直接填显示用的 °C 值即可。
     * 默认 1.5°C/格：下潜 10 格约 -15°C，明显变冷但可承受。
     */
    public static final ModConfigSpec.DoubleValue DEPTH_TEMP_PER_BLOCK = BUILDER
            .comment("Cold Sweat world temperature drop per block of depth (in Celsius, converted to Cold Sweat's internal MC scale automatically)")
            .defineInRange("depthTempPerBlock", 1.5, 0.0, 10.0);

    public static final ModConfigSpec SPEC = BUILDER.build();
}
