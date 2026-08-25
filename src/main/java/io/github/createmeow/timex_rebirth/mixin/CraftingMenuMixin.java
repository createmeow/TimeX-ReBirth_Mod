package io.github.createmeow.timex_rebirth.mixin;

import io.github.createmeow.timex_rebirth.research.TechTree;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

/**
 * 科技树配方门控：工作台匹配到被科技树锁定的配方时清空结果，
 * 玩家未完成对应研究前无法合成（材料不消耗）。
 * 挂在 CraftingMenu.slotChangedCraftingGrid 尾部。
 */
@Mixin(CraftingMenu.class)
public abstract class CraftingMenuMixin {

    @Inject(method = "slotChangedCraftingGrid", at = @At("TAIL"))
    private static void timex_rebirth$gateLockedRecipes(AbstractContainerMenu menu, Level level, Player player,
                                                        CraftingContainer craftSlots, ResultContainer resultSlots,
                                                        @Nullable RecipeHolder<CraftingRecipe> recipe, CallbackInfo ci) {
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) return;
        RecipeHolder<?> used = resultSlots.getRecipeUsed();
        if (used != null && TechTree.isRecipeLockedFor(serverPlayer, used.id())) {
            resultSlots.setItem(0, ItemStack.EMPTY);
            resultSlots.setRecipeUsed(null);
        }
    }
}
