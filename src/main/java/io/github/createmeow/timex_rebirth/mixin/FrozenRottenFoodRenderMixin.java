package io.github.createmeow.timex_rebirth.mixin;

import com.hexagram2021.fiahi.client.model.FIAHIBakedModel;
import com.hexagram2021.fiahi.common.item.capability.IFrozenRottenFood;
import com.hexagram2021.fiahi.register.FIAHIAttachmentTypes;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * FIAHI 联动：让非食物物品也能显示冻结/腐烂视觉效果。
 * <p>
 * FIAHI 的 {@code FIAHIBakedModel.getTemperatureEffectBakedModel} 调用
 * {@code IFrozenRottenFood.canBeFrozenRotten(ItemStack)} 判断是否显示冻结/腐烂。
 * 该方法默认只接受带 {@code DataComponents.FOOD} 的物品（食物），
 * 导致种子/枝条等非食物即使拥有温度组件也不会显示效果。
 * <p>
 * 本 mixin 重定向该 static 调用：当物品持有 FIAHI 温度组件时也返回 true。
 */
@Mixin(FIAHIBakedModel.class)
public abstract class FrozenRottenFoodRenderMixin {

    @Redirect(
            method = "getTemperatureEffectBakedModel",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/hexagram2021/fiahi/common/item/capability/IFrozenRottenFood;canBeFrozenRotten(Lnet/minecraft/world/item/ItemStack;)Z"
            ),
            remap = false
    )
    private static boolean timex_rebirth$allowTempComponentItems(ItemStack stack) {
        return stack.has(net.minecraft.core.component.DataComponents.FOOD)
                || (!stack.isEmpty() && stack.has(FIAHIAttachmentTypes.FOOD_TEMPERATURE.get()));
    }
}
