package io.github.createmeow.timex_rebirth.mixin;

import io.github.createmeow.timex_rebirth.compat.FiahiCompatHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vectorwing.farmersdelight.common.block.entity.CookingPotBlockEntity;
import vectorwing.farmersdelight.common.crafting.CookingPotRecipe;

/**
 * fiahi 联动：食品加工保留温度（农夫乐事厨锅）。
 * 厨锅炖煮完成时，把输入食材（槽 0~5）携带的 fiahi 温度（腐烂/冻结程度）
 * 复制到成品（槽 6），使"冷冻食材 → 炖锅 → 炖菜"不再丢失腐烂/冻结程度。
 * 注意：本类名含 FarmersDelight，由 TimeXMixinPlugin 按 farmersdelight 是否加载门控。
 */
@Mixin(CookingPotBlockEntity.class)
public abstract class FarmersDelightCookingPotMixin {

    @Shadow @Final
    private ItemStackHandler inventory;

    @Inject(method = "processCooking", at = @At("RETURN"))
    private void timex_rebirth$preservePotTemperature(RecipeHolder<CookingPotRecipe> recipe,
                                                      CookingPotBlockEntity cookingPot,
                                                      CallbackInfoReturnable<Boolean> cir) {
        ItemStack meal = this.inventory.getStackInSlot(CookingPotBlockEntity.MEAL_DISPLAY_SLOT);
        if (meal.isEmpty()) return;
        for (int i = 0; i < CookingPotBlockEntity.MEAL_DISPLAY_SLOT; i++) {
            int temp = FiahiCompatHelper.getFoodTemperature(this.inventory.getStackInSlot(i));
            if (temp != 0) {
                FiahiCompatHelper.setTemperature(meal, temp);
                return;
            }
        }
    }
}
