package io.github.createmeow.timex_rebirth.mixin;

import io.github.createmeow.timex_rebirth.compat.FiahiCompatHelper;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

/**
 * fiahi 联动：食品加工保留温度。
 * 熔炉/烟熏炉/高炉烧炼（面团→面包等）时，在产物写入结果槽而输入尚未消耗的瞬间，
 * 把原料的 fiahi 温度组件（腐烂/冻结程度）复制到产物，避免加工后程度直接消失。
 */
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class FurnaceTempPreserveMixin {

    @Inject(method = "burn",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V",
                    shift = At.Shift.BEFORE))
    private static void timex_rebirth$preserveFurnaceTemperature(
            RegistryAccess registryAccess,
            @Nullable RecipeHolder<?> recipe,
            NonNullList<ItemStack> inventory,
            int maxStackSize,
            AbstractFurnaceBlockEntity furnace,
            CallbackInfoReturnable<Boolean> cir) {
        // 此时产物已写入 inventory.get(2)，输入（slot 0）尚未 shrink → 温度仍可读取
        FiahiCompatHelper.copyTemperature(inventory.get(0), inventory.get(2));
    }
}
