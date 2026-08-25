package io.github.createmeow.timex_rebirth.mixin;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import io.github.createmeow.timex_rebirth.compat.FiahiCompatHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

/**
 * fiahi 联动：食品加工保留温度（Create 搅拌机/盆地）。
 * 与"草药茶继承输入水纯净度"（BasinRecipePurityMixin）同一注入点、同一思路：
 * 在 BasinRecipe.apply 结算产物时，把盆地输入物品携带的 fiahi 温度（腐烂/冻结程度）
 * 复制到输出物品，使"冷冻面粉 → 搅拌机 → 面团"等加工不再丢失腐烂/冻结程度。
 * 注意：simulate 预检阶段输入尚未消耗、温度可读，此时把温度写入产物栈；
 * 第二次（真实消耗）阶段产物栈沿用同一实例，温度已保留。
 */
@Mixin(value = BasinRecipe.class, remap = false)
public abstract class BasinRecipeTemperatureMixin {

    @Redirect(method = "apply(Lcom/simibubi/create/content/processing/basin/BasinBlockEntity;Lnet/minecraft/world/item/crafting/Recipe;Z)Z",
            at = @At(value = "INVOKE",
                    target = "Lcom/simibubi/create/content/processing/basin/BasinBlockEntity;acceptOutputs(Ljava/util/List;Ljava/util/List;Z)Z"))
    private static boolean timex_rebirth$inheritBasinTemperature(BasinBlockEntity basin,
                                                                 List<ItemStack> outputItems,
                                                                 List<FluidStack> outputFluids,
                                                                 boolean simulate) {
        if (simulate && outputItems != null) {
            int temp = FiahiCompatHelper.getFirstTemperature(basin.inputInventory);
            if (temp != 0) {
                for (ItemStack stack : outputItems) {
                    FiahiCompatHelper.setTemperature(stack, temp);
                }
            }
        }
        return basin.acceptOutputs(outputItems, outputFluids, simulate);
    }
}
