package io.github.createmeow.timex_rebirth.mixin;

import com.hexagram2021.fiahi.common.item.capability.impl.FrozenRottenFood;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 修复 fiahi 解冻 bug（fiahi 自身缺陷）：
 * IFrozenRottenFood.apply 在食物温度"等级下降"（解冻/复温）时会把温度
 * 重置回当前冻结等级边界，导致已冻结食物永远无法解冻（热场里只能让
 * 未冻结食物腐烂，冻结食物卡死）。
 * 这里在 foodTick 执行后检测：受热（输入温度高于当前）却仍冻结时，
 * 按 fiahi 正常的平衡速率推进温度向 0 靠拢，实现平滑解冻。
 */
@Mixin(FrozenRottenFood.class)
public abstract class FrozenRottenFoodMixin {

    @Inject(method = "foodTick", at = @At("RETURN"))
    private void timex_rebirth$fixThawing(double temperature, Item item, CallbackInfo ci) {
        FrozenRottenFood self = (FrozenRottenFood) (Object) this;
        double temp = self.getTemperature();
        // 受热（输入温度高于当前）却仍冻结：apply 的降级逻辑卡住了温度，按正常速率推进解冻
        if (temp < 0 && temperature > temp) {
            double diff = (temperature - temp) * ConfigSettings.TEMP_RATE.get() * self.getTemperatureBalanceRate();
            self.setTemperature(Math.min(0.0, temp + diff));
            self.updateFoodTag();
        }
    }
}
