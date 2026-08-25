package io.github.createmeow.timex_rebirth.mixin;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import io.github.createmeow.timex_rebirth.food.WastelandFoodRegistry;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Method;
import java.util.List;

/**
 * 自托管"草药茶液继承输入水纯净度"（不依赖 ThirstWasTaken MixinBasinRecipe 的
 * "输出流体名含 tea" 正则匹配，这里按流体 ID 精确判断，作为幂等兜底）。
 * <p>注入 BasinRecipe.apply 输出流体生成处（与 ThirstWasTaken 同注入点）：
 * 若输出流体是 timex_rebirth:herbal_tea，则把盆地输入槽中带纯净度的流体（水）的
 * 纯净度复制给它。配合 GenericItemFillingMixin 的装瓶纯净度复制，形成完整闭环：
 * 水 → 草药茶液 → 瓶装草药茶 全链路纯净度一致。
 */
@Mixin(value = BasinRecipe.class, remap = false)
public class BasinRecipePurityMixin {

    private static Method HAS_PURITY_FLUID;
    private static Method GET_PURITY_FLUID;
    private static Method ADD_PURITY_FLUID;
    private static boolean thirstChecked = false;

    private static void initThirst() {
        if (thirstChecked)
            return;
        thirstChecked = true;
        try {
            Class<?> wp = Class.forName("dev.ghen.thirst.content.purity.WaterPurity");
            HAS_PURITY_FLUID = wp.getMethod("hasPurity", FluidStack.class);
            GET_PURITY_FLUID = wp.getMethod("getPurity", FluidStack.class);
            ADD_PURITY_FLUID = wp.getMethod("addPurity", FluidStack.class, int.class);
        } catch (Exception ignored) {
            // ThirstWasTaken 未安装：无需纯净度联动
        }
    }

    @Inject(method = "apply(Lcom/simibubi/create/content/processing/basin/BasinBlockEntity;Lnet/minecraft/world/item/crafting/Recipe;Z)Z",
            at = @At(value = "INVOKE", target = "Ljava/util/List;addAll(Ljava/util/Collection;)Z", ordinal = 0))
    private static void timex_rebirth$inheritWaterPurity(BasinBlockEntity basin, Recipe<?> recipe, boolean test,
                                                         CallbackInfoReturnable<Boolean> cir) {
        if (!(recipe instanceof BasinRecipe basinRecipe))
            return;
        List<FluidStack> outputs = basinRecipe.getFluidResults();
        if (outputs.isEmpty())
            return;
        if (outputs.stream().noneMatch(f -> f.getFluid() == WastelandFoodRegistry.HERBAL_TEA_FLUID.get()))
            return;
        initThirst();
        if (ADD_PURITY_FLUID == null)
            return;
        try {
            // 从输入槽找带纯净度的流体（水）
            int purity = -1;
            IFluidHandler input = basin.inputTank.getCapability();
            if (input != null) {
                for (int tank = 0; tank < input.getTanks(); tank++) {
                    FluidStack fs = input.getFluidInTank(tank);
                    if (fs.isEmpty())
                        continue;
                    if ((Boolean) HAS_PURITY_FLUID.invoke(null, fs)) {
                        purity = (Integer) GET_PURITY_FLUID.invoke(null, fs);
                        break;
                    }
                }
            }
            if (purity < 0)
                return;
            for (FluidStack out : outputs) {
                if (out.getFluid() == WastelandFoodRegistry.HERBAL_TEA_FLUID.get()) {
                    ADD_PURITY_FLUID.invoke(null, out, purity);
                }
            }
        } catch (Exception ignored) {
        }
    }
}
