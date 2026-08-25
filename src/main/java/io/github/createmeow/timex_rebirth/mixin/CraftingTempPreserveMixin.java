package io.github.createmeow.timex_rebirth.mixin;

import io.github.createmeow.timex_rebirth.compat.FiahiCompatHelper;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

/**
 * fiahi 联动：食品加工保留温度。
 * 合成台（面团合成等）产物结算时，把原料携带的 fiahi 温度组件（腐烂/冻结程度）
 * 复制到产物，避免"加工后腐烂/冻结程度直接消失"。
 */
@Mixin(CraftingMenu.class)
public abstract class CraftingTempPreserveMixin {

    @Inject(method = "slotChangedCraftingGrid", at = @At("RETURN"))
    private static void timex_rebirth$preserveCraftingTemperature(
            AbstractContainerMenu menu, Level level, Player player,
            CraftingContainer container, ResultContainer result,
            @Nullable RecipeHolder<CraftingRecipe> recipe, CallbackInfo ci) {
        FiahiCompatHelper.copyTemperatureFromContainer(container, result.getItem(0));
    }
}
