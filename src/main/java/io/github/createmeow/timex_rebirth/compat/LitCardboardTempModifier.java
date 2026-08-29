package io.github.createmeow.timex_rebirth.compat;

import com.momosoftworks.coldsweat.api.temperature.modifier.TempModifier;
import com.momosoftworks.coldsweat.api.util.Temperature;
import net.minecraft.world.entity.LivingEntity;

import java.util.function.Function;

/**
 * 点燃的涂蜡纸板对玩家身体温度（CORE）的加温修饰器（参考 Cold Sweat 水袋 WaterskinTempModifier）。
 * <p>通过 NBT 存储热量偏置，每个修饰器实例对世界/身体温度加固定值；注册后走常规网络同步。</p>
 */
public class LitCardboardTempModifier extends TempModifier {

    /** 无参构造：注册/反序列化必需。 */
    public LitCardboardTempModifier() {
        this(0.0);
    }

    /** 以指定热量偏置创建。 */
    public LitCardboardTempModifier(double heat) {
        this.getNBT().putDouble("heat", heat);
    }

    @Override
    public Function<Double, Double> calculate(LivingEntity entity, Temperature.Trait trait) {
        return temp -> temp + this.getNBT().getDouble("heat");
    }
}
