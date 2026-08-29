package io.github.createmeow.timex_rebirth.mixin;

import io.github.createmeow.timex_rebirth.features.FireToolRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * 烤干配方耐久继承：
 * - 篝火、熔炉、晾晒架在烤干湿水的破袜子时，继承输入物品的耐久度；
 * - 类似于 {@link io.github.createmeow.timex_rebirth.features.DryingRackBlockEntity#finishDrying} 的逻辑。
 *
 * <p>注入点选择：
 * - 篝火：在 cookTick 的 dropItemStack 调用点（已有 SeedBonusMixin，在此注入）
 * - 熔炉：在 FurnaceBlockEntity.cook 时的产物设置点
 * - 晾晒架：在 DryingRackRecipe.assemble 的结果生成点注入
 */
public abstract class DryingRecipeDurabilityMixin {

    /** 篝火配方耐久继承：在 dropItemStack 调用点注入 */
    @Mixin(CampfireBlockEntity.class)
    public static class CampfireMixin {
        @Redirect(method = "cookTick",
                at = @At(value = "INVOKE",
                        target = "Lnet/minecraft/world/Containers;dropItemStack(Lnet/minecraft/world/level/Level;DDDLnet/minecraft/world/item/ItemStack;)V"))
        private static void timex_rebirth$inheritWetSocksDurability(Level level, double x, double y, double z, ItemStack stack) {
            // 只处理湿水的破袜子配方（原版配方已清空输入槽，从输入槽获取已无意义）
            // 但我们可以从方块实体的输入槽获取（dropItemStack 触发时输入尚未清空）
            BlockPos pos = BlockPos.containing(x, y, z);
            if (level.getBlockEntity(pos) instanceof CampfireBlockEntity campfire) {
                for (ItemStack input : campfire.getItems()) {
                    if (input.is(FireToolRegistry.WET_TORN_SOCKS.get())) {
                        inheritDurability(stack, input);
                        break;
                    }
                }
            }
            Containers.dropItemStack(level, x, y, z, stack);
        }
    }

    /** 熔炉配方耐久继承：在 cook 时的产物设置点注入 */
    @Mixin(FurnaceBlockEntity.class)
    public static class FurnaceMixin {
        @Inject(method = "cook",
                at = @At(value = "INVOKE",
                        target = "Lnet/minecraft/world/item/crafting/RecipeResultMap;set(Lnet/minecraft/core/Holder;Lnet/minecraft/world/item/crafting/RecipeResultMap$RecipeResult;)V",
                        ordinal = 0))
        private void timex_rebirth$inheritWetSocksDurability(CraftingRecipe recipe, CallbackInfoReturnable<Boolean> cir) {
            FurnaceBlockEntity furnace = (FurnaceBlockEntity) (Object) this;
            ItemStack fuelSlot = furnace.getItem(1);
            if (!fuelSlot.isEmpty() && fuelSlot.is(FireToolRegistry.WET_TORN_SOCKS.get())) {
                ItemStack result = furnace.getItem(0);
                if (!result.isEmpty() && result.is(FireToolRegistry.TORN_SOCKS.get())) {
                    inheritDurability(result, fuelSlot);
                }
            }
        }
    }

    /** 晾晒架配方耐久继承：在 DryingRackRecipe.assemble 的结果生成点注入 */
    @Mixin(value = io.github.createmeow.timex_rebirth.features.DryingRackRecipe.class, remap = false)
    public static class DryingRackMixin {
        @Inject(method = "assemble",
                at = @At("RETURN"))
        private void timex_rebirth$inheritWetSocksDurability(SingleRecipeInput input, HolderLookup.Provider registries, CallbackInfoReturnable<ItemStack> cir) {
            ItemStack result = cir.getReturnValue();
            ItemStack inputItem = input.item();

            // 只处理湿水的破袜子配方
            if (inputItem.is(FireToolRegistry.WET_TORN_SOCKS.get()) && result.is(FireToolRegistry.TORN_SOCKS.get())) {
                inheritDurability(result, inputItem);
            }
        }
    }

    /** 继承耐久度的辅助方法 */
    private static void inheritDurability(ItemStack result, ItemStack input) {
        if (input.isDamageableItem() && result.isDamageableItem()
                && input.getMaxDamage() == result.getMaxDamage()) {
            result.setDamageValue(input.getDamageValue());
        }
    }
}
