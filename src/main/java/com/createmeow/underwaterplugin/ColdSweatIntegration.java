package com.createmeow.underwaterplugin;

import com.momosoftworks.coldsweat.api.event.common.temperautre.TempModifierEvent;
import com.momosoftworks.coldsweat.api.temperature.modifier.WaterTempModifier;
import com.momosoftworks.coldsweat.api.util.Temperature;
import net.minecraft.world.entity.player.Player;

import java.util.function.Function;

/**
 * Cold Sweat 集成层。
 * 所有对 Cold Sweat API 的引用都集中在此类，仅当 Cold Sweat 已加载
 * （见 {@link #isAvailable()}）时才会被调用与类加载。
 *
 * 实现方式（防堆叠）：
 * 不再挂载自定义 TempModifier 实例（挂载/替换/序列化语义不可控，曾出现温度随时间
 * 不断累积降低的实测问题），改为监听官方事件 {@link TempModifierEvent.Calculate.Post}，
 * 锚定 Cold Sweat 自带的 WaterTempModifier（玩家在水中时必然挂载、同类型唯一），
 * 在其每次重算 function 时把深度偏移包装进去。
 * TempModifier.update 每 tickRate(5) tick 重新 calculate 生成全新原始 function，
 * 因此包装不会嵌套叠加；非 tick 轮次的 apply 使用缓存的包装后 function，偏移持续生效。
 */
public final class ColdSweatIntegration {

    private ColdSweatIntegration() {}

    public static boolean isAvailable() {
        return net.neoforged.fml.ModList.get().isLoaded("cold_sweat");
    }

    /** 模组构造阶段调用：注册深度温度事件监听 */
    public static void init() {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                ColdSweatIntegration::onCalculatePost);
    }

    /**
     * 在水温修正计算后叠加深度偏移。
     * WaterTempModifier 由 Cold Sweat 在玩家入水时挂载（tickRate=5，每 5 tick 重算一次），
     * 每次重算都会走这里，function 从原始值重新包装，数学上保证稳态 = -depth * perBlock。
     * <p>
     * <b>单位标度</b>：Cold Sweat 的 WORLD trait 内部使用 MC 标度（biome temp 0~2），
     * 显示为 °C 时 ×25（Temperature.convert，MC→C: value * 25）。
     * 配置值以 °C/格 为单位，叠加前须 ÷25 换算成 MC 标度——
     * 否则 0.3 会被当作 0.3 MC = 7.5°C/格（10 格 -75°C，实测"-60"的根因）。
     */
    private static void onCalculatePost(TempModifierEvent.Calculate.Post event) {
        if (event.getTrait() != Temperature.Trait.WORLD) return;
        if (!(event.getModifier() instanceof WaterTempModifier)) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        float depth = DepthTemperatureHandler.getCachedDepth(player);
        if (depth <= 0) return;

        double perBlockCelsius = Config.DEPTH_TEMP_PER_BLOCK.get();
        double offsetMC = -depth * perBlockCelsius / 25.0;
        Function<Double, Double> inner = event.getFunction();
        event.setFunction(temp -> inner.apply(temp) + offsetMC);
    }
}
