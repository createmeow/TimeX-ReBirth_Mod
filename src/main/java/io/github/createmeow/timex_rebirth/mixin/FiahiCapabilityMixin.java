package io.github.createmeow.timex_rebirth.mixin;

import com.hexagram2021.fiahi.common.item.capability.IFrozenRottenItemStack;
import com.hexagram2021.fiahi.register.FIAHICapabilities;
import io.github.createmeow.timex_rebirth.compat.FiahiCompatHelper;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * fiahi 联动：原版 fiahi 只为带食物组件的物品注册温度能力，
 * 麦类/面粉/面团/种子等无食物组件的"粮食"默认不会参与温度与腐烂。
 * 这里在 fiahi 注册完成后，为这些粮食补注册温度能力（跳过已有食物组件的物品，避免重复注册）。
 */
@Mixin(FIAHICapabilities.class)
public abstract class FiahiCapabilityMixin {

    @Inject(method = "register", at = @At("TAIL"))
    private static void timex_rebirth$registerGrainCapability(RegisterCapabilitiesEvent event, CallbackInfo ci) {
        ItemLike[] grains = BuiltInRegistries.ITEM.stream()
                .filter(item -> !item.components().has(DataComponents.FOOD) && FiahiCompatHelper.isPerishableGrain(item))
                .toArray(ItemLike[]::new);
        if (grains.length > 0) {
            event.registerItem(FIAHICapabilities.FOOD_CAPABILITY,
                    (itemStack, ignored) -> ((IFrozenRottenItemStack) (Object) itemStack).fiahi$getFrozenRottenFood(),
                    grains);
        }
    }
}
