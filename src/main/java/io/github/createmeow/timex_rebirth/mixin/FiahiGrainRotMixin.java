package io.github.createmeow.timex_rebirth.mixin;

import com.hexagram2021.fiahi.common.item.capability.IFrozenRottenFood;
import com.hexagram2021.fiahi.common.item.capability.impl.FrozenRottenFood;
import io.github.createmeow.timex_rebirth.compat.FiahiCompatHelper;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * fiahi 联动：原版 foodTick 只处理带食物组件且不在 fiahi:leftovers 的物品；
 * 麦类/面粉/面团/种子等"粮食"没有食物组件，默认不参与温度变化与腐烂/冻结。
 * 这里接管这些粮食的温度 tick，使它们同样经历腐烂（温度&gt;120 转为 fiahi:food_rotting 配方产物）
 * 与冻结（低温下降级），实现"粮食也会腐烂"。
 */
@Mixin(FrozenRottenFood.class)
public abstract class FiahiGrainRotMixin {

    @Inject(method = "foodTick", at = @At("HEAD"), cancellable = true)
    private void timex_rebirth$tickGrainFood(double temperature, Item item, CallbackInfo ci) {
        if (FiahiCompatHelper.isPerishableGrain(item) || FiahiCompatHelper.isSeedLike(item)) {
            ((IFrozenRottenFood) (Object) this).apply(temperature, item);
            ci.cancel();
        }
    }
}
